package com.antdigital.todo.common.enums;

/**
 * 逻辑删除标记枚举，对应 todo_item.is_deleted。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public enum DeleteFlagEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    /**
     * 标记值
     */
    private final int code;

    /**
     * 标记描述
     */
    private final String desc;

    DeleteFlagEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取标记值。
     *
     * @return 标记值
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取标记描述。
     *
     * @return 标记描述
     */
    public String getDesc() {
        return desc;
    }
}
