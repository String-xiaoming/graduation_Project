package org.txd.guizhoujob.email.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.txd.guizhoujob.common.Result;
import org.txd.guizhoujob.email.dto.EmailCodeSendDTO;
import org.txd.guizhoujob.email.service.EmailCodeService;

@RestController
@RequestMapping("/email/code")
public class EmailCodeController {

    private final EmailCodeService emailCodeService;

    public EmailCodeController(EmailCodeService emailCodeService) {
        this.emailCodeService = emailCodeService;
    }

    @PostMapping("/send")
    public Result<Void> send(@RequestBody @Valid EmailCodeSendDTO dto, HttpServletRequest request) {
        emailCodeService.sendCode(dto.getEmail(), dto.getScene(), clientIp(request));
        return Result.success();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
