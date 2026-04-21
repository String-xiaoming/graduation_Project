import argparse

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, lit, to_timestamp, trim, when
from pyspark.sql.types import DecimalType, IntegerType, StringType


JOB_INFO_COLUMNS = [
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

STRING_COLUMNS = [
    "job_title",
    "company_name",
    "city",
    "work_address",
    "education_text",
    "experience_text",
    "salary_text",
    "job_description",
    "data_batch_no",
    "job_hash",
]


def quote_identifier(name):
    return ".".join(f"`{part.replace('`', '``')}`" for part in name.split("."))


def build_spark(source_type):
    builder = SparkSession.builder.appName("export_job_info_to_mysql")
    if source_type == "table":
        builder = builder.enableHiveSupport()
    return builder.getOrCreate()


def ensure_columns(df):
    for column_name in JOB_INFO_COLUMNS:
        if column_name not in df.columns:
            df = df.withColumn(column_name, lit(None))
    return df


def read_source(spark, source_type, source):
    if source_type == "parquet":
        return spark.read.parquet(source)
    if source_type == "table":
        return spark.table(source)
    raise ValueError(f"Unsupported source type: {source_type}")


def prepare_job_info(df):
    df = ensure_columns(df)

    for column_name in STRING_COLUMNS:
        df = df.withColumn(column_name, trim(col(column_name).cast(StringType())))

    df = (
        df.withColumn("salary_min", col("salary_min").cast(DecimalType(10, 2)))
        .withColumn("salary_max", col("salary_max").cast(DecimalType(10, 2)))
        .withColumn(
            "publish_time",
            when(trim(col("publish_time").cast(StringType())) == "", None).otherwise(
                to_timestamp(col("publish_time").cast(StringType()))
            ),
        )
        .withColumn("status", when(col("status").isNull(), lit(1)).otherwise(col("status")).cast(IntegerType()))
    )

    # MySQL uses job_hash as the unique key, so invalid hashes should not enter the business table.
    df = df.filter(col("job_hash").isNotNull() & (trim(col("job_hash")) != ""))
    df = df.filter(col("job_title").isNotNull() & (trim(col("job_title")) != ""))
    df = df.filter(col("company_name").isNotNull() & (trim(col("company_name")) != ""))
    df = df.filter(col("job_description").isNotNull() & (trim(col("job_description")) != ""))
    return df.select(*JOB_INFO_COLUMNS).dropDuplicates(["job_hash"])


def write_stage(df, mysql_url, mysql_user, mysql_password, mysql_driver, stage_table):
    (
        df.write.format("jdbc")
        .mode("overwrite")
        .option("url", mysql_url)
        .option("dbtable", stage_table)
        .option("user", mysql_user)
        .option("password", mysql_password)
        .option("driver", mysql_driver)
        .option("batchsize", "500")
        .save()
    )


def execute_upsert(spark, mysql_url, mysql_user, mysql_password, mysql_driver, stage_table, target_table):
    jvm = spark.sparkContext._gateway.jvm
    jvm.java.lang.Class.forName(mysql_driver)
    conn = jvm.java.sql.DriverManager.getConnection(mysql_url, mysql_user, mysql_password)
    stmt = conn.createStatement()

    stage = quote_identifier(stage_table)
    target = quote_identifier(target_table)
    columns = ", ".join(quote_identifier(column_name) for column_name in JOB_INFO_COLUMNS)
    select_columns = ", ".join(quote_identifier(column_name) for column_name in JOB_INFO_COLUMNS)

    updatable_columns = [column_name for column_name in JOB_INFO_COLUMNS if column_name != "job_hash"]
    update_clause = ",\n    ".join(
        f"{quote_identifier(column_name)} = VALUES({quote_identifier(column_name)})"
        for column_name in updatable_columns
    )

    upsert_sql = f"""
INSERT INTO {target} ({columns})
SELECT {select_columns}
FROM {stage}
ON DUPLICATE KEY UPDATE
    {update_clause},
    `update_time` = CURRENT_TIMESTAMP
"""

    try:
        affected_rows = stmt.executeUpdate(upsert_sql)
        target_count = query_scalar(stmt, f"SELECT COUNT(*) FROM {target}")
        return affected_rows, target_count
    finally:
        stmt.close()
        conn.close()


def query_scalar(stmt, sql):
    rs = stmt.executeQuery(sql)
    try:
        if rs.next():
            return rs.getLong(1)
        return 0
    finally:
        rs.close()


def export(args):
    spark = build_spark(args.source_type)
    try:
        source_df = read_source(spark, args.source_type, args.source)
        job_df = prepare_job_info(source_df)
        stage_count = job_df.count()

        write_stage(
            job_df,
            args.mysql_url,
            args.mysql_user,
            args.mysql_password,
            args.mysql_driver,
            args.stage_table,
        )
        affected_rows, target_count = execute_upsert(
            spark,
            args.mysql_url,
            args.mysql_user,
            args.mysql_password,
            args.mysql_driver,
            args.stage_table,
            args.target_table,
        )

        print(f"stage_count={stage_count}")
        print(f"affected_rows={affected_rows}")
        print(f"target_count={target_count}")
    finally:
        spark.stop()


def parse_args():
    parser = argparse.ArgumentParser(description="Export cleaned job_info data from DWD to MySQL.")
    parser.add_argument("--source-type", choices=["parquet", "table"], default="parquet")
    parser.add_argument("--source", required=True, help="HDFS parquet path or Hive table name.")
    parser.add_argument("--mysql-url", required=True)
    parser.add_argument("--mysql-user", required=True)
    parser.add_argument("--mysql-password", required=True)
    parser.add_argument("--mysql-driver", default="com.mysql.cj.jdbc.Driver")
    parser.add_argument("--stage-table", default="job_info_stage")
    parser.add_argument("--target-table", default="job_info")
    return parser.parse_args()


if __name__ == "__main__":
    export(parse_args())
