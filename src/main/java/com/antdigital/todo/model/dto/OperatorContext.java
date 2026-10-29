package com.antdigital.todo.model.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可信操作人上下文：仅由网关注入的请求头解析得到，业务层不信任请求体中的身份字段（R05）。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class OperatorContext {

    /**
     * 可信用户 ID
     */
    private final String userId;

    /**
     * 可信用户展示名
     */
    private final String userName;

    /**
     * 租户标识
     */
    private final String tenantId;

    /**
     * 角色/权限点列表，用于垂直权限判定（R06）
     */
    private final List<String> roles;

    /**
     * 构造操作人上下文。
     *
     * @param userId   可信用户 ID
     * @param userName 可信用户展示名
     * @param tenantId 租户标识
     * @param roles    角色或权限点列表，可为 null
     */
    public OperatorContext(String userId, String userName, String tenantId, List<String> roles) {
        this.userId = userId;
        this.userName = userName;
        this.tenantId = tenantId;
        this.roles = roles == null ? new ArrayList<String>() : new ArrayList<>(roles);
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

    /**
     * 获取角色或权限点列表的只读视图。
     *
     * @return 不可变列表，永不为 null
     */
    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * 输出字段快照，用户展示名不打印原文。
     *
     * @return 字段快照
     */
    @Override
    public String toString() {
        return "OperatorContext{userId='" + userId + '\''
                + ", tenantId='" + tenantId + '\''
                + ", roleCount=" + roles.size()
                + '}';
    }
}
