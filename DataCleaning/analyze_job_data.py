# -*- coding: utf-8 -*-
import argparse
from datetime import date

from pyspark.sql import SparkSession, Window
from pyspark.sql.functions import (
    array,
    col,
    count,
    explode,
    lit,
    round as spark_round,
    row_number,
    to_date,
    trim,
    udf,
    when,
)
from pyspark.sql.types import ArrayType, DateType, IntegerType, StringType


REQUIRED_COLUMNS = [
    "job_title",
    "company_name",
    "city",
    "work_address",
    "education_text",
    "experience_text",
    "salary_text",
    "salary_min",
    "salary_max",
    "job_description",
    "publish_time",
    "data_batch_no",
    "job_hash",
    "status",
]

SKILL_KEYWORDS = [
    "Java",
    "JavaScript",
    "Python",
    "SQL",
    "MySQL",
    "Redis",
    "Spring",
    "Spring Boot",
    "Vue",
    "React",
    "HTML",
    "CSS",
    "Hadoop",
    "Spark",
    "Hive",
    "HDFS",
    "Linux",
    "Excel",
    "Word",
    "PPT",
    "PowerPoint",
    "CAD",
    "Photoshop",
    "PS",
    "UI",
    "数据分析",
    "机器学习",
    "算法",
    "前端",
    "后端",
    "运维",
    "测试",
    "软件测试",
    "网络安全",
    "数据库",
    "会计",
    "财务",
    "销售",
    "客服",
    "运营",
    "新媒体",
    "文案",
    "直播",
    "短视频",
    "设计",
    "教师",
    "培训",
    "沟通",
    "管理",
    "招聘",
    "人事",
    "行政",
    "采购",
    "物流",
    "仓储",
    "驾驶",
    "叉车",
    "电工",
    "焊工",
    "施工",
    "预算",
    "造价",
    "法务",
    "律师",
    "护士",
    "药师",
    "护理",
    "康复",
    "餐饮",
    "厨师",
    "美容",
    "保安",
]


def quote_identifier(name):
    return ".".join(f"`{part.replace('`', '``')}`" for part in name.split("."))


def infer_batch_no(source):
    cleaned = source.rstrip("/")
    if not cleaned:
        return "unknown_batch"
    return cleaned.split("/")[-1]


def build_spark(source_type):
    builder = SparkSession.builder.appName("analyze_job_data")
    if source_type == "table":
        builder = builder.enableHiveSupport()
    return builder.getOrCreate()


def read_source(spark, source_type, source):
    if source_type == "parquet":
        return spark.read.parquet(source)
    if source_type == "table":
        return spark.table(source)
    raise ValueError(f"Unsupported source type: {source_type}")


def ensure_columns(df):
    for column_name in REQUIRED_COLUMNS:
        if column_name not in df.columns:
            df = df.withColumn(column_name, lit(None))
    return df


def normalize_base_df(df):
    df = ensure_columns(df)
    return (
        df.select(*REQUIRED_COLUMNS)
        .withColumn("job_title", trim(col("job_title").cast(StringType())))
        .withColumn("company_name", trim(col("company_name").cast(StringType())))
        .withColumn("city", trim(col("city").cast(StringType())))
        .withColumn("education_text", trim(col("education_text").cast(StringType())))
        .withColumn("experience_text", trim(col("experience_text").cast(StringType())))
        .withColumn("salary_text", trim(col("salary_text").cast(StringType())))
        .withColumn("job_description", trim(col("job_description").cast(StringType())))
        .withColumn("data_batch_no", trim(col("data_batch_no").cast(StringType())))
        .withColumn("status", col("status").cast(IntegerType()))
        .withColumn(
            "city",
            when(col("city").isNull() | (col("city") == ""), lit("未知")).otherwise(col("city")),
        )
        .withColumn(
            "education_text",
            when(col("education_text").isNull() | (col("education_text") == ""), lit("不限")).otherwise(
                col("education_text")
            ),
        )
        .withColumn(
            "experience_text",
            when(col("experience_text").isNull() | (col("experience_text") == ""), lit("不限")).otherwise(
                col("experience_text")
            ),
        )
        .filter((col("status").isNull()) | (col("status") == 1))
        .filter(col("job_title").isNotNull() & (col("job_title") != ""))
        .filter(col("company_name").isNotNull() & (col("company_name") != ""))
    )


