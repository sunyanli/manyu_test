package com.dtazziboot.todoapp.common.enums;

/**
 * 待办事项错误码枚举
 *
 * @author AiWork
 * @date 2026/09/09
 */
public enum ErrorCodeEnum {

    /**
     * 事项名称不能为空
     */
    TODO_001("TODO_001", "事项名称不能为空"),

    /**
     * 事项名称长度超过 128 字符
     */
    TODO_002("TODO_002", "事项名称长度超过128字符"),

    /**
     * 事项描述长度超过 1024 字符
     */
    TODO_003("TODO_003", "事项描述长度超过1024字符"),

    /**
     * 未登录或登录态已失效
     */
    TODO_004("TODO_004", "未登录或登录态已失效"),

    /**
     * 系统异常
     */
    TODO_005("TODO_005", "系统异常，请稍后重试");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示信息
     */
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
