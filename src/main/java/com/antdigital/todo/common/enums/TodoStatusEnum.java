package com.antdigital.todo.common.enums;

/**
 * 待办状态枚举，对应 todo_item.status。
 *
 * <p>本期仅实现创建（状态固定为待处理），已完成/已取消为预留终态。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public enum TodoStatusEnum {

    /**
     * 待处理（创建后默认状态）
     */
    PENDING(0, "待处理"),

    /**
     * 已完成（本期不提供流转入口，预留）
     */
    FINISHED(1, "已完成"),

    /**
     * 已取消（本期不提供流转入口，预留）
     */
    CANCELLED(2, "已取消");

    /**
     * 状态编码
     */
    private final int code;

    /**
     * 状态描述
     */
    private final String desc;

    TodoStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述
     */
    public String getDesc() {
        return desc;
    }
}
