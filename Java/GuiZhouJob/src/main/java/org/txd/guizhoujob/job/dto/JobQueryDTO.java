package org.txd.guizhoujob.job.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String city;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
}
