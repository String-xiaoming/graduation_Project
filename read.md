# 贵州人才招聘市场分析管理平台项目说明

更新时间：2026-04-28

本文档用于后续继续开发、运行和维护本项目。内容根据当前项目代码、数据清洗脚本、前后端结构以及前期对话整理。

## 1. 项目定位

本项目是一个基于 Hadoop、Spark、Hive、MySQL、Spring Boot 和 Vue 的贵州人才招聘市场分析管理平台。

核心目标：

- 爬取贵州各城市岗位数据。
- 使用 Spark 对岗位数据进行清洗、去重、薪资解析、城市过滤和字段规范化。
- 将清洗结果挂载到 Hive，并同步到 MySQL 业务库。
- 基于清洗后的岗位数据生成薪资、学历、经验、城市、技能热词、jieba + TF-IDF 关键词等分析结果。
- 后端 Spring Boot 提供用户、岗位、分析看板、管理员接口。
- 前端 Vue 展示首页、岗位列表、岗位详情、数据看板、个人中心、管理员管理页面。
- 推荐系统是后续重点功能，目前已有部分表结构基础，但完整推荐逻辑还需要继续实现。

## 2. 当前项目结构

项目根目录：

```text
D:\graduation Project
```

主要目录：

```text
D:\graduation Project
├─ Crawler/                 爬虫项目
├─ DataCleaning/            Spark 清洗、分析、导入 MySQL 脚本
├─ Java/GuiZhouJob/         Spring Boot 后端
├─ Vue/guizhou-job-web/     Vue 前端
├─ tmp/                     临时文档、截图、运行日志
├─ README.md                早期占位文件
└─ read.md                  当前总说明文档
```

## 3. 总体数据流

当前推荐的完整链路：

```text
爬虫 JSONL
  -> 上传到 node1
  -> HDFS ODS 原始层
  -> Spark 清洗
  -> HDFS DWD Parquet 清洗层
  -> Hive 外部表查看
  -> Spark JDBC 导入 MySQL job_info
  -> Spark 生成 MySQL analysis_* 分析表
  -> Spark jieba + TF-IDF 生成 analysis_skill_tfidf
  -> Spring Boot 查询 MySQL
  -> Vue 前端展示
```

注意：

- Hive 主要用于大数据侧查看和论文体现。
- MySQL 是业务查询库，Spring Boot 直接查 MySQL。
- `job_info_stage` 是中转表，最终业务查询看 `job_info`。

## 4. 爬虫模块

路径：

```text
D:\graduation Project\Crawler\test_1.0\test1.py
```

说明文档：

```text
D:\graduation Project\Crawler\使用说明.md
```

用途：

- 按城市采集 BOSS 直聘岗位。
- 支持贵州主要城市和州：
  `贵阳`、`遵义`、`安顺`、`六盘水`、`毕节`、`铜仁`、`黔东南`、`黔南`、`黔西南`。
- 支持英文别名：
  `guiyang`、`zunyi`、`anshun`、`liupanshui`、`bijie`、`tongren`、`qiandongnan`、`qiannan`、`qianxinan`。
- 使用多类别种子关键词和自动扩词，不需要手动输入岗位关键词。
- 默认使用 `safe` 模式，速度较慢但更稳。
- 使用 `cache` 做跨批次去重和扩词缓存。

运行方式：

```powershell
cd "D:\graduation Project\Crawler\test_1.0"
python test1.py
```

输出目录：

```text
D:\graduation Project\Crawler\test_1.0\output\<batch>\
```

常见输出文件：

```text
<batch>.csv                 与 job_info 对齐的主 CSV
<batch>.jsonl               推荐用于 Spark 清洗的主 JSONL
<batch>_detail_raw.jsonl    详情页原始字段
meta.json                   本次爬取元数据
debug/                      每页接口调试数据
detail_html/                详情页 HTML
```

当前清洗推荐使用 JSONL，不推荐用 CSV：

