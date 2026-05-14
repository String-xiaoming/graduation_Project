package org.txd.guizhoujob.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.user.entity.SysUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretBytes;
    private final long expireSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expire-hours:24}") long expireHours
    ) {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = Math.max(expireHours, 1) * 3600;
    }

    public TokenPair issue(SysUser user) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expireSeconds;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        claims.put("iat", now);
        claims.put("exp", expiresAt);

        String payload = encodeJson(header) + "." + encodeJson(claims);
        String token = payload + "." + sign(payload);
        return new TokenPair(token, expiresAt);
    }

    public JwtUser verify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("请先登录");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException("登录已过期，请重新登录");
        }

        String payload = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(payload), parts[2])) {
            throw new BusinessException("登录已过期，请重新登录");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        long expiresAt = asLong(claims.get("exp"));
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new BusinessException("登录已过期，请重新登录");
        }

        return new JwtUser(
                asLong(claims.get("sub")),
                asString(claims.get("email")),
                asString(claims.get("role"))
        );
    }

    private String encodeJson(Map<String, Object> value) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(escapeJson(entry.getKey())).append("\":");
            Object item = entry.getValue();
            if (item instanceof Number || item instanceof Boolean) {
                json.append(item);
            } else {
                json.append('"').append(escapeJson(item == null ? "" : String.valueOf(item))).append('"');
            }
            first = false;
        }
        json.append('}');
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return parseFlatJson(new String(json, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("登录已过期，请重新登录");
        }
    }

    private Map<String, Object> parseFlatJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        String text = json.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        int index = 1;
        while (index < text.length() - 1) {
            index = skipWhitespace(text, index);
            if (text.charAt(index) == '}') {
                break;
            }
            ParsedString key = readJsonString(text, index);
            index = skipWhitespace(text, key.nextIndex());
            if (text.charAt(index) != ':') {
                throw new BusinessException("登录已过期，请重新登录");
            }
            index = skipWhitespace(text, index + 1);

            Object item;
            if (text.charAt(index) == '"') {
                ParsedString parsed = readJsonString(text, index);
                item = parsed.value();
                index = parsed.nextIndex();
            } else {
                int start = index;
                while (index < text.length() - 1 && text.charAt(index) != ',') {
                    index++;
                }
                item = text.substring(start, index).trim();
            }
            result.put(key.value(), item);

            index = skipWhitespace(text, index);
            if (index < text.length() - 1 && text.charAt(index) == ',') {
                index++;
            }
        }
        return result;
    }

    private ParsedString readJsonString(String text, int index) {
        if (text.charAt(index) != '"') {
            throw new BusinessException("登录已过期，请重新登录");
        }
        StringBuilder value = new StringBuilder();
        index++;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '"') {
                return new ParsedString(value.toString(), index + 1);
            }
            if (current == '\\') {
                index++;
                if (index >= text.length()) {
                    throw new BusinessException("登录已过期，请重新登录");
                }
                value.append(unescapeJson(text.charAt(index)));
            } else {
                value.append(current);
            }
            index++;
        }
        throw new BusinessException("登录已过期，请重新登录");
    }

    private int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '"' || current == '\\') {
                escaped.append('\\').append(current);
            } else if (current == '\n') {
                escaped.append("\\n");
            } else if (current == '\r') {
                escaped.append("\\r");
            } else if (current == '\t') {
                escaped.append("\\t");
            } else {
                escaped.append(current);
            }
        }
        return escaped.toString();
    }

    private char unescapeJson(char value) {
        return switch (value) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> value;
        };
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("JWT sign failed", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw new BusinessException("登录已过期，请重新登录");
            }
        }
        throw new BusinessException("登录已过期，请重新登录");
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record TokenPair(String token, Long expiresAt) {
    }

    private record ParsedString(String value, int nextIndex) {
    }
}
