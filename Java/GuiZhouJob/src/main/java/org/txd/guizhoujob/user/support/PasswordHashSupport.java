package org.txd.guizhoujob.user.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class PasswordHashSupport {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordHashSupport() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return ENCODER.matches(rawPassword, storedHash);
        }
        return legacyMd5(rawPassword).equals(storedHash);
    }

    public static boolean needsUpgrade(String storedHash) {
        return storedHash != null && !isBcrypt(storedHash);
    }

    private static boolean isBcrypt(String storedHash) {
        return storedHash.startsWith("$2a$")
                || storedHash.startsWith("$2b$")
                || storedHash.startsWith("$2y$");
    }

    private static String legacyMd5(String source) {
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
