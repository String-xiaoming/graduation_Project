package org.txd.guizhoujob.email.service;

public interface EmailCodeService {
    void sendCode(String email, String scene, String sendIp);

    void verifyAndConsume(String email, String scene, String code);
}
