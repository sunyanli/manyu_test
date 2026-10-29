package com.antdigital.todo.integration.impl;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.util.TodoTextUtil;
import com.antdigital.todo.integration.AuthIntegration;
import com.antdigital.todo.model.dto.OperatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 登录态与身份解析实现（I01）：读取统一网关注入的身份头。
 *
 * <p>身份字段只信任网关注入值，请求体中的同名字段一律忽略（R05）。
 * {@code auth.verify.enabled=true} 时要求登录票据头存在，用于网关无法透传可信身份的部署形态。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class GatewayAuthIntegrationImpl implements AuthIntegration {

    /**
     * 是否开启二次票据校验
     */
    private final boolean verifyTicketEnabled;

    /**
     * 生产构造：票据校验开关由配置项 auth.verify.enabled 控制，默认关闭（信任网关）。
     *
     * @param verifyTicketEnabled 是否开启二次票据校验
     */
    public GatewayAuthIntegrationImpl(
            @Value("${auth.verify.enabled:false}") boolean verifyTicketEnabled) {
        this.verifyTicketEnabled = verifyTicketEnabled;
    }

    /**
     * 从请求头解析可信操作人上下文。
     *
     * @param headers 请求头键值
     * @return 可信操作人上下文
     * @throws TodoBizException 用户身份缺失或票据缺失时抛出 TODO_0005
     */
    @Override
    public OperatorContext resolveOperator(Map<String, String> headers) {
        if (headers == null) {
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        String userId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_ID));
        if (userId.isEmpty()) {
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        if (verifyTicketEnabled) {
            String ticket = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_AUTH_TICKET));
            if (ticket.isEmpty()) {
                throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
            }
        }
        String userName = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_NAME));
        if (userName.isEmpty()) {
            userName = userId;
        }
        userName = TodoTextUtil.truncate(userName, TodoConstants.MAX_CREATOR_NAME_LENGTH);
        String tenantId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_TENANT_ID));
        if (tenantId.isEmpty()) {
            tenantId = TodoConstants.DEFAULT_TENANT_ID;
        }
        return new OperatorContext(userId, userName, tenantId, parseRoles(headers.get(TodoConstants.HEADER_USER_ROLES)));
    }

    /**
     * 解析角色/权限点列表，多个值以英文逗号分隔。
     *
     * @param rawRoles 原始角色头，可为 null
     * @return 角色列表，永不为 null
     */
    private List<String> parseRoles(String rawRoles) {
        List<String> roles = new ArrayList<>();
        if (rawRoles == null || rawRoles.trim().isEmpty()) {
            return roles;
        }
        String[] parts = rawRoles.split(TodoConstants.ROLE_SEPARATOR);
        for (String part : parts) {
            String role = TodoTextUtil.trimWhitespace(part);
            if (!role.isEmpty()) {
                roles.add(role);
            }
        }
        return roles;
    }
}
