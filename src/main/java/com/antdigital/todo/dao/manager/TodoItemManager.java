package com.antdigital.todo.dao.manager;

import com.antdigital.todo.common.exception.TodoDuplicateKeyException;
import com.antdigital.todo.dao.entity.TodoItemDO;
import com.antdigital.todo.dao.mapper.TodoItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 待办数据访问编排（Manager 层）：对 Mapper 做异常语义转换，使业务层不感知持久化框架异常。
 *
 * <p>MyBatis-Spring 会把数据库唯一约束冲突翻译为 {@link DuplicateKeyException}，
 * 本类将其转换为 {@link TodoDuplicateKeyException} 并尽力解析冲突索引名，供业务层
 * 区分「幂等键冲突」与「编号冲突」。索引名解析仅用于日志与判断提示，**不作为正确性依据**：
 * 解析失败时业务层仍会先做幂等回查，避免把幂等冲突误判为编号冲突。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoItemManager {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoItemManager.class);

    /**
     * 待办数据访问
     */
    private final TodoItemMapper todoItemMapper;

    /**
     * 构造注入 Mapper。
     *
     * @param todoItemMapper 待办数据访问
     */
    public TodoItemManager(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    /**
     * 按租户 + 幂等键查询未删除的待办。
     *
     * @param query 查询条件载体
     * @return 命中的待办，未命中返回 null
     */
    public TodoItemDO selectByRequestId(TodoItemDO query) {
        return todoItemMapper.selectByRequestId(query);
    }

    /**
     * 按租户 + 幂等键加锁查询未删除的待办，用于唯一键冲突后的幂等回查。
     *
     * @param query 查询条件载体
     * @return 命中的待办，未命中返回 null
     */
    public TodoItemDO selectByRequestIdForUpdate(TodoItemDO query) {
        return todoItemMapper.selectByRequestIdForUpdate(query);
    }

    /**
     * 新增待办，唯一键冲突转换为业务可识别的异常。
     *
     * @param todoItem 待办实体，自增主键回填至 id
     * @throws TodoDuplicateKeyException 触发唯一索引约束时抛出
     */
    public void insert(TodoItemDO todoItem) {
        try {
            todoItemMapper.insert(todoItem);
        } catch (DuplicateKeyException e) {
            String constraintName = resolveConstraintName(e);
            LOGGER.warn("duplicate key on todo_item insert, constraint={}, todoNo={}",
                    constraintName, todoItem.getTodoNo());
            throw new TodoDuplicateKeyException(constraintName);
        }
    }

    /**
     * 从数据库异常信息中解析冲突的唯一索引名称。
     *
     * @param e 唯一键冲突异常
     * @return 冲突索引名；无法识别时返回空串（业务层按未知冲突处理：先幂等回查再重试）
     */
    private String resolveConstraintName(DuplicateKeyException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        if (message == null) {
            return "";
        }
        if (message.contains(TodoDuplicateKeyException.UK_REQUEST_ID)) {
            return TodoDuplicateKeyException.UK_REQUEST_ID;
        }
        if (message.contains(TodoDuplicateKeyException.UK_TODO_NO)) {
            return TodoDuplicateKeyException.UK_TODO_NO;
        }
        return "";
    }
}
