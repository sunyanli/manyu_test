package com.dtazziboot.todoapp.common.exception;

import com.dtazziboot.todoapp.common.enums.ErrorCodeEnum;
import com.dtazziboot.todoapp.common.model.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author AiWork
 * @date 2026/09/09
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("业务异常: code={}, msg={}", e.getErrorCode().getCode(), e.getMessage());
        }
        return ApiResult.fail(e.getErrorCode());
    }

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError == null) {
            return ApiResult.fail(ErrorCodeEnum.TODO_001);
        }
        String field = fieldError.getField();
        String code = fieldError.getCode();
        if ("title".equals(field)) {
            if ("NotBlank".equals(code)) {
                return ApiResult.fail(ErrorCodeEnum.TODO_001);
            }
            return ApiResult.fail(ErrorCodeEnum.TODO_002);
        }
        return ApiResult.fail(ErrorCodeEnum.TODO_003);
    }

    /**
     * 处理未知异常
     *
     * @param e 未知异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnknownException(Exception e) {
        LOGGER.error("系统异常: errorMessage={}", e.getMessage(), e);
        return ApiResult.fail(ErrorCodeEnum.TODO_005);
    }
}
