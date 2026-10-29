package com.antdigital.todo.common.interceptor;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.config.TodoCreateProperties;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.security.TrustedGatewayProperties;
import com.antdigital.todo.item.constant.TodoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 登录态拦截器：校验网关来源与登录 Header，写入操作人上下文，并完成开关/灰度校验。
 *
 * @author AiWork
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginInterceptor.class);

    private final TrustedGatewayProperties gatewayProperties;

    private final TodoCreateProperties createProperties;

    public LoginInterceptor(TrustedGatewayProperties gatewayProperties,
                            TodoCreateProperties createProperties) {
        this.gatewayProperties = gatewayProperties;
        this.createProperties = createProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // R11：来源 IP 必须在受信网关白名单内（白名单为空时由网络层保证）
        List<String> trustedIps = gatewayProperties.getTrustedGatewayIps();
        if (!trustedIps.isEmpty() && !trustedIps.contains(request.getRemoteAddr())) {
            LOGGER.warn("拒绝非受信网关来源请求, remoteAddr={}, uri={}",
                    request.getRemoteAddr(), request.getRequestURI());
            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
        }

        // R12：登录态 Header 必须非空且长度受限
        String userId = trimToNull(request.getHeader(TodoConstants.HEADER_USER_ID));
        String tenantId = trimToNull(request.getHeader(TodoConstants.HEADER_TENANT_ID));
        if (!isValidOperatorField(userId) || !isValidOperatorField(tenantId)) {
            LOGGER.warn("登录态缺失或非法, uri={}, traceId={}",
                    request.getRequestURI(), request.getHeader(TodoConstants.HEADER_TRACE_ID));
            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
        }
        String userName = trimToNull(request.getHeader(TodoConstants.HEADER_USER_NAME));
        if (userName == null) {
            userName = userId;
        }

        // R13：请求参数不得与登录态归属不一致
        String queryTenantId = trimToNull(request.getParameter("tenantId"));
        if (queryTenantId != null && !queryTenantId.equals(tenantId)) {
            throw new BizException(ResultCode.TODO_003);
        }

        OperatorContextHolder.set(new OperatorContext(userId, userName, tenantId));
        checkSwitchAndGray(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        OperatorContextHolder.clear();
    }

    /**
     * R09：总开关与租户尾号灰度校验。
     *
     * @param tenantId 租户 ID
     */
    private void checkSwitchAndGray(String tenantId) {
        if (!createProperties.isEnabled()) {
            throw new BizException(ResultCode.TODO_007);
        }
        if (!createProperties.isGrayEnabled()) {
            return;
        }
        String suffix = tenantId.substring(tenantId.length() - 1);
        if (!createProperties.getGrayTenantSuffix().contains(suffix)) {
            throw new BizException(ResultCode.TODO_006);
        }
    }

    private boolean isValidOperatorField(String value) {
        return value != null && value.length() <= TodoConstants.OPERATOR_FIELD_MAX_LEN;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
