package org.txd.guizhoujob.analysis.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SkillTfidfVO {
    private Long id;
    private LocalDate statDate;
    private String city;
    private String keyword;
    private BigDecimal tfidfScore;
    private Integer jobCount;
    private Integer rankNo;
    private String dataBatchNo;
}
