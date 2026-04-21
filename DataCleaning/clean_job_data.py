import argparse
import json
import re
from datetime import datetime

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, lit, trim, udf, when
from pyspark.sql.types import DoubleType, StringType


WORKDAYS_PER_MONTH = 22
HOURS_PER_WORKDAY = 8
WEEKS_PER_MONTH = 52 / 12


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

REQUIRED_SOURCE_COLUMNS = [
    "job_title",
    "company_name",
    "city",
    "work_address",
    "education_text",
    "experience_text",
    "salary_text",
    "job_description",
    "publish_time",
    "data_batch_no",
    "job_hash",
    "detail_city",
    "detail_location_address",
    "detail_degree_text",
    "detail_experience_text",
    "detail_salary_text",
    "detail_job_description",
    "detail_updated_time",
    "detail_company",
    "detail_company_full_name",
]


def clean_text(value):
    if value is None:
        return ""
    text = str(value)
    text = text.replace("\r", "\n")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def normalize_city(value, default_city):
    text = clean_text(value)
    if not text:
        return default_city
    if "贵阳" in text:
        return "贵阳"
    if "遵义" in text:
        return "遵义"
    if "安顺" in text:
        return "安顺"
    if "六盘水" in text:
        return "六盘水"
    if "毕节" in text:
        return "毕节"
    if "铜仁" in text:
        return "铜仁"
    if "黔东南" in text:
        return "黔东南"
    if "黔南" in text:
        return "黔南"
    if "黔西南" in text:
        return "黔西南"
    return text


def normalize_education(value):
    text = clean_text(value)
    if not text:
        return "不限"
    text = text.replace(" ", "")
    if "学历不限" in text or text == "不限":
        return "不限"

    levels = [
        ("博士", "博士"),
        ("硕士", "硕士"),
        ("研究生", "硕士"),
        ("本科", "本科"),
        ("大专", "大专"),
        ("专科", "大专"),
        ("中专", "中专/中技"),
        ("中技", "中专/中技"),
        ("高中", "高中"),
        ("初中", "初中"),
    ]

    for keyword, normalized in levels:
        if keyword in text:
            if "及以上" in text or "以上" in text or "以上学历" in text:
                return f"{normalized}及以上"
            if "及以下" in text or "以下" in text:
                return f"{normalized}及以下"
            if normalized == "初中":
                return "初中及以下"
            return normalized

    if "博士" in text:
        return "博士"
    if "硕士" in text:
        return "硕士"
    if "本科" in text:
        return "本科"
    if "大专" in text:
        return "大专"
    if "中专" in text or "中技" in text:
        return "中专/中技"
    if "高中" in text:
        return "高中"
    if "不限" in text:
        return "不限"
    return text


def normalize_experience(value):
    text = clean_text(value)
    if not text:
        return "不限"
    if "经验不限" in text or text == "不限":
        return "不限"
    if "应届" in text or "在校" in text:
        return "应届生"
    return text


