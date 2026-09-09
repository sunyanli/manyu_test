package com.antdigital.todo.common.exception;

/**
 * 业务异常
 *
 * @author AiWork
 * @since 2026-09-09
 */
public class BizException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 构造业务异常
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public BizException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}
