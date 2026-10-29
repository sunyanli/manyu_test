package com.antdigital.todo.common.interceptor;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.config.TodoCreateProperties;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RateLimitInterceptor} 单元测试。
 *
 * @author AiWork
 */
class RateLimitInterceptorTest {

    private static final int QPS = 10;

    @AfterEach
    void tearDown() {
        OperatorContextHolder.clear();
    }

    @Test
    @DisplayName("超出用户级限流阈值：抛 TODO_004")
    void should_throwTodo004_when_rateLimited() {
        // Arrange
        TodoCreateProperties properties = new TodoCreateProperties();
        properties.setRateLimitQps(QPS);
        RateLimitInterceptor interceptor = new RateLimitInterceptor(properties);
        OperatorContextHolder.set(new OperatorContext("U1001", "张三", "T01"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/todo/item/create");
        for (int i = 0; i < QPS; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        }

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_004);
    }

    @Test
    @DisplayName("无登录态上下文：直接放行，由登录拦截器负责拒绝")
    void should_pass_when_operatorAbsent() {
        // Arrange
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new TodoCreateProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/todo/item/create");

        // Act
        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // Assert
        assertThat(passed).isTrue();
    }
}
