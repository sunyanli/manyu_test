package com.antdigital.todo.common.exception;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;

/**
 * 待办业务异常：承载业务错误码与面向用户的提示信息，由统一异常处理器转换为响应体。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoBizException extends RuntimeException {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码
     */
    private final String code;

    /**
     * 使用错误码枚举构造异常，提示信息取枚举默认文案。
     *
     * @param errorCode 错误码枚举
     */
    public TodoBizException(TodoErrorCodeEnum errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码枚举与自定义提示构造异常。
     *
     * @param errorCode 错误码枚举
     * @param msg       面向用户的提示信息
     */
    public TodoBizException(TodoErrorCodeEnum errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码，形如 TODO_0001
     */
    public String getCode() {
        return code;
    }
}
