package org.txd.guizhoujob.user.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private Long id;
    private String email;
    private String nickname;
    private String role;
}
