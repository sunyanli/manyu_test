package com.antdigital.todo.integration.impl;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.util.TodoTextUtil;
import com.antdigital.todo.integration.AuthIntegration;
import com.antdigital.todo.integration.support.GatewaySignatureVerifier;
import com.antdigital.todo.model.dto.OperatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 登录态与身份解析实现（I01）：读取统一网关注入并经签名校验的身份与权限头。
 *
 * <p>身份与权限字段只信任网关注入值，请求体中的同名字段一律忽略（R05）；
 * 请求头必须带有网关 HMAC-SHA256 签名，客户端自行构造的 {@code X-User-Id} /
 * {@code X-User-Permissions} 因缺少共享密钥而无法通过校验（S8.1）。
 * 用户 ID 或租户标识缺失一律视为未登录（TODO_0005），不再静默回落到默认租户。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class GatewayAuthIntegrationImpl implements AuthIntegration {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayAuthIntegrationImpl.class);

    /**
     * 身份载荷字段分隔符
     */
    private static final String PAYLOAD_SEPARATOR = "\n";

    /**
     * 网关签名校验器
     */
    private final GatewaySignatureVerifier signatureVerifier;

    /**
     * 构造注入签名校验器。
     *
     * @param signatureVerifier 网关签名校验器
     */
    public GatewayAuthIntegrationImpl(GatewaySignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    /**
     * 从请求头解析可信操作人上下文。
     *
     * @param headers 请求头键值
     * @return 可信操作人上下文
     * @throws TodoBizException 身份或租户缺失、签名校验失败时抛出 TODO_0005
     */
    @Override
    public OperatorContext resolveOperator(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        String userId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_ID));
        String tenantId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_TENANT_ID));
        if (userId.isEmpty() || tenantId.isEmpty()) {
            LOGGER.error("gateway identity header missing, userIdPresent={}, tenantIdPresent={}",
                    !userId.isEmpty(), !tenantId.isEmpty());
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        String rawUserName = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_NAME));
        List<String> permissions = parsePermissions(headers.get(TodoConstants.HEADER_USER_PERMISSIONS));
        signatureVerifier.verify(headers, buildCanonicalPayload(userId, rawUserName, tenantId, permissions));
        String userName = rawUserName.isEmpty()
                ? userId
                : TodoTextUtil.truncate(rawUserName, TodoConstants.MAX_CREATOR_NAME_LENGTH);
        return new OperatorContext(userId, userName, tenantId, permissions);
    }

    /**
     * 构造参与签名的规范化载荷：网关按同一顺序拼接并签名。
     *
     * @param userId      用户 ID
     * @param userName    用户展示名（trim 后原文，未截断）
     * @param tenantId    租户标识
     * @param permissions 权限点列表
     * @return 规范化载荷
     */
    private String buildCanonicalPayload(String userId, String userName, String tenantId,
                                         List<String> permissions) {
        return userId + PAYLOAD_SEPARATOR + userName + PAYLOAD_SEPARATOR + tenantId
                + PAYLOAD_SEPARATOR + String.join(TodoConstants.PERMISSION_SEPARATOR, permissions);
    }

    /**
     * 解析权限点列表，多个值以英文逗号分隔。
     *
     * @param rawPermissions 原始权限点头，可为 null
     * @return 权限点列表，永不为 null
     */
    private List<String> parsePermissions(String rawPermissions) {
        if (rawPermissions == null || rawPermissions.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> permissions = new ArrayList<>();
        String[] parts = rawPermissions.split(TodoConstants.PERMISSION_SEPARATOR);
        for (String part : parts) {
            String permission = TodoTextUtil.trimWhitespace(part);
            if (!permission.isEmpty()) {
                permissions.add(permission);
            }
        }
        return permissions;
    }
}
