package com.antdigital.todo;

import com.antdigital.todo.common.exception.GlobalExceptionHandler;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 *
 * @author AiWork
 * @since 2026-09-09
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_returnFailResult_when_bizException() {
        // Act
        Result<Void> result = handler.handleBizException(new BizException("TODO_001", "事项名称不能为空"));

        // Assert
        assertEquals("TODO_001", result.getCode());
        assertEquals("事项名称不能为空", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void should_returnFailResult_when_systemException() {
        // Act
        Result<Void> result = handler.handleException(new RuntimeException("db error"));

        // Assert
        assertEquals("TODO_004", result.getCode());
        assertEquals("系统异常，请稍后重试", result.getMsg());
    }
}
