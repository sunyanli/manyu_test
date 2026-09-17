package com.antdigital.video.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.antdigital.video.common.constant.ErrorCodes;
import com.antdigital.video.common.model.ApiResponse;

/**
 * 全局异常处理：将异常转换为统一响应，错误信息含 errorCode/errorMessage/userTip。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        logger.warn("业务异常, errorCode: {}, errorMessage: {}", ex.getErrorCode(), ex.getMessage());
        return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getUserTip());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleValidationException(Exception ex) {
        logger.warn("参数校验失败, errorMessage: {}", ex.getMessage());
        return ApiResponse.error(ErrorCodes.VT_001, "参数校验失败", "请检查请求参数");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        logger.error("系统异常, errorMessage: {}", ex.getMessage(), ex);
        return ApiResponse.error(ErrorCodes.VT_003, "系统处理失败", "系统繁忙，请稍后重试");
    }
}