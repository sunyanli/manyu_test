package com.antdigital.todo.service.component;

/**
 * 待办编号生成器（S04）：生成 TD + yyyyMMdd + 6 位当日序号。
 *
 * <p>不保证绝对唯一（降级场景可能与既有编号重复），唯一性由 {@code uk_todo_item_no} 兜底，
 * 冲突后由调用方重试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public interface TodoNoGenerator {

    /**
     * 生成下一个待办编号。
     *
     * @return 待办编号，形如 TD20261029000042
     */
    String nextTodoNo();
}
