#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  export MYSQL_PASSWORD='your_mysql_password'
  ./run_job_pipeline.sh <batch> <city> [node1_jsonl_path]

Example:
  export MYSQL_PASSWORD='******'
  ./run_job_pipeline.sh guiyang_2604201111 贵阳

Optional environment variables:
  STAT_DATE=2026-04-21
  MYSQL_HOST=192.168.232.1
  MYSQL_PORT=3306
  MYSQL_DB=guizhou_job_platform
  MYSQL_USER=job_export

Default node1 JSONL path:
  /home/hadoop/data/<batch>.jsonl

Required scripts on node1:
  /home/hadoop/spark/clean_job_data.py
  /home/hadoop/spark/export_job_info_to_mysql.py
  /home/hadoop/spark/analyze_job_data.py
  /home/hadoop/spark/analyze_skill_tfidf.py
  /home/hadoop/spark/stopwords.txt
EOF
}

if [[ $# -lt 2 ]]; then
  usage
  exit 1
fi

BATCH="$1"
CITY="$2"
LOCAL_JSONL="${3:-/home/hadoop/data/${BATCH}.jsonl}"
STAT_DATE="${STAT_DATE:-$(date +%F)}"

MYSQL_HOST="${MYSQL_HOST:-192.168.232.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-guizhou_job_platform}"
MYSQL_USER="${MYSQL_USER:-job_export}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

SPARK_JOB_DIR="${SPARK_JOB_DIR:-/home/hadoop/spark}"
SPARK_TMP_DIR="${SPARK_TMP_DIR:-/home/hadoop/spark_tmp}"
MYSQL_JAR="${MYSQL_JAR:-/opt/module/hadoop/apache-hive-4.1.0-bin/lib/mysql-connector-j-9.6.0.jar}"
HIVE_COMMONS_JAR="${HIVE_COMMONS_JAR:-/opt/module/hadoop/apache-hive-4.1.0-bin/lib/commons-collections-3.2.2.jar}"

ODS_PATH="hdfs:///recruit/ods/job/${BATCH}.jsonl"
DWD_PATH="hdfs:///recruit/dwd/job_clean_parquet/${BATCH}"
REPORT_PATH="hdfs:///recruit/dwd/job_clean_report/${BATCH}"
MYSQL_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

if [[ -z "$MYSQL_PASSWORD" ]]; then
  echo "ERROR: Please run: export MYSQL_PASSWORD='your_mysql_password'"
  exit 1
fi

if [[ ! -f "$LOCAL_JSONL" ]]; then
  echo "ERROR: JSONL file not found: $LOCAL_JSONL"
  echo "Upload it first, for example:"
  echo "  scp your_batch.jsonl hadoop@node1:/home/hadoop/data/${BATCH}.jsonl"
  exit 1
fi

for required_file in \
  "${SPARK_JOB_DIR}/clean_job_data.py" \
  "${SPARK_JOB_DIR}/export_job_info_to_mysql.py" \
  "${SPARK_JOB_DIR}/analyze_job_data.py" \
  "${SPARK_JOB_DIR}/analyze_skill_tfidf.py" \
  "${SPARK_JOB_DIR}/stopwords.txt" \
  "$MYSQL_JAR"; do
  if [[ ! -f "$required_file" ]]; then
    echo "ERROR: Missing required file: $required_file"
    exit 1
  fi
done

export JAVA_HOME="${JAVA_HOME:-/opt/module/hadoop/jdk17}"
export HADOOP_HOME="${HADOOP_HOME:-/opt/module/hadoop/hadoop}"
export HADOOP_CONF_DIR="${HADOOP_CONF_DIR:-$HADOOP_HOME/etc/hadoop}"
export YARN_CONF_DIR="${YARN_CONF_DIR:-$HADOOP_HOME/etc/hadoop}"
export SPARK_HOME="${SPARK_HOME:-/opt/module/hadoop/spark}"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$SPARK_HOME/bin:$PATH"
export TMPDIR="$SPARK_TMP_DIR"
export SPARK_LOCAL_DIRS="$SPARK_TMP_DIR"
export _JAVA_OPTIONS="${_JAVA_OPTIONS:--Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem}"

mkdir -p "$SPARK_TMP_DIR"

echo "== Batch    : ${BATCH}"
echo "== City     : ${CITY}"
echo "== StatDate : ${STAT_DATE}"
echo "== ODS      : ${ODS_PATH}"
echo "== DWD      : ${DWD_PATH}"

echo "== Check Hadoop status"
hdfs dfsadmin -safemode get
yarn node -list

echo "== Step 1: Upload JSONL to HDFS ODS"
hdfs dfs -mkdir -p /recruit/ods/job
hdfs dfs -put -f "$LOCAL_JSONL" "$ODS_PATH"
hdfs dfs -ls "$ODS_PATH"

echo "== Step 2: Clean JSONL to HDFS DWD Parquet"
hdfs dfs -rm -r -f "$DWD_PATH" "$REPORT_PATH" >/dev/null 2>&1 || true

spark-submit \
  --master yarn \
  --deploy-mode client \
  --conf spark.local.dir="$SPARK_TMP_DIR" \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.driver.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  --conf spark.executor.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  "${SPARK_JOB_DIR}/clean_job_data.py" \
  --input "$ODS_PATH" \
  --output "$DWD_PATH" \
  --report "$REPORT_PATH" \
  --format jsonl \
  --output-format parquet \
  --default-city "$CITY" \
  --strict-city

echo "== Step 3: Check clean result"
hdfs dfs -ls "$DWD_PATH"
hdfs dfs -cat "${REPORT_PATH}/part-*" | head || true

echo "== Step 4: Refresh Hive external table"
HIVE_SQL=$(cat <<SQL
SET hive.execution.engine=mr;
ADD JAR ${HIVE_COMMONS_JAR};
CREATE DATABASE IF NOT EXISTS recruit_warehouse;
USE recruit_warehouse;
CREATE EXTERNAL TABLE IF NOT EXISTS dwd_job_clean (
    job_title STRING,
    company_name STRING,
    city STRING,
    work_address STRING,
    education_text STRING,
    experience_text STRING,
    salary_text STRING,
    salary_min DOUBLE,
    salary_max DOUBLE,
    job_description STRING,
    publish_time STRING,
    data_batch_no STRING,
    job_hash STRING,
    status INT
)
STORED AS PARQUET
LOCATION '${DWD_PATH}';
ALTER TABLE dwd_job_clean SET LOCATION '${DWD_PATH}';
SELECT COUNT(*) AS cnt FROM dwd_job_clean;
SQL
)

if command -v beeline >/dev/null 2>&1; then
  beeline -u jdbc:hive2://node1:10000/default -n hadoop -e "$HIVE_SQL"
elif command -v hive >/dev/null 2>&1; then
  hive -e "$HIVE_SQL"
else
  echo "WARN: beeline/hive command not found. Skipped Hive table refresh."
fi

echo "== Step 5: Export DWD Parquet to MySQL job_info"
spark-submit \
  --master yarn \
  --deploy-mode client \
  --jars "$MYSQL_JAR" \
  --driver-class-path "$MYSQL_JAR" \
  --conf spark.local.dir="$SPARK_TMP_DIR" \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.driver.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  --conf spark.executor.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  "${SPARK_JOB_DIR}/export_job_info_to_mysql.py" \
  --source-type parquet \
  --source "$DWD_PATH" \
  --mysql-url "$MYSQL_URL" \
  --mysql-user "$MYSQL_USER" \
  --mysql-password "$MYSQL_PASSWORD" \
  --stage-table job_info_stage \
  --target-table job_info

echo "== Step 6: Analyze DWD Parquet to MySQL analysis_* tables"
spark-submit \
  --master yarn \
  --deploy-mode client \
  --jars "$MYSQL_JAR" \
  --driver-class-path "$MYSQL_JAR" \
  --conf spark.local.dir="$SPARK_TMP_DIR" \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.driver.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  --conf spark.executor.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  "${SPARK_JOB_DIR}/analyze_job_data.py" \
  --source-type parquet \
  --source "$DWD_PATH" \
  --mysql-url "$MYSQL_URL" \
  --mysql-user "$MYSQL_USER" \
  --mysql-password "$MYSQL_PASSWORD" \
  --stat-date "$STAT_DATE" \
  --batch-no "$BATCH" \
  --top-n 50

echo "== Step 7: Analyze job descriptions with jieba + TF-IDF"
spark-submit \
  --master yarn \
  --deploy-mode client \
  --jars "$MYSQL_JAR" \
  --driver-class-path "$MYSQL_JAR" \
  --conf spark.local.dir="$SPARK_TMP_DIR" \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.driver.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  --conf spark.executor.extraJavaOptions="-Djava.io.tmpdir=$SPARK_TMP_DIR -XX:+PerfDisableSharedMem" \
  "${SPARK_JOB_DIR}/analyze_skill_tfidf.py" \
  --source-type parquet \
  --source "$DWD_PATH" \
  --mysql-url "$MYSQL_URL" \
  --mysql-user "$MYSQL_USER" \
  --mysql-password "$MYSQL_PASSWORD" \
  --stat-date "$STAT_DATE" \
  --batch-no "$BATCH" \
  --top-n 30 \
  --stopwords "${SPARK_JOB_DIR}/stopwords.txt"

echo "== Step 8: Verify MySQL data"
if command -v mysql >/dev/null 2>&1; then
  env MYSQL_PWD="$MYSQL_PASSWORD" mysql \
    -h "$MYSQL_HOST" \
    -P "$MYSQL_PORT" \
    -u"$MYSQL_USER" \
    -D "$MYSQL_DB" \
    -e "SELECT COUNT(*) AS job_info_count FROM job_info;
        SELECT 'analysis_city_job_stats' AS table_name, COUNT(*) AS cnt FROM analysis_city_job_stats
        UNION ALL SELECT 'analysis_city_position_stats', COUNT(*) FROM analysis_city_position_stats
        UNION ALL SELECT 'analysis_education_stats', COUNT(*) FROM analysis_education_stats
        UNION ALL SELECT 'analysis_experience_stats', COUNT(*) FROM analysis_experience_stats
        UNION ALL SELECT 'analysis_salary_distribution', COUNT(*) FROM analysis_salary_distribution
        UNION ALL SELECT 'analysis_skill_hotword', COUNT(*) FROM analysis_skill_hotword
        UNION ALL SELECT 'analysis_skill_tfidf', COUNT(*) FROM analysis_skill_tfidf;
        SELECT id, job_title, company_name, city, salary_text FROM job_info ORDER BY id DESC LIMIT 5;
        SELECT city, salary_range, job_count FROM analysis_salary_distribution WHERE stat_date='${STAT_DATE}' ORDER BY city, salary_range;
        SELECT city, skill_name, hot_value, rank_no FROM analysis_skill_hotword WHERE stat_date='${STAT_DATE}' ORDER BY city, rank_no LIMIT 10;
        SELECT city, keyword, tfidf_score, job_count, rank_no FROM analysis_skill_tfidf WHERE stat_date='${STAT_DATE}' ORDER BY city, rank_no LIMIT 10;"
else
  echo "WARN: mysql command not found. Please verify in DataGrip or backend API."
fi

echo "== Pipeline finished successfully."
