package com.alipay.xchain.web.demo.common;

/**
 * 演示模块业务异常
 *
 * @author AiWork
 * @since 2026-10-30
 */
public class DemoException extends RuntimeException {

    private final String errorCode;

    public DemoException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "DemoException{errorCode='" + errorCode + "', message='" + getMessage() + "'}";
    }
}