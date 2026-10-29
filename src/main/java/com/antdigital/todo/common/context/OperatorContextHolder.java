package com.antdigital.todo.common.context;

/**
 * 操作人上下文持有器，绑定当前请求线程。
 *
 * @author AiWork
 */
public final class OperatorContextHolder {

    private static final ThreadLocal<OperatorContext> HOLDER = new ThreadLocal<OperatorContext>();

    private OperatorContextHolder() {
    }

    public static void set(OperatorContext context) {
        HOLDER.set(context);
    }

    public static OperatorContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
