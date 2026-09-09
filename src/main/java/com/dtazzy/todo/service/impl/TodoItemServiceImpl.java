package com.dtazzy.todo.service.impl;

import com.dtazzy.todo.api.request.CreateTodoItemRequest;
import com.dtazzy.todo.common.exception.BusinessException;
import com.dtazzy.todo.dao.entity.TodoItemDO;
import com.dtazzy.todo.dao.mapper.TodoItemMapper;
import com.dtazzy.todo.service.TodoItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 待办事项业务服务实现。
 */
@Service
public class TodoItemServiceImpl implements TodoItemService {

    private static final Logger logger = LoggerFactory.getLogger(TodoItemServiceImpl.class);

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final TodoItemMapper todoItemMapper;

    public TodoItemServiceImpl(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    @Override
    public TodoItemDO createTodoItem(String tenantId, CreateTodoItemRequest request) {
        validateTenantId(tenantId);
        validateName(request.getName());
        validateDescription(request.getDescription());

        TodoItemDO todoItem = new TodoItemDO();
        todoItem.setTenantId(tenantId);
        todoItem.setName(request.getName());
        todoItem.setDescription(request.getDescription());

        try {
            todoItemMapper.insert(todoItem);
            logger.info("待办事项创建成功, id: {}, tenantId: {}", todoItem.getId(), tenantId);
            return todoItem;
        } catch (Exception e) {
            logger.error("待办事项创建失败, tenantId: {}, name: {}", tenantId, request.getName(), e);
            throw new BusinessException("TODO_004", "系统繁忙，请稍后重试", e);
        }
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException("TODO_005", "租户标识不能为空");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("TODO_001", "事项名称不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("TODO_002", "事项名称长度不能超过 200 字符");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException("TODO_003", "事项描述长度不能超过 2000 字符");
        }
    }
}