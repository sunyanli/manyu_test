package com.antdigital.todo.common.enums;

/**
 * 待办模块错误码枚举，与系分 5.1.2 W01 错误码表一一对应。
 *
 * <p>说明：错误码段 TODO_0001~TODO_0099 由系分约定；接入公司统一错误码平台后，
 * 仅需在本枚举内调整映射，对外错误码契约保持不变。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public enum TodoErrorCodeEnum {

    /**
     * 请求参数缺失或格式非法（requestId/itemName 缺失、source 非法）
     */
    PARAM_INVALID("TODO_0001", "请求参数缺失或格式非法"),

    /**
     * 事项名称去空白后为空
     */
    ITEM_NAME_REQUIRED("TODO_0002", "事项名称不能为空"),

    /**
     * 事项名称超长（超过 128 字符）
     */
    ITEM_NAME_TOO_LONG("TODO_0003", "事项名称不能超过 128 个字符"),

    /**
     * 事项描述超长（超过 512 字符）
     */
    ITEM_DESC_TOO_LONG("TODO_0004", "事项描述不能超过 512 个字符"),

    /**
     * 未登录或登录态失效
     */
    NOT_LOGIN("TODO_0005", "登录态已失效，请重新登录"),

    /**
     * 无创建待办权限
     */
    NO_PERMISSION("TODO_0006", "暂无创建待办的权限"),

    /**
     * 待办编号生成冲突（重试一次仍冲突）
     */
    TODO_NO_CONFLICT("TODO_0007", "创建繁忙，请稍后重试"),

    /**
     * 存储异常，创建失败
     */
    STORAGE_ERROR("TODO_0008", "创建失败，请稍后重试"),

    /**
     * 请求过于频繁（限流）
     */
    RATE_LIMITED("TODO_0009", "操作过于频繁，请稍后重试");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 面向用户的提示信息
     */
    private final String msg;

    TodoErrorCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码，形如 TODO_0001
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取面向用户的提示信息。
     *
     * @return 提示信息
     */
    public String getMsg() {
        return msg;
    }
}
