package com.antdigital.todo.common.interceptor;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.config.TodoCreateSettings;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.ratelimit.TokenBucketRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户级限流拦截器，保护单实例容量。
 *
 * @author AiWork
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final int PERMITS_PER_REQUEST = 1;

    private final TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

    private final TodoCreateSettings createSettings;

    public RateLimitInterceptor(TodoCreateSettings createSettings) {
        this.createSettings = createSettings;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        OperatorContext operator = OperatorContextHolder.get();
        if (operator == null) {
            return true;
        }
        boolean acquired = rateLimiter.tryAcquire(operator.getUserId(), PERMITS_PER_REQUEST,
                createSettings.rateLimitQps());
        if (!acquired) {
            LOGGER.warn("用户请求超过限流阈值, userId={}, uri={}",
                    operator.getUserId(), request.getRequestURI());
            throw new BizException(ResultCode.TODO_004, "请求过于频繁，请稍后重试");
        }
        return true;
    }
}
