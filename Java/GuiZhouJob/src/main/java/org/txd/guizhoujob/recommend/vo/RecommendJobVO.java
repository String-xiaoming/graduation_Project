package org.txd.guizhoujob.recommend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendJobVO {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String city;
    private String workAddress;
    private String educationText;
    private String experienceText;
    private String salaryText;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String jobDescription;
    private LocalDateTime publishTime;
    private String dataBatchNo;
    private Double recommendScore;
    private Double contentScore;
    private Double cityScore;
    private Double salaryScore;
    private Double educationScore;
    private Double experienceScore;
    private String reasonText;
    private List<String> reasons;
    private LocalDateTime createTime;
}
