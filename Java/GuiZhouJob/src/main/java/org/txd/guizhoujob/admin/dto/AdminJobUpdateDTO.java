package org.txd.guizhoujob.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminJobUpdateDTO {
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
    private Integer status;
}
