package com.manyu.todo.common.exception;

import com.manyu.todo.common.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：将异常转化为统一响应结构。
 *
 * @author AiWork
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        LOGGER.warn("业务异常, code: {}, message: {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("参数校验失败");
        LOGGER.warn("参数校验失败, message: {}", message);
        return Result.fail("A0400", message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception e) {
        LOGGER.error("系统异常, errorMessage: {}", e.getMessage(), e);
        return Result.fail("B0001", "系统繁忙，请稍后再试");
    }
}
