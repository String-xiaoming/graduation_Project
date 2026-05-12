# -*- coding: utf-8 -*-
import argparse
import re
from datetime import date
from pathlib import Path

import jieba
from pyspark.ml.feature import CountVectorizer, IDF
from pyspark.sql import SparkSession, Window
from pyspark.sql.functions import (
    col,
    concat_ws,
    countDistinct,
    explode,
    lit,
    row_number,
    sum as spark_sum,
    to_date,
    trim,
    udf,
    when,
)
from pyspark.sql.types import (
    ArrayType,
    DateType,
    DoubleType,
    IntegerType,
    StringType,
    StructField,
    StructType,
)


REQUIRED_COLUMNS = [
    "id",
    "job_title",
    "company_name",
    "city",
    "job_description",
    "data_batch_no",
    "job_hash",
    "status",
]

DEFAULT_STOPWORDS = {
    "岗位",
    "职责",
    "要求",
    "负责",
    "工作",
    "完成",
    "相关",
    "优先",
    "良好",
    "能力",
    "具有",
    "以上",
    "以下",
    "不限",
    "经验",
    "学历",
    "公司",
    "提供",
    "进行",
    "以及",
    "根据",
    "协助",
    "熟悉",
    "掌握",
    "使用",
    "能够",
    "需要",
    "任职",
    "资格",
    "内容",
    "职位",
    "招聘",
    "人员",
}

KEEP_WORDS = [
    "Java",
    "JavaScript",
    "Python",
    "Boot",
    "Spring Boot",
    "Spring",
    "MySQL",
    "Redis",
    "Vue",
    "React",
    "Hadoop",
    "Spark",
    "Hive",
    "HDFS",
    "Linux",
    "Excel",
    "CAD",
    "Photoshop",
    "UI",
    "SQL",
]


def quote_identifier(name):
    return ".".join(f"`{part.replace('`', '``')}`" for part in name.split("."))


def infer_batch_no(source):
    cleaned = source.rstrip("/")
    if not cleaned:
        return "unknown_batch"
    return cleaned.split("/")[-1]


def load_stopwords(path):
    words = set(DEFAULT_STOPWORDS)
    if path and Path(path).exists():
        with open(path, "r", encoding="utf-8") as file:
            for line in file:
                word = line.strip()
                if word and not word.startswith("#"):
                    words.add(word)
    return words


def normalize_token(token):
    token = token.strip()
    if not token:
        return ""
    ascii_token = token.upper()
    canonical = {
        "JAVA": "Java",
        "JAVASCRIPT": "JavaScript",
        "JS": "JavaScript",
        "PYTHON": "Python",
        "MYSQL": "MySQL",
        "REDIS": "Redis",
        "SPRING": "Spring",
        "VUE": "Vue",
        "REACT": "React",
        "HADOOP": "Hadoop",
        "SPARK": "Spark",
        "HIVE": "Hive",
        "HDFS": "HDFS",
        "LINUX": "Linux",
        "SQL": "SQL",
        "EXCEL": "Excel",
        "CAD": "CAD",
        "PS": "Photoshop",
        "UI": "UI",
    }
    return canonical.get(ascii_token, token)


def make_tokenizer(stopwords):
    for word in KEEP_WORDS:
        jieba.add_word(word, freq=200000)

    def tokenize(text):
        if not text:
            return []

        source = str(text)
        source = re.sub(r"[^\u4e00-\u9fa5A-Za-z0-9+#.]+", " ", source)
        tokens = []
        for token in jieba.cut(source):
            word = normalize_token(token)
            if not word:
                continue
            if word == "Boot":
                word = "Spring Boot"
            if word in stopwords or word.lower() in stopwords:
                continue
            if len(word) < 2 and not re.fullmatch(r"[A-Za-z+#.]+", word):
                continue
            if re.fullmatch(r"\d+", word):
                continue
            if re.fullmatch(r"\d+[kK]?", word):
                continue
            if re.search(r"\d", word):
                continue
            tokens.append(word)

        return list(dict.fromkeys(tokens))

    return tokenize


