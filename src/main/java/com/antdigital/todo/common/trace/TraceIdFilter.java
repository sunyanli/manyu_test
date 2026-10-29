package com.antdigital.todo.common.trace;

import com.antdigital.todo.item.constant.TodoConstants;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 链路追踪过滤器：请求入口生成/透传 traceId，并保证请求结束时清理 MDC。
 *
 * <p>放在过滤器而非拦截器，是因为拦截器 {@code preHandle} 抛异常时
 * {@code afterCompletion} 不会被回调，只有过滤器 {@code finally}
 * 才能保证任何路径都清理线程变量（G8.5）。</p>
 *
 * @author AiWork
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceContext.initTraceId(request.getHeader(TodoConstants.HEADER_TRACE_ID));
        response.setHeader(TodoConstants.HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