def add_common_columns(df, stat_date, batch_no):
    return (
        df.withColumn("stat_date", to_date(lit(stat_date)).cast(DateType()))
        .withColumn("data_batch_no", lit(batch_no))
    )


def city_job_stats(df, stat_date, batch_no):
    return add_common_columns(
        df.groupBy("city").agg(count(lit(1)).cast(IntegerType()).alias("job_count")),
        stat_date,
        batch_no,
    ).select("stat_date", "city", "job_count", "data_batch_no")


def city_position_stats(df, stat_date, batch_no, top_n):
    grouped = df.groupBy("city", "job_title").agg(count(lit(1)).cast(IntegerType()).alias("job_count"))
    window = Window.partitionBy("city").orderBy(col("job_count").desc(), col("job_title").asc())
    ranked = grouped.withColumn("rank_no", row_number().over(window)).filter(col("rank_no") <= top_n)
    return add_common_columns(ranked, stat_date, batch_no).select(
        "stat_date", "city", "job_title", "job_count", "data_batch_no"
    )


def education_stats(df, stat_date, batch_no):
    grouped = df.groupBy("city", "education_text").agg(count(lit(1)).cast(IntegerType()).alias("job_count"))
    total_window = Window.partitionBy("city")
    result = grouped.withColumn(
        "proportion",
        spark_round(col("job_count") * lit(100.0) / sum_count(total_window), 2),
    )
    return add_common_columns(result, stat_date, batch_no).select(
        "stat_date", "city", "education_text", "job_count", "proportion", "data_batch_no"
    )


def experience_stats(df, stat_date, batch_no):
    grouped = df.groupBy("city", "experience_text").agg(count(lit(1)).cast(IntegerType()).alias("job_count"))
    total_window = Window.partitionBy("city")
    result = grouped.withColumn(
        "proportion",
        spark_round(col("job_count") * lit(100.0) / sum_count(total_window), 2),
    )
    return add_common_columns(result, stat_date, batch_no).select(
        "stat_date", "city", "experience_text", "job_count", "proportion", "data_batch_no"
    )


def sum_count(window):
    from pyspark.sql.functions import sum as spark_sum

    return spark_sum("job_count").over(window)


def salary_distribution(df, stat_date, batch_no):
    salary_avg = (col("salary_min").cast("double") + col("salary_max").cast("double")) / lit(2.0)
    bucketed = df.withColumn(
        "salary_range",
        when(col("salary_min").isNull() | col("salary_max").isNull(), lit("未知"))
        .when(salary_avg < 3000, lit("3K以下"))
        .when(salary_avg < 5000, lit("3K-5K"))
        .when(salary_avg < 8000, lit("5K-8K"))
        .when(salary_avg < 12000, lit("8K-12K"))
        .when(salary_avg < 15000, lit("12K-15K"))
        .otherwise(lit("15K以上")),
    )
    grouped = bucketed.groupBy("city", "salary_range").agg(count(lit(1)).cast(IntegerType()).alias("job_count"))
    return add_common_columns(grouped, stat_date, batch_no).select(
        "stat_date", "city", "salary_range", "job_count", "data_batch_no"
    )


def extract_skills(text):
    if not text:
        return []
    original = str(text)
    lower_text = original.lower()
    matched = []
    for keyword in SKILL_KEYWORDS:
        if keyword.lower() in lower_text:
            matched.append(keyword)
    return list(dict.fromkeys(matched))


