package com.antdigital.todo.common.exception;

import com.antdigital.todo.common.api.ApiResponse;
import com.antdigital.todo.common.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 全局异常处理，统一转换为 {@link ApiResponse} 出参结构。
 *
 * @author AiWork
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验失败：返回首个字段错误提示。
     *
     * @param ex 校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.TODO_001.getMessage() : fieldError.getDefaultMessage();
        return ApiResponse.fail(ResultCode.TODO_001.getCode(), message);
    }

    /**
     * 请求体无法解析（非 JSON 或字段类型不符）。
     *
     * @param ex 解析异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        LOGGER.warn("请求体解析失败, cause={}", ex.getMessage());
        return ApiResponse.fail(ResultCode.TODO_001.getCode(), "请求参数格式不正确");
    }

    /**
     * 业务异常：使用异常自带结果码。
     *
     * @param ex 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        LOGGER.warn("业务异常, code={}, message={}", ex.getResultCode().getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getResultCode().getCode(), ex.getMessage());
    }

    /**
     * 兜底异常：不暴露内部细节。
     *
     * @param ex 未知异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception ex) {
        LOGGER.error("系统异常", ex);
        return ApiResponse.fail(ResultCode.TODO_004.getCode(), ResultCode.TODO_004.getMessage());
    }
}
