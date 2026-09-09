package com.dtazziboot.todoapp.common.exception;

import com.dtazziboot.todoapp.common.enums.ErrorCodeEnum;

/**
 * 业务异常，携带业务错误码
 *
 * @author AiWork
 * @date 2026/09/09
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCodeEnum errorCode;

    /**
     * 使用错误码构造业务异常
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCodeEnum getErrorCode() {
        return errorCode;
    }
}