def skill_hotword(df, stat_date, batch_no, top_n):
    extract_skills_udf = udf(extract_skills, ArrayType(StringType()))
    text_df = df.withColumn(
        "skill_source",
        array(
            col("job_title").cast(StringType()),
            col("job_description").cast(StringType()),
            col("salary_text").cast(StringType()),
        ),
    )
    skill_df = (
        text_df.withColumn("skill_list", extract_skills_udf(col("skill_source").cast(StringType())))
        .withColumn("skill_name", explode(col("skill_list")))
        .groupBy("city", "skill_name")
        .agg(count(lit(1)).cast(IntegerType()).alias("hot_value"))
    )
    window = Window.partitionBy("city").orderBy(col("hot_value").desc(), col("skill_name").asc())
    ranked = skill_df.withColumn("rank_no", row_number().over(window)).filter(col("rank_no") <= top_n)
    return add_common_columns(ranked, stat_date, batch_no).select(
        "stat_date", "city", "skill_name", "hot_value", "rank_no", "data_batch_no"
    )


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


def clear_stat_date(spark, args, tables):
    for table in tables:
        sql = f"DELETE FROM {quote_identifier(table)} WHERE stat_date = '{args.stat_date}'"
        deleted = execute_sql(
            spark,
            args.mysql_url,
            args.mysql_user,
            args.mysql_password,
            args.mysql_driver,
            sql,
        )
        print(f"deleted {deleted} rows from {table} for stat_date={args.stat_date}")


def write_mysql(df, args, table_name):
    (
        df.write.format("jdbc")
        .mode("append")
        .option("url", args.mysql_url)
        .option("dbtable", table_name)
        .option("user", args.mysql_user)
        .option("password", args.mysql_password)
        .option("driver", args.mysql_driver)
        .option("batchsize", "500")
        .save()
    )
    print(f"wrote {df.count()} rows to {table_name}")


def analyze(args):
    spark = build_spark(args.source_type)
    tables = [
        "analysis_city_job_stats",
        "analysis_city_position_stats",
        "analysis_education_stats",
        "analysis_experience_stats",
        "analysis_salary_distribution",
        "analysis_skill_hotword",
    ]

    try:
        source_df = read_source(spark, args.source_type, args.source)
        base_df = normalize_base_df(source_df).cache()
        source_count = base_df.count()
        print(f"source_count={source_count}")

        clear_stat_date(spark, args, tables)

        outputs = [
            ("analysis_city_job_stats", city_job_stats(base_df, args.stat_date, args.batch_no)),
            ("analysis_city_position_stats", city_position_stats(base_df, args.stat_date, args.batch_no, args.top_n)),
            ("analysis_education_stats", education_stats(base_df, args.stat_date, args.batch_no)),
            ("analysis_experience_stats", experience_stats(base_df, args.stat_date, args.batch_no)),
            ("analysis_salary_distribution", salary_distribution(base_df, args.stat_date, args.batch_no)),
            ("analysis_skill_hotword", skill_hotword(base_df, args.stat_date, args.batch_no, args.top_n)),
        ]

        for table_name, output_df in outputs:
            write_mysql(output_df, args, table_name)
    finally:
        spark.stop()


def parse_args():
    parser = argparse.ArgumentParser(description="Analyze cleaned job data and write analysis_* tables to MySQL.")
    parser.add_argument("--source-type", choices=["parquet", "table"], default="parquet")
    parser.add_argument("--source", required=True, help="HDFS parquet path or Hive table name.")
    parser.add_argument("--mysql-url", required=True)
    parser.add_argument("--mysql-user", required=True)
    parser.add_argument("--mysql-password", required=True)
    parser.add_argument("--mysql-driver", default="com.mysql.cj.jdbc.Driver")
    parser.add_argument("--stat-date", default=date.today().isoformat())
    parser.add_argument("--batch-no", default=None)
    parser.add_argument("--top-n", type=int, default=50)
    args = parser.parse_args()
    if not args.batch_no:
        args.batch_no = infer_batch_no(args.source)
    return args


if __name__ == "__main__":
    analyze(parse_args())
