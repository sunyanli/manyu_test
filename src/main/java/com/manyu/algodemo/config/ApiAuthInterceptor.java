package com.manyu.algodemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manyu.algodemo.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * /api 登录态校验拦截器（设计 6.4.2）。
 *
 * <p>演示环境（security.mock-caller-enabled=true）放行请求头模拟身份（A03）；
 * 正式环境禁用模拟通道后，要求网关侧注入的认证用户头，缺失返回 COMMON_401。</p>
 */
@Component
public class ApiAuthInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthInterceptor.class);

    /** 正式环境由统一登录网关注入的认证用户头。 */
    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    private final boolean mockCallerEnabled;
    private final ObjectMapper objectMapper;

    /**
     * 构造器注入。
     *
     * @param mockCallerEnabled 演示身份模拟通道开关
     * @param objectMapper      JSON 序列化器
     */
    public ApiAuthInterceptor(
            @Value("${security.mock-caller-enabled:true}") boolean mockCallerEnabled,
            ObjectMapper objectMapper) {
        this.mockCallerEnabled = mockCallerEnabled;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (mockCallerEnabled) {
            return true;
        }
        if (StringUtils.hasText(request.getHeader(HEADER_AUTH_USER_ID))) {
            return true;
        }
        LOGGER.warn("未登录访问被拦截: uri={}, remoteAddr={}",
                request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", ErrorCode.COMMON_401.getCode(),
                        "msg", ErrorCode.COMMON_401.getDefaultMsg(),
                        "data", (Object) null)));
        return false;
    }
}
