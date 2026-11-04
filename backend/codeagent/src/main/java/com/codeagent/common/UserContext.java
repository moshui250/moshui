package com.codeagent.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前请求的登录用户上下文。
 * 由 JwtAuthInterceptor 写入，afterCompletion 中必须清理，避免线程池复用串号。
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo user) {
        HOLDER.set(user);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserInfo user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final Long id;
        private final String username;
        private final String role;
    }
}