```text
D:\graduation Project\Crawler\test_1.0\output\<batch>\<batch>.jsonl
```

重要维护点：

- 如果重新爬某个城市，并希望不受历史去重影响，删除对应城市缓存：

```powershell
Remove-Item "D:\graduation Project\Crawler\test_1.0\cache\<city>_keyword_pool.json"
Remove-Item "D:\graduation Project\Crawler\test_1.0\cache\<city>_seen_hashes.txt"
```

- 铜仁和六盘水曾出现 cityCode 写反问题，已修正：

```text
六盘水 -> 101260600
铜仁   -> 101260400
```

- 旧批次如果是在修正前爬的，可能存在城市错标，不建议导入数据库。

## 5. Hadoop / Spark / Hive / MySQL 数据处理模块

本地脚本目录：

```text
D:\graduation Project\DataCleaning
```

核心脚本：

```text
clean_job_data.py              Spark 基础清洗
export_job_info_to_mysql.py    DWD Parquet 导入 MySQL job_info
analyze_job_data.py            生成 analysis_* 基础分析表
analyze_skill_tfidf.py         jieba + TF-IDF 技能关键词分析
run_job_pipeline.sh            一键流水线
stopwords.txt                  jieba 分词停用词
create_analysis_skill_tfidf.sql
```

node1 推荐脚本目录：

```text
/home/hadoop/spark
```

node1 推荐数据上传目录：

```text
/home/hadoop/data
```

HDFS 主要路径：

```text
hdfs:///recruit/ods/job/<batch>.jsonl
hdfs:///recruit/dwd/job_clean_parquet/<batch>
hdfs:///recruit/dwd/job_clean_report/<batch>
```

一键流水线运行方式：

```bash
cd /home/hadoop/spark
export MYSQL_PASSWORD='你的MySQL密码'
./run_job_pipeline.sh <batch> <city>
```

如果 JSONL 不在默认目录，可以传第三个参数：

```bash
./run_job_pipeline.sh <batch> <city> /home/hadoop/data/<batch>.jsonl
```

示例：

```bash
cd /home/hadoop/spark
export MYSQL_PASSWORD='你的MySQL密码'
./run_job_pipeline.sh anshun_2604211551 安顺 /home/hadoop/data/anshun_2604211551.jsonl
```

流水线内部步骤：

```text
Step 1 上传 JSONL 到 HDFS ODS
Step 2 Spark 清洗到 HDFS DWD Parquet
Step 3 查看清洗报告
Step 4 刷新 Hive 外部表 dwd_job_clean
Step 5 导入 MySQL job_info_stage / job_info
Step 6 生成 analysis_* 统计分析表
Step 7 执行 jieba + TF-IDF，生成 analysis_skill_tfidf
Step 8 验证 MySQL 数据
```

基础清洗规则：

- 删除岗位名、公司名、岗位描述、`job_hash` 为空的数据。
- 按 `job_hash` 去重。
- 使用详情页字段优先回填公司全称、地址、学历、经验、薪资、岗位描述、发布时间。
- 使用 `--strict-city` 过滤非目标城市数据。
- 解析月薪：
  `K/月`、`千/月`、`万/年`、`元/天`、`元/时`、`元/周` 等会折算为月薪。
- 过滤明显异常薪资。

## 6. jieba + TF-IDF 在哪一步执行

该功能不是爬虫阶段执行，也不是后端实时执行。

执行位置：

```text
D:\graduation Project\DataCleaning\analyze_skill_tfidf.py
```

在一键流水线中位于：

```text
run_job_pipeline.sh 的 Step 7
```

核心作用：

- jieba：对岗位标题和岗位描述进行中文分词。
- CountVectorizer：把分词结果转成词频向量。
- IDF：降低常见词权重，提高更能代表岗位特征的词。
- TF-IDF：得到每个城市、每批岗位中更有代表性的技能关键词。

结果写入 MySQL 表：

