package org.txd.guizhoujob.email.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailVerificationCode {
    private Long id;
    private String email;
    private String scene;
    private String codeHash;
    private LocalDateTime expireTime;
    private Integer used;
    private String sendIp;
    private LocalDateTime createTime;
    private LocalDateTime usedTime;
}
