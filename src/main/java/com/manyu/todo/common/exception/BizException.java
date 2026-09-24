package com.manyu.todo.common.exception;

/**
 * 业务异常。
 *
 * <p>code 为 5 位错误码：错误产生来源字母（A-用户，B-系统，C-第三方）+ 4 位数字编号。
 *
 * @author AiWork
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