```text
analysis_skill_tfidf
```

前端数据看板读取的是 MySQL 中已经算好的结果，不负责计算算法。

## 7. MySQL 数据库

数据库名：

```text
guizhou_job_platform
```

主要业务表：

```text
sys_user                 系统用户表
user_preference          用户偏好表
user_skill               用户技能表
user_behavior_log        用户行为日志
user_search_log          用户搜索日志
job_info                 清洗后的岗位信息表
job_info_stage           岗位导入中转表
job_skill                岗位技能关联表
skill_dict               技能词典表
recommend_result         推荐结果表
etl_batch_log            ETL 批次日志表
crawl_task_log           爬虫任务日志表
```

主要分析表：

```text
analysis_city_job_stats          城市岗位总量统计
analysis_city_position_stats     城市岗位名称统计
analysis_education_stats         学历要求统计
analysis_experience_stats        经验要求统计
analysis_salary_distribution     薪资分布统计
analysis_skill_hotword           规则词典技能热词统计
analysis_skill_tfidf             jieba + TF-IDF 技能关键词统计
```

当前后端主要查：

```text
job_info
analysis_*
analysis_skill_tfidf
sys_user
```

## 8. Spring Boot 后端

路径：

```text
D:\graduation Project\Java\GuiZhouJob
```

技术栈：

```text
Java 17
Spring Boot 4.0.5
MyBatis
MySQL
Lombok
```

配置文件：

```text
D:\graduation Project\Java\GuiZhouJob\src\main\resources\application.properties
```

当前后端端口：

```text
8080
```

启动方式：

```powershell
cd "D:\graduation Project\Java\GuiZhouJob"
.\mvnw spring-boot:run
```

主要模块：

```text
common      统一 Result、分页、异常处理
user        注册、登录、个人资料
job         岗位分页、热门岗位、岗位详情
analysis    数据看板、TF-IDF 分析结果
admin       管理员用户和岗位管理
```

主要接口：

```text
POST   /user/register
POST   /user/login
GET    /user/{id}
PUT    /user/profile

GET    /job/page
GET    /job/hot
GET    /job/{id}

GET    /analysis/skill-tfidf
GET    /analysis/skill-tfidf/cities

GET    /admin/users
POST   /admin/users
PUT    /admin/users/{id}
DELETE /admin/users/{id}
GET    /admin/jobs
PUT    /admin/jobs/{id}
DELETE /admin/jobs/{id}
```

注意：

- 当前登录是本地项目演示式实现，没有完整 JWT / Session 安全体系。
- 管理员权限主要靠用户角色字段和前端路由控制，后续正式化需要补鉴权拦截器。

## 9. Vue 前端

路径：

```text
D:\graduation Project\Vue\guizhou-job-web
```

技术栈：

```text
Vue 3
Vite
Vue Router
Axios
ECharts
```

启动方式：

