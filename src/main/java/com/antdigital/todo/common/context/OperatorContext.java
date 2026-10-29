package com.antdigital.todo.common.context;

/**
 * 登录态操作人上下文，由登录拦截器根据网关注入的 Header 构建。
 *
 * @author AiWork
 */
public class OperatorContext {

    private final String userId;

    private final String userName;

    private final String tenantId;

    public OperatorContext(String userId, String userName, String tenantId) {
        this.userId = userId;
        this.userName = userName;
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getTenantId() {
        return tenantId;
    }
}
