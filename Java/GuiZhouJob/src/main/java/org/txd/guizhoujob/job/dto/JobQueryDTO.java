package org.txd.guizhoujob.job.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String city;
    private String educationText;
    private String experienceText;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Boolean onlyWithSalary;

    public Integer getOffset() {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (safePageNum - 1) * safePageSize;
    }
}