```powershell
cd "D:\graduation Project\Vue\guizhou-job-web"
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

接口代理：

```text
前端 /api -> 后端 http://localhost:8080
```

主要页面：

```text
/             首页
/jobs         岗位列表
/jobs/:id     岗位详情
/dashboard    数据看板
/profile      个人中心
/admin        管理员页面
/login        登录
/register     注册
```

关键前端文件：

```text
src/router/index.js
src/api/request.js
src/api/user.js
src/api/job.js
src/api/analysis.js
src/api/admin.js
src/views/*.vue
src/components/*.vue
src/assets/styles/global.css
```

## 10. 推荐系统当前状态

推荐系统是项目的重要目标，但当前还不是完整版本。

已经具备的基础：

- 用户表、用户偏好表、用户技能表、用户行为日志表、用户搜索日志表。
- 岗位表 `job_info`。
- 推荐结果表 `recommend_result`。
- 技能词典和岗位技能关联表。
- 清洗和分析阶段已经能产生薪资、经验、学历、城市、技能热词、TF-IDF 关键词等特征。

后续推荐系统建议实现方式：

```text
用户信息
  -> 自然语言技能提取
  -> 用户偏好权重
  -> 用户行为日志加权
  -> 岗位特征匹配
  -> 推荐分数
  -> recommend_result
  -> 首页和推荐模块展示
```

推荐权重示例：

```text
本地城市偏好      高权重
期望岗位          中高权重
技能匹配          高权重
薪资范围          中权重
学历经验匹配      中权重
浏览/点击行为     动态加权
```

用户填写“更希望本地工作”为一级偏好时，推荐应该优先提高本地岗位分数，而不是只按岗位名称推荐。

## 11. 常用操作清单

### 11.1 爬取岗位

```powershell
cd "D:\graduation Project\Crawler\test_1.0"
python test1.py
```

输入城市和目标数量后，等待输出 JSONL。

### 11.2 上传 JSONL 到 node1

在 Windows 上使用 scp：

```powershell
scp "D:\graduation Project\Crawler\test_1.0\output\<batch>\<batch>.jsonl" hadoop@192.168.232.101:/home/hadoop/data/<batch>.jsonl
```

### 11.3 在 node1 执行完整清洗和导入

```bash
cd /home/hadoop/spark
export MYSQL_PASSWORD='你的MySQL密码'
./run_job_pipeline.sh <batch> <city> /home/hadoop/data/<batch>.jsonl
```

### 11.4 启动后端

```powershell
cd "D:\graduation Project\Java\GuiZhouJob"
.\mvnw spring-boot:run
```

### 11.5 启动前端

```powershell
cd "D:\graduation Project\Vue\guizhou-job-web"
npm run dev
```

### 11.6 验证 MySQL 数据

```sql
SELECT COUNT(*) FROM job_info;
SELECT id, job_title, company_name, city, salary_text FROM job_info ORDER BY id DESC LIMIT 10;
SELECT * FROM analysis_skill_tfidf ORDER BY stat_date DESC, city, rank_no LIMIT 20;
```

## 12. 已知问题和注意事项

- 爬虫受目标网站限制，不能保证真正全量，只能做高覆盖采样。
- 如果详情页未登录或被限制，岗位描述可能不完整。
- 爬虫速度过快容易触发限制，默认推荐使用 `safe` 模式。
- 部分历史输出可能来自旧版本爬虫，导入前要确认城市和批次。
- `job_info_stage` 是导入中转表，不是最终业务表。
- `jieba + TF-IDF` 在 Spark 分析阶段执行，结果写入 `analysis_skill_tfidf`。
- 前端数据看板没有数据时，优先检查 MySQL 的 `analysis_*` 表是否有数据。
- 后端连接的是本机 MySQL，Hadoop 节点导入 MySQL 时需要确保 MySQL 允许对应主机访问。
- 当前登录鉴权是毕业设计演示强度，后续如要上线，需要补 token、安全拦截器和密码加密策略。

## 13. 后续开发优先级

建议继续按以下顺序推进：

1. 扩大岗位数据量，保证贵州各城市至少有稳定样本。
2. 完善推荐系统，包括用户偏好权重、行为日志、技能匹配和推荐结果落表。
3. 增加用户自然语言技能提取，把“会使用 Java、Python”等描述转成结构化技能。
4. 完善管理员模块，包括数据批次、爬虫任务、ETL 日志查看。
5. 补充后端鉴权和密码加密。
6. 补充系统测试和论文截图。
7. 根据最终论文内容固定数据批次和可视化结果。

## 14. 一句话记忆

本项目的数据主线是：

```text
爬虫抓 JSONL -> Spark 清洗 -> Hive 查看 -> MySQL 存业务和分析结果 -> Spring Boot 提供接口 -> Vue 展示
```

其中：

```text
岗位查询看 job_info
数据看板看 analysis_*
jieba + TF-IDF 看 analysis_skill_tfidf
推荐系统后续看 recommend_result
```