def parse_salary_pair(value):
    text = clean_text(value)
    if not text or "面议" in text:
        return None, None

    upper = text.upper().replace(" ", "")
    upper = upper.replace("Ｋ", "K")

    # 10-20万/年, convert to monthly estimate.
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)万/年", text)
    if m:
        low = float(m.group(1)) * 10000 / 12
        high = float(m.group(2)) * 10000 / 12
        return low, high

    # 150-200元/天, convert to monthly estimate by 22 workdays.
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)元/天", text)
    if m:
        low = float(m.group(1)) * 22
        high = float(m.group(2)) * 22
        return low, high

    # 30-50元/时, convert to monthly estimate by 8 hours * 22 workdays.
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)元/时", text)
    if m:
        low = float(m.group(1)) * 8 * 22
        high = float(m.group(2)) * 8 * 22
        return low, high

    # 800-1200元/周, 800-1200元/星期, convert to monthly estimate by average weeks per month.
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)元/(?:周|星期)", text)
    if m:
        low = float(m.group(1)) * WEEKS_PER_MONTH
        high = float(m.group(2)) * WEEKS_PER_MONTH
        return low, high

    # 1-2K/周, 1K-2K/周, convert to monthly estimate by average weeks per month.
    m = re.search(r"(\d+(?:\.\d+)?)K?-(\d+(?:\.\d+)?)K/(?:周|星期)", upper)
    if m:
        low = float(m.group(1)) * 1000 * WEEKS_PER_MONTH
        high = float(m.group(2)) * 1000 * WEEKS_PER_MONTH
        return low, high

    # 1000元/周, 1000元/一周, 1000元每周, convert to monthly estimate.
    m = re.search(r"(\d+(?:\.\d+)?)元(?:/|每)?一?(?:周|星期)", text)
    if m:
        value = float(m.group(1)) * WEEKS_PER_MONTH
        return value, value

    # 1K/周, convert to monthly estimate.
    m = re.search(r"(\d+(?:\.\d+)?)K/(?:周|星期)", upper)
    if m:
        value = float(m.group(1)) * 1000 * WEEKS_PER_MONTH
        return value, value

    # 8000-12000元/月
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)元/月", text)
    if m:
        return float(m.group(1)), float(m.group(2))

    # 8-12K, 8K-12K, 8k-12k, default BOSS format means monthly salary.
    m = re.search(r"(\d+(?:\.\d+)?)K?-(\d+(?:\.\d+)?)K", upper)
    if m:
        low = float(m.group(1)) * 1000
        high = float(m.group(2)) * 1000
        return low, high

    # 4-6千, default monthly salary.
    m = re.search(r"(\d+(?:\.\d+)?)-(\d+(?:\.\d+)?)千", text)
    if m:
        low = float(m.group(1)) * 1000
        high = float(m.group(2)) * 1000
        return low, high

    return None, None


def salary_min(value):
    parsed = parse_salary_pair(value)
    return parsed[0]


def salary_max(value):
    parsed = parse_salary_pair(value)
    return parsed[1]


def normalize_publish_time(value):
    text = clean_text(value)
    if not text:
        return ""
    # Spark/MySQL can parse "yyyy-MM-dd HH:mm:ss" more reliably than ISO with timezone.
    try:
        if "T" in text:
            return datetime.fromisoformat(text.replace("Z", "+00:00")).strftime("%Y-%m-%d %H:%M:%S")
    except Exception:
        return text
    return text


def build_spark():
    return (
        SparkSession.builder.appName("clean_job_data")
        .enableHiveSupport()
        .getOrCreate()
    )


def read_source(spark, input_path, input_format):
    if input_format == "jsonl":
        return spark.read.json(input_path)
    if input_format == "csv":
        return (
            spark.read.option("header", "true")
            .option("multiLine", "true")
            .option("escape", '"')
            .csv(input_path)
        )
    raise ValueError(f"Unsupported input format: {input_format}")


def ensure_columns(df, columns):
    for column_name in columns:
        if column_name not in df.columns:
            df = df.withColumn(column_name, lit(""))
    return df


