package com.antdigital.todo.common.api;

/**
 * 统一结果码。
 *
 * <p>编码格式为 {@code {MODULE}_{SEQ}}，本模块前缀为 {@code TODO}。</p>
 *
 * @author AiWork
 */
public enum ResultCode {

    /** 成功。 */
    OK("OK", "SUCCESS"),

    /** 参数校验失败。 */
    TODO_001("TODO_001", "参数校验失败"),

    /** 未登录或登录态失效。 */
    TODO_002("TODO_002", "未登录或登录态失效"),

    /** 无权代表该用户/租户创建。 */
    TODO_003("TODO_003", "无权代表该用户/租户创建"),

    /** 系统繁忙。 */
    TODO_004("TODO_004", "系统繁忙，请稍后重试"),

    /** 内容包含非法字符。 */
    TODO_005("TODO_005", "内容包含非法字符"),

    /** 功能未开放（灰度未命中）。 */
    TODO_006("TODO_006", "功能未开放"),

    /** 功能已临时关闭。 */
    TODO_007("TODO_007", "功能已临时关闭");

    private final String code;

    private final String message;

    ResultCode(String code, String message) {
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
