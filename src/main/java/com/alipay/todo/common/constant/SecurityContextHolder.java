package com.alipay.todo.common.constant;

/**
 * 安全上下文持有者（模拟统一登录拦截器）
 * 实际项目中由登录拦截器设置当前用户信息
 *
 * @author AiWork
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<String> CREATOR_HOLDER = new ThreadLocal<>();

    public static void setCreator(String creator) {
        CREATOR_HOLDER.set(creator);
    }

    public static String getCreator() {
        return CREATOR_HOLDER.get();
    }

    public static void clear() {
        CREATOR_HOLDER.remove();
    }

    private SecurityContextHolder() {
        throw new IllegalStateException("Utility class");
    }
}