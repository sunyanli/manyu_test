package com.antdigital.todo.item.enums;

/**
 * 待办状态，本期仅创建，固定为待办。
 *
 * @author AiWork
 */
public enum TodoStatus {

    /** 待办（未完成）。 */
    TODO(0, "待办");

    private final int code;

    private final String desc;

    TodoStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
