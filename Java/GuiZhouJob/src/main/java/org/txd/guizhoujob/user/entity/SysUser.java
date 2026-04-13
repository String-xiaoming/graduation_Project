package org.txd.guizhoujob.user.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String localCity;
    private String educationText;
    private String expectedPosition;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String skillInputText;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
