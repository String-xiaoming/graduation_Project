# 数据清洗说明

## 目标

把爬虫输出的数据清洗成后端 `job_info` 表可以直接使用的字段。

当前推荐输入文件不是主 CSV，而是爬虫生成的主 JSONL：

```text
D:\graduation Project\Crawler\test_1.0\output\<batch>\<batch>.jsonl
```

原因：

- CSV 只保留了 `job_info` 字段。
- JSONL 同时包含列表字段、详情页字段、原始字段，更适合作为 ODS 原始层。

## 清洗输出字段

输出字段严格对齐当前 `job_info`：

```text
job_title
company_name
city
work_address
education_text
experience_text
salary_text
salary_min
salary_max
job_description
publish_time
data_batch_no
job_hash
status
```

## 清洗规则

- 删除岗位名、公司名、岗位描述、`job_hash` 为空的数据，避免空描述岗位进入 Hive/MySQL。
- 按 `job_hash` 去重。
- 优先使用详情页字段回填公司全称、地址、薪资、学历、经验、岗位描述、发布时间。
- 如果爬虫使用 `detail_mode=none`，原始数据里通常没有 `detail_company_full_name`，公司名可能仍是平台列表页的简称。
- 规范城市、学历、经验字段。
- 解析薪资：
  - `8-12K` -> `8000, 12000`
  - `4-6千` -> `4000, 6000`
  - `10-20万/年` -> 按 12 个月折算月薪
  - `150-200元/天` -> 按 22 个工作日折算月薪
  - `30-50元/时` -> 按 8 小时 * 22 天折算月薪
  - `800-1200元/周`、`1-2K/周` -> 按 `52 / 12` 周折算月薪
- 过滤明显异常薪资。

## 本地快速测试

如果只是先在本机验证脚本逻辑，可以把输入输出都放本地：

```bash
spark-submit ^
  D:\graduation Project\DataCleaning\clean_job_data.py ^
  --input "D:\graduation Project\Crawler\test_1.0\output\guiyang_2604201111\guiyang_2604201111.jsonl" ^
  --output "D:\graduation Project\DataCleaning\output\guiyang_2604201111_clean" ^
  --report "D:\graduation Project\DataCleaning\output\guiyang_2604201111_report" ^
  --format jsonl ^
  --output-format parquet ^
  --default-city 贵阳
```

## 上传到 HDFS 后运行

假设你把爬虫 JSONL 上传到了：

```text
hdfs:///recruit/ods/job/guiyang_2604201111.jsonl
```

运行：

```bash
spark-submit \
  --master yarn \
  --deploy-mode client \
  /opt/module/spark_jobs/clean_job_data.py \
  --input hdfs:///recruit/ods/job/guiyang_2604201111.jsonl \
  --output hdfs:///recruit/dwd/job_clean/guiyang_2604201111 \
  --report hdfs:///recruit/dwd/job_clean_report/guiyang_2604201111 \
  --format jsonl \
  --output-format parquet \
  --default-city 贵阳
```

查看结果：

```bash
hdfs dfs -ls /recruit/dwd/job_clean/guiyang_2604201111
hdfs dfs -du -h /recruit/dwd/job_clean/guiyang_2604201111
```

## 推荐处理链路

```text
爬虫 JSONL
  -> HDFS ODS 原始层
  -> Spark 清洗
  -> HDFS DWD 清洗层
  -> Hive 外部表
  -> MySQL job_info / analysis_* 表
  -> Spring Boot / Vue
```

## DWD 同步到 MySQL

当前推荐链路不是让 Hive 直接写 MySQL，而是：

```text
HDFS DWD Parquet / Hive Table
  -> Spark JDBC
  -> MySQL job_info_stage
  -> MySQL job_info
```

原因：

- Spark 已经负责清洗和字段转换，继续用 Spark 读 DWD 最直接。
- 先写 `job_info_stage`，再按 `job_hash` 合并到 `job_info`，可以避免重复岗位导致整批失败。
- MySQL 是业务查询库，Spring Boot 直接查 `job_info`；Hive/HDFS 继续保留明细和分析数据。

导出脚本：

```text
D:\graduation Project\DataCleaning\export_job_info_to_mysql.py
```

node1 上建议放到：

```text
/home/hadoop/spark/export_job_info_to_mysql.py
```

首次需要给 Hadoop 节点授权访问 Windows MySQL，建议创建专用用户，不要直接开放 `root`：

```sql
CREATE USER IF NOT EXISTS 'job_export'@'192.168.232.%' IDENTIFIED BY '你的MySQL密码';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER
ON guizhou_job_platform.*
TO 'job_export'@'192.168.232.%';
FLUSH PRIVILEGES;
```

执行导入时，在 node1 运行：

```bash
export JAVA_HOME=/opt/module/hadoop/jdk17
export HADOOP_HOME=/opt/module/hadoop/hadoop
export HADOOP_CONF_DIR=/opt/module/hadoop/hadoop/etc/hadoop
export YARN_CONF_DIR=/opt/module/hadoop/hadoop/etc/hadoop
export SPARK_HOME=/opt/module/hadoop/spark
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$SPARK_HOME/bin:$PATH
export TMPDIR=/home/hadoop/spark_tmp
export SPARK_LOCAL_DIRS=/home/hadoop/spark_tmp
export _JAVA_OPTIONS='-Djava.io.tmpdir=/home/hadoop/spark_tmp -XX:+PerfDisableSharedMem'
export MYSQL_PASSWORD='你的MySQL密码'

spark-submit \
  --master yarn \
  --deploy-mode client \
  --jars /opt/module/hadoop/apache-hive-4.1.0-bin/lib/mysql-connector-j-9.6.0.jar \
  --driver-class-path /opt/module/hadoop/apache-hive-4.1.0-bin/lib/mysql-connector-j-9.6.0.jar \
  --conf spark.local.dir=/home/hadoop/spark_tmp \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.driver.extraJavaOptions='-Djava.io.tmpdir=/home/hadoop/spark_tmp -XX:+PerfDisableSharedMem' \
  --conf spark.executor.extraJavaOptions='-Djava.io.tmpdir=/home/hadoop/spark_tmp -XX:+PerfDisableSharedMem' \
  /home/hadoop/spark/export_job_info_to_mysql.py \
  --source-type parquet \
  --source hdfs:///recruit/dwd/job_clean_parquet/guiyang_2604201111 \
  --mysql-url 'jdbc:mysql://192.168.232.1:3306/guizhou_job_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
  --mysql-user job_export \
  --mysql-password "$MYSQL_PASSWORD" \
  --stage-table job_info_stage \
  --target-table job_info
```

验证：

```sql
SELECT COUNT(*) FROM job_info;
SELECT id, job_title, company_name, city, salary_text
FROM job_info
ORDER BY id DESC
LIMIT 10;
```
