package com.manyu.algodemo.common.exception;

/**
 * 业务异常：携带统一错误码，由全局异常处理器转为统一出参。
 */
public class BizException extends RuntimeException {

    /** 错误码。 */
    private final String code;

    /**
     * 使用错误码枚举构造。
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码枚举与自定义信息构造。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public String getCode() {
        return code;
    }
}