def build_spark(source_type):
    builder = SparkSession.builder.appName("analyze_skill_tfidf")
    if source_type == "table":
        builder = builder.enableHiveSupport()
    return builder.getOrCreate()


def read_source(spark, args):
    if args.source_type == "parquet":
        return spark.read.parquet(args.source)
    if args.source_type == "table":
        return spark.table(args.source)
    if args.source_type == "mysql":
        return (
            spark.read.format("jdbc")
            .option("url", args.mysql_url)
            .option("dbtable", args.source)
            .option("user", args.mysql_user)
            .option("password", args.mysql_password)
            .option("driver", args.mysql_driver)
            .load()
        )
    raise ValueError(f"Unsupported source type: {args.source_type}")


def ensure_columns(df):
    for column_name in REQUIRED_COLUMNS:
        if column_name not in df.columns:
            df = df.withColumn(column_name, lit(None))
    return df


def normalize_base_df(df):
    df = ensure_columns(df)
    return (
        df.select(*REQUIRED_COLUMNS)
        .withColumn("raw_id", trim(col("id").cast(StringType())))
        .withColumn("job_hash", trim(col("job_hash").cast(StringType())))
        .withColumn("job_title", trim(col("job_title").cast(StringType())))
        .withColumn("company_name", trim(col("company_name").cast(StringType())))
        .withColumn("city", trim(col("city").cast(StringType())))
        .withColumn("job_description", trim(col("job_description").cast(StringType())))
        .withColumn("data_batch_no", trim(col("data_batch_no").cast(StringType())))
        .withColumn("status", col("status").cast(IntegerType()))
        .withColumn(
            "job_id",
            when(col("raw_id").isNotNull() & (col("raw_id") != ""), col("raw_id"))
            .when(col("job_hash").isNotNull() & (col("job_hash") != ""), col("job_hash"))
            .otherwise(concat_ws("::", col("job_title"), col("company_name"), col("job_description"))),
        )
        .withColumn("city", when(col("city").isNull() | (col("city") == ""), lit("未知")).otherwise(col("city")))
        .filter((col("status").isNull()) | (col("status") == 1))
        .filter(col("job_title").isNotNull() & (col("job_title") != ""))
        .filter(col("job_description").isNotNull() & (col("job_description") != ""))
    )


def top_terms_udf(vocabulary, per_doc_limit):
    schema = ArrayType(
        StructType(
            [
                StructField("keyword", StringType(), False),
                StructField("tfidf_score", DoubleType(), False),
            ]
        )
    )

    def extract(vector):
        if vector is None:
            return []
        if hasattr(vector, "indices"):
            pairs = zip(vector.indices.tolist(), vector.values.tolist())
        else:
            pairs = enumerate(vector.tolist())
        ranked = sorted(pairs, key=lambda item: item[1], reverse=True)[:per_doc_limit]
        return [
            {
                "keyword": vocabulary[index],
                "tfidf_score": float(score),
            }
            for index, score in ranked
            if score > 0 and index < len(vocabulary)
        ]

    return udf(extract, schema)


def execute_sql(spark, mysql_url, mysql_user, mysql_password, mysql_driver, sql):
    jvm = spark.sparkContext._gateway.jvm
    jvm.java.lang.Class.forName(mysql_driver)
    conn = jvm.java.sql.DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
    stmt = conn.createStatement()
    try:
        return stmt.executeUpdate(sql)
    finally:
        stmt.close()
        conn.close()


def clear_target(spark, args):
    sql = (
        f"DELETE FROM {quote_identifier(args.output_table)} "
        f"WHERE stat_date = '{args.stat_date}' AND data_batch_no = '{args.batch_no}'"
    )
    deleted = execute_sql(
        spark,
        args.mysql_url,
        args.mysql_user,
        args.mysql_password,
        args.mysql_driver,
        sql,
    )
    print(f"deleted {deleted} rows from {args.output_table}")


def write_mysql(df, args):
    (
        df.write.format("jdbc")
        .mode("append")
        .option("url", args.mysql_url)
        .option("dbtable", args.output_table)
        .option("user", args.mysql_user)
        .option("password", args.mysql_password)
        .option("driver", args.mysql_driver)
        .option("batchsize", "500")
        .save()
    )
    print(f"wrote {df.count()} rows to {args.output_table}")


