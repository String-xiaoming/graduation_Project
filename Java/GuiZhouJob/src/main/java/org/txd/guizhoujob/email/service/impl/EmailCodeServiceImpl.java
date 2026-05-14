package org.txd.guizhoujob.email.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.email.entity.EmailVerificationCode;
import org.txd.guizhoujob.email.mapper.EmailVerificationCodeMapper;
import org.txd.guizhoujob.email.service.EmailCodeService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Set<String> SCENES = Set.of(
            "REGISTER",
            "EMAIL_LOGIN",
            "RESET_PASSWORD",
            "CHANGE_PASSWORD",
            "CHANGE_EMAIL",
            "SENSITIVE_ACTION"
    );

    private final EmailVerificationCodeMapper codeMapper;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email-code.expire-minutes:5}")
    private int expireMinutes;

    @Value("${app.email-code.send-interval-seconds:60}")
    private int sendIntervalSeconds;

    public EmailCodeServiceImpl(EmailVerificationCodeMapper codeMapper, JavaMailSender mailSender) {
        this.codeMapper = codeMapper;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCode(String email, String scene, String sendIp) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedScene = normalizeScene(scene);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCreateTime = codeMapper.selectLastCreateTime(normalizedEmail, normalizedScene);
        if (lastCreateTime != null) {
            long seconds = Duration.between(lastCreateTime, now).getSeconds();
            if (seconds < sendIntervalSeconds) {
                throw new BusinessException("验证码发送过于频繁，请稍后再试");
            }
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        EmailVerificationCode record = new EmailVerificationCode();
        record.setEmail(normalizedEmail);
        record.setScene(normalizedScene);
        record.setCodeHash(hash(normalizedEmail, normalizedScene, code));
        record.setExpireTime(now.plusMinutes(expireMinutes));
        record.setUsed(0);
        record.setSendIp(sendIp);
        int rows = codeMapper.insert(record);
        if (rows <= 0) {
            throw new BusinessException("验证码保存失败");
        }

        sendMail(normalizedEmail, normalizedScene, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyAndConsume(String email, String scene, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedScene = normalizeScene(scene);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("验证码不能为空");
        }

        EmailVerificationCode record = codeMapper.selectLatestAvailable(
                normalizedEmail,
                normalizedScene,
                LocalDateTime.now()
        );
        if (record == null) {
            throw new BusinessException("验证码不存在或已过期");
        }

        String codeHash = hash(normalizedEmail, normalizedScene, code.trim());
        if (!codeHash.equals(record.getCodeHash())) {
            throw new BusinessException("验证码错误");
        }

        int rows = codeMapper.markUsed(record.getId());
        if (rows <= 0) {
            throw new BusinessException("验证码状态更新失败");
        }
    }

    private void sendMail(String email, String scene, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("贵州岗位平台邮箱验证码");
        message.setText("你的验证码是：" + code + "\n\n用途：" + sceneLabel(scene)
                + "\n有效期：" + expireMinutes + "分钟。\n如非本人操作，请忽略本邮件。");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new BusinessException("邮件发送失败，请检查SMTP服务和邮箱授权码");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            throw new BusinessException("验证码场景不能为空");
        }
        String normalized = scene.trim().toUpperCase(Locale.ROOT);
        if (!SCENES.contains(normalized)) {
            throw new BusinessException("不支持的验证码场景");
        }
        return normalized;
    }

    private String hash(String email, String scene, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((email + ":" + scene + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String sceneLabel(String scene) {
        return switch (scene) {
            case "REGISTER" -> "注册账号";
            case "EMAIL_LOGIN" -> "邮箱验证码登录";
            case "RESET_PASSWORD" -> "重置密码";
            case "CHANGE_PASSWORD" -> "修改密码";
            case "CHANGE_EMAIL" -> "修改邮箱";
            default -> "敏感操作验证";
        };
    }
}
