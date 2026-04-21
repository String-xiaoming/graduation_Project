package org.txd.guizhoujob.job.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobInfoVO {
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
}
