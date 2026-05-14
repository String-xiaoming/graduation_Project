package org.txd.guizhoujob.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUser {
    private Long userId;
    private String email;
    private String role;
}
