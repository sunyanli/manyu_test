package com.alipay.todo.service.impl;

import com.alipay.todo.common.constant.SecurityContextHolder;
import com.alipay.todo.common.constant.TodoConstants;
import com.alipay.todo.common.enums.TodoStatusEnum;
import com.alipay.todo.common.exception.TodoException;
import com.alipay.todo.dao.mapper.TodoItemMapper;
import com.alipay.todo.model.dto.TodoCreateRequest;
import com.alipay.todo.model.dto.TodoItemDTO;
import com.alipay.todo.model.entity.TodoItemDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 待办事项业务服务实现
 *
 * @author AiWork
 */
@Service
public class TodoServiceImpl implements TodoService {

    private static final Logger logger = LoggerFactory.getLogger(TodoServiceImpl.class);

    private final TodoItemMapper todoItemMapper;

    public TodoServiceImpl(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoItemDTO create(TodoCreateRequest request) {
        // 参数校验
        validateRequest(request);

        // 组装实体
        TodoItemDO todoItem = buildEntity(request);

        // 持久化
        int affected = todoItemMapper.insert(todoItem);
        if (affected != 1) {
            logger.error("create todo failed, affected rows: {}, name: {}", affected, request.getName());
            throw new TodoException("TODO_003", "系统异常，创建待办事项失败");
        }

        logger.info("create todo success, id: {}, creator: {}", todoItem.getId(), todoItem.getCreator());

        // 转换DTO返回
        return convertToDTO(todoItem);
    }

    private void validateRequest(TodoCreateRequest request) {
        if (request == null) {
            throw new TodoException("TODO_001", "参数校验失败：请求不能为空");
        }
        String name = request.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new TodoException("TODO_001", "事项名称必填且不超过64字");
        }
        if (name.length() > TodoConstants.NAME_MAX_LEN) {
            throw new TodoException("TODO_001", "事项名称不超过64字");
        }
        if (request.getDescription() != null && request.getDescription().length() > TodoConstants.DESC_MAX_LEN) {
            throw new TodoException("TODO_001", "事项描述不超过512字");
        }
    }

    private TodoItemDO buildEntity(TodoCreateRequest request) {
        TodoItemDO todoItem = new TodoItemDO();
        todoItem.setTenantId(TodoConstants.DEFAULT_TENANT_ID);
        todoItem.setName(request.getName().trim());
        todoItem.setDescription(request.getDescription() != null ? request.getDescription() : "");
        todoItem.setStatus(TodoStatusEnum.INIT.getCode());
        todoItem.setCreator(getCurrentCreator());
        todoItem.setIsDeleted(0);
        todoItem.setGmtCreate(LocalDateTime.now());
        todoItem.setGmtModified(LocalDateTime.now());
        return todoItem;
    }

    private String getCurrentCreator() {
        // 从登录上下文获取当前用户工号
        // 假设通过统一登录拦截器设置
        String creator = SecurityContextHolder.getCreator();
        if (creator == null || creator.isEmpty()) {
            throw new TodoException("TODO_002", "请先登录");
        }
        return creator;
    }

    private TodoItemDTO convertToDTO(TodoItemDO todoItem) {
        TodoItemDTO dto = new TodoItemDTO();
        dto.setId(todoItem.getId());
        dto.setName(todoItem.getName());
        dto.setDescription(todoItem.getDescription());
        dto.setStatus(todoItem.getStatus());
        dto.setCreator(todoItem.getCreator());
        dto.setGmtCreate(todoItem.getGmtCreate());
        return dto;
    }
}