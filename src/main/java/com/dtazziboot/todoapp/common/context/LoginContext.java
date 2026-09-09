package com.dtazziboot.todoapp.common.context;

/**
 * 登录上下文工具类，用于承载当前请求的登录身份信息
 *
 * @author AiWork
 * @date 2026/09/09
 */
public final class LoginContext {

    /**
     * 创建人 ID 线程变量
     */
    private static final ThreadLocal<String> CREATOR_ID = new ThreadLocal<>();

    /**
     * 租户 ID 线程变量
     */
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    /**
     * 私有构造，禁止实例化
     */
    private LoginContext() {
    }

    /**
     * 设置登录身份信息
     *
     * @param creatorId 创建人 ID
     * @param tenantId  租户 ID
     */
    public static void set(String creatorId, String tenantId) {
        CREATOR_ID.set(creatorId);
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取创建人 ID
     *
     * @return 创建人 ID，未登录时返回 null
     */
    public static String getCreatorId() {
        return CREATOR_ID.get();
    }

    /**
     * 获取租户 ID
     *
     * @return 租户 ID，未登录时返回 null
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清理线程变量，防止内存泄漏
     */
    public static void clear() {
        CREATOR_ID.remove();
        TENANT_ID.remove();
    }
}
