package org.txd.guizhoujob.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminUserSaveDTO {
    private String email;
    private String password;
    private String nickname;
    private String role;
    private Integer status;
    private String localCity;
    private String educationText;
    private String expectedPosition;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String skillInputText;
}