def clean(input_path, output_path, report_path, input_format, default_city, output_format):
    spark = build_spark()

    clean_text_udf = udf(clean_text, StringType())
    normalize_city_udf = udf(lambda v: normalize_city(v, default_city), StringType())
    normalize_education_udf = udf(normalize_education, StringType())
    normalize_experience_udf = udf(normalize_experience, StringType())
    salary_min_udf = udf(salary_min, DoubleType())
    salary_max_udf = udf(salary_max, DoubleType())
    normalize_publish_time_udf = udf(normalize_publish_time, StringType())

    raw_df = read_source(spark, input_path, input_format)
    raw_df = ensure_columns(raw_df, REQUIRED_SOURCE_COLUMNS)
    raw_count = raw_df.count()

    # Prefer detail-page fields when present, then fall back to list fields.
    df = raw_df.select(
        clean_text_udf(col("job_title")).alias("job_title"),
        clean_text_udf(
            when(
                col("detail_company_full_name").isNotNull() & (trim(col("detail_company_full_name")) != ""),
                col("detail_company_full_name"),
            )
            .when(col("detail_company").isNotNull() & (trim(col("detail_company")) != ""), col("detail_company"))
            .otherwise(col("company_name"))
        ).alias("company_name"),
        normalize_city_udf(
            when(col("detail_city").isNotNull() & (trim(col("detail_city")) != ""), col("detail_city"))
            .otherwise(col("city"))
        ).alias("city"),
        clean_text_udf(
            when(col("detail_location_address").isNotNull() & (trim(col("detail_location_address")) != ""), col("detail_location_address"))
            .otherwise(col("work_address"))
        ).alias("work_address"),
        normalize_education_udf(
            when(col("detail_degree_text").isNotNull() & (trim(col("detail_degree_text")) != ""), col("detail_degree_text"))
            .otherwise(col("education_text"))
        ).alias("education_text"),
        normalize_experience_udf(
            when(col("detail_experience_text").isNotNull() & (trim(col("detail_experience_text")) != ""), col("detail_experience_text"))
            .otherwise(col("experience_text"))
        ).alias("experience_text"),
        clean_text_udf(
            when(col("detail_salary_text").isNotNull() & (trim(col("detail_salary_text")) != ""), col("detail_salary_text"))
            .otherwise(col("salary_text"))
        ).alias("salary_text"),
        clean_text_udf(
            when(col("detail_job_description").isNotNull() & (trim(col("detail_job_description")) != ""), col("detail_job_description"))
            .otherwise(col("job_description"))
        ).alias("job_description"),
        normalize_publish_time_udf(
            when(col("detail_updated_time").isNotNull() & (trim(col("detail_updated_time")) != ""), col("detail_updated_time"))
            .otherwise(col("publish_time"))
        ).alias("publish_time"),
        clean_text_udf(col("data_batch_no")).alias("data_batch_no"),
        clean_text_udf(col("job_hash")).alias("job_hash"),
        lit(1).alias("status"),
    )

    df = df.withColumn("salary_min", salary_min_udf(col("salary_text")))
    df = df.withColumn("salary_max", salary_max_udf(col("salary_text")))

    valid_required_df = df.filter(
        (col("job_title").isNotNull())
        & (trim(col("job_title")) != "")
        & (col("company_name").isNotNull())
        & (trim(col("company_name")) != "")
        & (col("job_description").isNotNull())
        & (trim(col("job_description")) != "")
        & (col("job_hash").isNotNull())
        & (trim(col("job_hash")) != "")
    )

    valid_required_count = valid_required_df.count()
    missing_required_count = raw_count - valid_required_count
    df = valid_required_df.dropDuplicates(["job_hash"])
    after_dedup_count = df.count()

    df = df.filter(
        (col("salary_min").isNull())
        | (col("salary_max").isNull())
        | (
            (col("salary_min") >= 0)
            & (col("salary_max") >= col("salary_min"))
            & (col("salary_max") <= 100000)
        )
    )

    cleaned_df = df.select(*JOB_INFO_COLUMNS)
    cleaned_count = cleaned_df.count()

    if output_format == "csv":
        cleaned_df.write.mode("overwrite").option("header", "true").csv(output_path)
    elif output_format == "parquet":
        cleaned_df.write.mode("overwrite").parquet(output_path)
    else:
        raise ValueError(f"Unsupported output format: {output_format}")

    report = {
        "input_path": input_path,
        "output_path": output_path,
        "input_format": input_format,
        "output_format": output_format,
        "default_city": default_city,
        "raw_count": raw_count,
        "valid_required_count": valid_required_count,
        "missing_required_count": missing_required_count,
        "duplicate_count": valid_required_count - after_dedup_count,
        "invalid_salary_count": after_dedup_count - cleaned_count,
        "cleaned_count": cleaned_count,
        "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }

    if report_path:
        spark.sparkContext.parallelize([json.dumps(report, ensure_ascii=False)]).saveAsTextFile(report_path)

    print(json.dumps(report, ensure_ascii=False, indent=2))
    spark.stop()


def parse_args():
    parser = argparse.ArgumentParser(description="Clean crawler output into job_info-ready data.")
    parser.add_argument("--input", required=True, help="Input path, local or hdfs. Prefer crawler main jsonl.")
    parser.add_argument("--output", required=True, help="Output HDFS/local directory.")
    parser.add_argument("--report", default="", help="Optional report output directory.")
    parser.add_argument("--format", choices=["jsonl", "csv"], default="jsonl")
    parser.add_argument("--output-format", choices=["parquet", "csv"], default="parquet")
    parser.add_argument("--default-city", default="贵阳")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    clean(args.input, args.output, args.report, args.format, args.default_city, args.output_format)