def aggregate_keywords(keyword_df, stat_date, batch_no, top_n):
    grouped = (
        keyword_df.groupBy("city", "keyword")
        .agg(
            spark_sum("tfidf_score").alias("tfidf_score"),
            countDistinct("job_id").cast(IntegerType()).alias("job_count"),
        )
        .filter(col("keyword").isNotNull() & (col("keyword") != ""))
    )
    window = Window.partitionBy("city").orderBy(col("tfidf_score").desc(), col("job_count").desc(), col("keyword").asc())
    return (
        grouped.withColumn("rank_no", row_number().over(window))
        .filter(col("rank_no") <= top_n)
        .withColumn("stat_date", to_date(lit(stat_date)).cast(DateType()))
        .withColumn("data_batch_no", lit(batch_no))
        .select("stat_date", "city", "keyword", "tfidf_score", "job_count", "rank_no", "data_batch_no")
    )


def analyze(args):
    stopwords = load_stopwords(args.stopwords)
    spark = build_spark(args.source_type)

    try:
        source_df = read_source(spark, args)
        base_df = normalize_base_df(source_df).cache()
        source_count = base_df.count()
        print(f"source_count={source_count}")
        clear_target(spark, args)
        if source_count == 0:
            print("no valid job descriptions, skip TF-IDF")
            return

        tokenize_udf = udf(make_tokenizer(stopwords), ArrayType(StringType()))
        tokenized_df = (
            base_df.withColumn("text_source", col("job_description"))
            .withColumn("tokens", tokenize_udf(col("text_source")))
            .filter(col("tokens").isNotNull())
        )

        vectorizer = CountVectorizer(
            inputCol="tokens",
            outputCol="tf",
            vocabSize=args.vocab_size,
            minDF=args.min_df,
            minTF=1.0,
        )
        cv_model = vectorizer.fit(tokenized_df)
        tf_df = cv_model.transform(tokenized_df)
        idf_model = IDF(inputCol="tf", outputCol="tfidf").fit(tf_df)
        tfidf_df = idf_model.transform(tf_df)

        extract_top_terms_udf = top_terms_udf(cv_model.vocabulary, args.per_doc_limit)
        exploded_df = (
            tfidf_df.withColumn("top_terms", extract_top_terms_udf(col("tfidf")))
            .withColumn("term", explode(col("top_terms")))
            .select(
                col("job_id"),
                col("city"),
                col("term.keyword").alias("keyword"),
                col("term.tfidf_score").alias("tfidf_score"),
            )
        )

        province_df = exploded_df.withColumn("city", lit("全省"))
        output_df = aggregate_keywords(exploded_df.unionByName(province_df), args.stat_date, args.batch_no, args.top_n)

        write_mysql(output_df, args)
    finally:
        spark.stop()


def parse_args():
    parser = argparse.ArgumentParser(description="Analyze job descriptions with jieba + TF-IDF.")
    parser.add_argument("--source-type", choices=["parquet", "table", "mysql"], default="mysql")
    parser.add_argument("--source", default="job_info", help="Parquet path, Hive table name, or MySQL table name.")
    parser.add_argument("--mysql-url", required=True)
    parser.add_argument("--mysql-user", required=True)
    parser.add_argument("--mysql-password", required=True)
    parser.add_argument("--mysql-driver", default="com.mysql.cj.jdbc.Driver")
    parser.add_argument("--output-table", default="analysis_skill_tfidf")
    parser.add_argument("--stopwords", default=str(Path(__file__).resolve().parent / "stopwords.txt"))
    parser.add_argument("--stat-date", default=date.today().isoformat())
    parser.add_argument("--batch-no", default=None)
    parser.add_argument("--top-n", type=int, default=30)
    parser.add_argument("--per-doc-limit", type=int, default=8)
    parser.add_argument("--vocab-size", type=int, default=5000)
    parser.add_argument("--min-df", type=float, default=5.0)
    args = parser.parse_args()
    if not args.batch_no:
        args.batch_no = infer_batch_no(args.source)
    return args


if __name__ == "__main__":
    analyze(parse_args())
