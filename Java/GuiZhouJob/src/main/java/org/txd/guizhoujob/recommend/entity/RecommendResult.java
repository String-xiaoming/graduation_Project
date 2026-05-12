package org.txd.guizhoujob.recommend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendResult {
    private Long id;
    private Long userId;
    private Long jobId;
    private Double recommendScore;
    private Double contentScore;
    private Double cityScore;
    private Double salaryScore;
    private Double educationScore;
    private Double experienceScore;
    private String reasonText;
    private Integer rankNo;
    private String batchNo;
    private LocalDateTime createTime;
}
