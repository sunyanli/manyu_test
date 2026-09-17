package com.antdigital.video.common.exception;

/**
 * 业务异常。错误码约定见 {@link com.antdigital.video.common.constant.ErrorCodes}。
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String userTip;

    public BusinessException(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, errorMessage);
    }

    public BusinessException(String errorCode, String errorMessage, String userTip) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.userTip = userTip;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserTip() {
        return userTip;
    }
}