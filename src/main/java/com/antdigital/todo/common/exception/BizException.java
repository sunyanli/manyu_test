package com.antdigital.todo.common.exception;

import com.antdigital.todo.common.api.ResultCode;

/**
 * 业务异常，携带对外结果码。
 *
 * @author AiWork
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
