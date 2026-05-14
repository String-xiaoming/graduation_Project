package org.txd.guizhoujob.auth;

import org.txd.guizhoujob.common.BusinessException;

public final class AuthContext {

    private static final ThreadLocal<JwtUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(JwtUser user) {
        CURRENT_USER.set(user);
    }

    public static JwtUser get() {
        return CURRENT_USER.get();
    }

    public static Long requireUserId() {
        JwtUser user = CURRENT_USER.get();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException("请先登录");
        }
        return user.getUserId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
