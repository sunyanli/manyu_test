package com.antdigital.todo.common.exception;

import com.antdigital.todo.common.api.ApiResponse;
import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.metrics.TodoCreateMetrics;
import com.antdigital.todo.common.trace.TraceContext;
import com.antdigital.todo.item.constant.TodoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理，统一转换为 {@link ApiResponse} 出参结构。
 *
 * <p>失败分支统一登记结果码请求属性与错误码分桶指标，并输出 traceId
 * 便于串联排障。</p>
 *
 * @author AiWork
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TodoCreateMetrics todoCreateMetrics;

    public GlobalExceptionHandler(TodoCreateMetrics todoCreateMetrics) {
        this.todoCreateMetrics = todoCreateMetrics;
    }

    /**
     * 参数校验失败：返回首个字段错误提示。
     *
     * @param ex      校验异常
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(BindException ex, HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.TODO_001.getMessage() : fieldError.getDefaultMessage();
        return fail(request, ResultCode.TODO_001, message);
    }

    /**
     * 请求体无法解析（非 JSON 或字段类型不符）。
     *
     * @param ex      解析异常
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        LOGGER.warn("请求体解析失败, traceId={}, cause={}", TraceContext.currentTraceId(), ex.getMessage());
        return fail(request, ResultCode.TODO_001, "请求参数格式不正确");
    }

    /**
     * 业务异常：使用异常自带结果码。
     *
     * @param ex      业务异常
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex, HttpServletRequest request) {
        LOGGER.warn("业务异常, traceId={}, code={}, message={}", TraceContext.currentTraceId(),
                ex.getResultCode().getCode(), ex.getMessage());
        return fail(request, ex.getResultCode(), ex.getMessage());
    }

    /**
     * 兜底异常：不暴露内部细节。
     *
     * @param ex      未知异常
     * @param request 当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception ex, HttpServletRequest request) {
        LOGGER.error("系统异常, traceId={}", TraceContext.currentTraceId(), ex);
        return fail(request, ResultCode.TODO_004, ResultCode.TODO_004.getMessage());
    }

    private ApiResponse<Void> fail(HttpServletRequest request, ResultCode resultCode, String message) {
        request.setAttribute(TodoConstants.ATTR_RESULT_CODE, resultCode.getCode());
        todoCreateMetrics.recordFailure(resultCode.getCode());
        return ApiResponse.fail(resultCode.getCode(), message);
    }
}
