package org.txd.guizhoujob.analysis.dto;

import lombok.Data;

@Data
public class SkillTfidfQueryDTO {
    private String city = "全省";
    private Integer limit = 20;
}
