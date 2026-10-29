package com.antdigital.todo.common.interceptor;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.config.TodoCreateSettings;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.security.GatewayIpMatcher;
import com.antdigital.todo.common.security.TrustedGatewayProperties;
import com.antdigital.todo.common.trace.TraceContext;
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
 * <p>任何拒绝路径都会清理 {@link OperatorContextHolder}，
 * 避免线程池复用导致的身份串号（G8.5）。</p>
 *
 * @author AiWork
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginInterceptor.class);

    /** 请求开始时间的请求属性键，用于 afterCompletion 计算耗时。 */
    public static final String ATTR_START_NANOS = LoginInterceptor.class.getName() + ".startNanos";

    /** 请求属性键：登录态用户 ID，用于拒绝路径的访问日志。 */
    public static final String ATTR_USER_ID = LoginInterceptor.class.getName() + ".userId";

    /** 请求属性键：登录态租户 ID，用于拒绝路径的访问日志。 */
    public static final String ATTR_TENANT_ID = LoginInterceptor.class.getName() + ".tenantId";

    private final TrustedGatewayProperties gatewayProperties;

    private final TodoCreateSettings createSettings;

    public LoginInterceptor(TrustedGatewayProperties gatewayProperties, TodoCreateSettings createSettings) {
        this.gatewayProperties = gatewayProperties;
        this.createSettings = createSettings;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startNanos = System.nanoTime();
        request.setAttribute(ATTR_START_NANOS, Long.valueOf(startNanos));
        try {
            String userId = trimToNull(request.getHeader(TodoConstants.HEADER_USER_ID));
            String tenantId = trimToNull(request.getHeader(TodoConstants.HEADER_TENANT_ID));
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_TENANT_ID, tenantId);

            // R11：来源 IP 必须在受信网关白名单内，白名单为空时 fail-closed 拒绝
            checkTrustedGateway(request);
            // R12：登录态 Header 必须非空且长度受限
            checkOperatorHeaders(request, userId, tenantId);
            // R13：请求参数不得与登录态归属不一致
            checkTenantParam(request, tenantId);
            // R09：总开关与灰度
            checkSwitchAndGray(tenantId);

            String userName = trimToNull(request.getHeader(TodoConstants.HEADER_USER_NAME));
            OperatorContextHolder.set(new OperatorContext(userId, userName == null ? userId : userName, tenantId));
            return true;
        } catch (BizException ex) {
            handleRejected(request, startNanos, ex.getResultCode());
            throw ex;
        } catch (RuntimeException ex) {
            handleRejected(request, startNanos, ResultCode.TODO_004);
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            logAccessLog(request, resolveResultCode(request));
        } finally {
            OperatorContextHolder.clear();
        }
    }

    /**
     * R11：来源 IP 必须在受信网关白名单内，白名单为空即拒绝（fail-closed）。
     *
     * @param request 当前请求
     */
    private void checkTrustedGateway(HttpServletRequest request) {
        List<String> trustedIps = gatewayProperties.getTrustedGatewayIps();
        String remoteAddr = request.getRemoteAddr();
        if (trustedIps == null || trustedIps.isEmpty()) {
            LOGGER.error("安全告警：受信网关白名单为空，按 fail-closed 拒绝全部请求, "
                            + "uri={}, remoteAddr={}",
                    request.getRequestURI(), remoteAddr);
            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
        }
        if (!GatewayIpMatcher.matches(trustedIps, remoteAddr)) {
            LOGGER.warn("安全告警：拒绝非受信网关来源请求, remoteAddr={}, uri={}",
                    remoteAddr, request.getRequestURI());
            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
        }
    }

    private void checkOperatorHeaders(HttpServletRequest request, String userId, String tenantId) {
        if (!isValidOperatorField(userId) || !isValidOperatorField(tenantId)) {
            LOGGER.warn("登录态缺失或非法, uri={}, traceId={}",
                    request.getRequestURI(), TraceContext.currentTraceId());
            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
        }
    }

    private void checkTenantParam(HttpServletRequest request, String tenantId) {
        String queryTenantId = trimToNull(request.getParameter("tenantId"));
        if (queryTenantId != null && !queryTenantId.equals(tenantId)) {
            throw new BizException(ResultCode.TODO_003);
        }
    }

    /**
     * R09：总开关与租户尾号灰度校验。
     *
     * @param tenantId 租户 ID
     */
    private void checkSwitchAndGray(String tenantId) {
        if (!createSettings.isCreateEnabled()) {
            throw new BizException(ResultCode.TODO_007);
        }
        if (!createSettings.isGrayEnabled()) {
            return;
        }
        String suffix = tenantId.substring(tenantId.length() - 1);
        if (!createSettings.grayTenantSuffix().contains(suffix)) {
            throw new BizException(ResultCode.TODO_006);
        }
    }

    /**
     * 拒绝路径统一收口：清理上下文并打印访问日志，保证不残留线程变量。
     *
     * @param request     当前请求
     * @param startNanos  请求开始时间
     * @param resultCode  拒绝结果码
     */
    private void handleRejected(HttpServletRequest request, long startNanos, ResultCode resultCode) {
        OperatorContextHolder.clear();
        request.setAttribute(TodoConstants.ATTR_RESULT_CODE, resultCode.getCode());
        logAccessLog(request, resultCode.getCode());
    }

    private String resolveResultCode(HttpServletRequest request) {
        Object resultCode = request.getAttribute(TodoConstants.ATTR_RESULT_CODE);
        return resultCode == null ? ResultCode.OK.getCode() : String.valueOf(resultCode);
    }

    private void logAccessLog(HttpServletRequest request, String resultCode) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        long costMillis = currentCostMillis(request);
        OperatorContext operator = OperatorContextHolder.get();
        Object userId = operator == null ? request.getAttribute(ATTR_USER_ID) : operator.getUserId();
        Object tenantId = operator == null ? request.getAttribute(ATTR_TENANT_ID) : operator.getTenantId();
        LOGGER.info("待办创建访问日志, traceId={}, uri={}, userId={}, tenantId={}, result={}, costMs={}",
                TraceContext.currentTraceId(), request.getRequestURI(), userId, tenantId, resultCode, costMillis);
    }

    private long currentCostMillis(HttpServletRequest request) {
        Object startNanos = request.getAttribute(ATTR_START_NANOS);
        if (!(startNanos instanceof Long)) {
            return 0L;
        }
        return (System.nanoTime() - ((Long) startNanos).longValue()) / TodoConstants.NANOS_PER_MILLI;
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
