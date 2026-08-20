package com.manyu.algodemo.common.exception;

import com.manyu.algodemo.common.web.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一将异常转为 {@code {code, msg, data}} 出参，避免堆栈信息泄露。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常处理。
     *
     * @param e 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    public CommonResponse<Void> handleBizException(BizException e) {
        return CommonResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理（@Valid 方法体校验）。
     *
     * @param e 校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public CommonResponse<Void> handleValidationException(Exception e) {
        String detail = extractValidationMessage(e);
        return CommonResponse.fail(ErrorCode.DEMO_001.getCode(), ErrorCode.DEMO_001.getDefaultMsg() + "：" + detail);
    }

    /**
     * 请求体不可读异常处理。
     *
     * @param e 请求体解析异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResponse<Void> handleUnreadableException(HttpMessageNotReadableException e) {
        return CommonResponse.fail(ErrorCode.DEMO_001.getCode(), ErrorCode.DEMO_001.getDefaultMsg());
    }

    /**
     * 兜底异常处理。
     *
     * @param e 未预期异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleException(Exception e) {
        LOGGER.error("系统异常", e);
        return CommonResponse.fail(ErrorCode.COMMON_500.getCode(), ErrorCode.COMMON_500.getDefaultMsg());
    }

    private String extractValidationMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            return ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            return ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (e instanceof ConstraintViolationException ex) {
            return ex.getMessage();
        }
        return "参数校验失败";
    }
}
