package com.antdigital.todo.controller;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.model.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 统一异常处理器：把内部异常转换为「错误码 + 提示信息 + HTTP 状态」的响应体。
 *
 * <p>说明：错误码不直接作为用户提示（用户提示统一取枚举文案），
 * 异常日志包含案发现场信息与堆栈，且不记录事项名称/描述原文。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@RestControllerAdvice
public class TodoExceptionHandler {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoExceptionHandler.class);

    /**
     * 请求体字段名：幂等键
     */
    private static final String FIELD_REQUEST_ID = "requestId";

    /**
     * 请求体字段名：事项名称
     */
    private static final String FIELD_ITEM_NAME = "itemName";

    /**
     * 请求体字段名：事项描述
     */
    private static final String FIELD_ITEM_DESC = "itemDesc";

    /**
     * 业务异常处理：按错误码映射 HTTP 状态。
     *
     * @param e 业务异常
     * @return 统一响应体
     */
    @ExceptionHandler(TodoBizException.class)
    public ResponseEntity<ApiResult<Void>> handleBizException(TodoBizException e) {
        LOGGER.warn("todo biz exception, code={}, msg={}", e.getCode(), e.getMessage());
        TodoErrorCodeEnum errorCode = resolveByCode(e.getCode());
        return ResponseEntity.status(resolveHttpStatus(errorCode))
                .body(new ApiResult<>(e.getCode(), e.getMessage(), null));
    }

    /**
     * 请求体反序列化失败：统一按参数非法处理，不进入业务层。
     *
     * @param e 反序列化异常
     * @return 统一响应体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        LOGGER.warn("todo request body unreadable, cause={}", e.getMessage());
        return buildError(TodoErrorCodeEnum.PARAM_INVALID);
    }

    /**
     * 必填请求头缺失（身份头缺失场景）：按未登录处理，防止匿名写入。
     *
     * @param e 缺失请求头异常
     * @return 统一响应体
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        LOGGER.error("required header missing, header={}", e.getHeaderName());
        return buildError(TodoErrorCodeEnum.NOT_LOGIN);
    }

    /**
     * 参数类型不匹配：按参数非法处理。
     *
     * @param e 类型不匹配异常
     * @return 统一响应体
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        LOGGER.warn("todo argument type mismatch, name={}, value={}", e.getName(), e.getValue());
        return buildError(TodoErrorCodeEnum.PARAM_INVALID);
    }

    /**
     * 注解校验失败：按字段映射到精确的业务错误码。
     *
     * @param e 校验异常
     * @return 统一响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleInvalidArgument(MethodArgumentNotValidException e) {
        TodoErrorCodeEnum errorCode = TodoErrorCodeEnum.PARAM_INVALID;
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            LOGGER.warn("todo argument invalid, field={}", fieldError.getField());
            errorCode = resolveFieldCode(fieldError.getField());
        }
        return buildError(errorCode);
    }

    /**
     * 兜底异常处理：避免异常堆栈直出到前端。
     *
     * @param e 未预期异常
     * @return 统一响应体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception e) {
        LOGGER.error("todo unexpected error", e);
        return buildError(TodoErrorCodeEnum.STORAGE_ERROR);
    }

    /**
     * 按字段名映射错误码。
     *
     * @param field 字段名
     * @return 对应错误码
     */
    private TodoErrorCodeEnum resolveFieldCode(String field) {
        if (FIELD_REQUEST_ID.equals(field)) {
            return TodoErrorCodeEnum.PARAM_INVALID;
        }
        if (FIELD_ITEM_NAME.equals(field)) {
            return TodoErrorCodeEnum.ITEM_NAME_REQUIRED;
        }
        if (FIELD_ITEM_DESC.equals(field)) {
            return TodoErrorCodeEnum.ITEM_DESC_TOO_LONG;
        }
        return TodoErrorCodeEnum.PARAM_INVALID;
    }

    /**
     * 错误码字符串反查枚举，未知码归入存储异常。
     *
     * @param code 错误码字符串
     * @return 错误码枚举
     */
    private TodoErrorCodeEnum resolveByCode(String code) {
        for (TodoErrorCodeEnum errorCode : TodoErrorCodeEnum.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return TodoErrorCodeEnum.STORAGE_ERROR;
    }

    /**
     * 错误码映射 HTTP 状态。
     *
     * @param errorCode 错误码枚举
     * @return HTTP 状态
     */
    private HttpStatus resolveHttpStatus(TodoErrorCodeEnum errorCode) {
        switch (errorCode) {
            case PARAM_INVALID:
            case ITEM_NAME_REQUIRED:
            case ITEM_NAME_TOO_LONG:
            case ITEM_DESC_TOO_LONG:
                return HttpStatus.BAD_REQUEST;
            case NOT_LOGIN:
                return HttpStatus.UNAUTHORIZED;
            case NO_PERMISSION:
                return HttpStatus.FORBIDDEN;
            case RATE_LIMITED:
                return HttpStatus.TOO_MANY_REQUESTS;
            case TODO_NO_CONFLICT:
            case STORAGE_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 构造错误响应。
     *
     * @param errorCode 错误码枚举
     * @return 错误响应体
     */
    private ResponseEntity<ApiResult<Void>> buildError(TodoErrorCodeEnum errorCode) {
        return ResponseEntity.status(resolveHttpStatus(errorCode)).body(ApiResult.failure(errorCode));
    }
}
