package org.txd.guizhoujob.job.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobInfo {
    private Long id;
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
    private String jobHash;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
