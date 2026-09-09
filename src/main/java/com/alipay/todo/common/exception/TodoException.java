package com.alipay.todo.common.exception;

/**
 * 待办事项业务异常
 *
 * @author AiWork
 */
public class TodoException extends RuntimeException {

    private final String errorCode;

    public TodoException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TodoException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}