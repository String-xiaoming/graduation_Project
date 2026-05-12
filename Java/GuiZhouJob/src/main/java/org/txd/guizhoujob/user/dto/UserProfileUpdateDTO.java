package org.txd.guizhoujob.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String nickname;
    private String localCity;
    private String educationText;
    private String expectedPosition;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String skillInputText;
}
