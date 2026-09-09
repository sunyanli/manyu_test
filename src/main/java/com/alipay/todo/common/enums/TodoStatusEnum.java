package com.alipay.todo.common.enums;

/**
 * 待办事项状态枚举
 *
 * @author AiWork
 */
public enum TodoStatusEnum {

    INIT("INIT", "待处理"),
    DONE("DONE", "已完成");

    private final String code;
    private final String desc;

    TodoStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 状态编码
     * @return 枚举对象
     * @throws IllegalArgumentException 当 code 不存在时抛出
     */
    public static TodoStatusEnum fromCode(String code) {
        for (TodoStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown TodoStatusEnum code: " + code);
    }
}