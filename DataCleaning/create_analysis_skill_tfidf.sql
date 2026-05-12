CREATE TABLE IF NOT EXISTS analysis_skill_tfidf (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stat_date DATE NOT NULL COMMENT '统计日期',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    keyword VARCHAR(100) NOT NULL COMMENT '关键词',
    tfidf_score DECIMAL(12,4) NOT NULL COMMENT 'TF-IDF权重',
    job_count INT NOT NULL COMMENT '包含该关键词的岗位数量',
    rank_no INT NOT NULL COMMENT '排名',
    data_batch_no VARCHAR(100) COMMENT '数据批次号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_city_rank (city, rank_no),
    INDEX idx_stat_date (stat_date),
    INDEX idx_keyword (keyword)
) COMMENT='基于jieba分词和TF-IDF的岗位技能关键词分析表';
