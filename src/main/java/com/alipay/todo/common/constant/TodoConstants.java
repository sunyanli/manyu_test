package com.alipay.todo.common.constant;

/**
 * 待办事项常量定义
 *
 * @author AiWork
 */
public final class TodoConstants {

    public static final int NAME_MAX_LEN = 64;
    public static final int DESC_MAX_LEN = 512;
    public static final String DEFAULT_TENANT_ID = "default";
    public static final String TODO_CREATE_ENABLED = "todo.create.enabled";

    private TodoConstants() {
        throw new IllegalStateException("Utility class");
    }
}