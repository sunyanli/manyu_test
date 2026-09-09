package com.dtazziboot.todoapp.service.impl;

import com.dtazziboot.todoapp.common.constant.TodoConstants;
import com.dtazziboot.todoapp.common.context.LoginContext;
import com.dtazziboot.todoapp.common.enums.ErrorCodeEnum;
import com.dtazziboot.todoapp.common.exception.BusinessException;
import com.dtazziboot.todoapp.dao.mapper.TodoItemMapper;
import com.dtazziboot.todoapp.model.dto.TodoCreateRequest;
import com.dtazziboot.todoapp.model.dto.TodoCreateResult;
import com.dtazziboot.todoapp.model.entity.TodoItemDO;
import com.dtazziboot.todoapp.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 待办事项业务服务实现
 *
 * @author AiWork
 * @date 2026/09/09
 */
@Service
public class TodoServiceImpl implements TodoService {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoServiceImpl.class);

    /**
     * 待办事项数据访问对象
     */
    private final TodoItemMapper todoItemMapper;

    public TodoServiceImpl(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    /**
     * {@inheritDoc}
     * 业务规则：
     * R01 - title 不能为空且长度 ≤128（由 @Valid 在 Controller 层拦截）
     * R02 - description 长度 ≤1024（由 @Valid 在 Controller 层拦截）
     * R03 - creator_id 从登录上下文获取，不可为空
     * R04 - tenant_id 从登录上下文获取，不可为空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoCreateResult createTodo(TodoCreateRequest request) {
        // 填充创建人 ID 和租户 ID（R03 / R04）
        String creatorId = LoginContext.getCreatorId();
        String tenantId = LoginContext.getTenantId();

        if (creatorId == null || creatorId.isBlank()) {
            LOGGER.warn("未登录或 creatorId 为空");
            throw new BusinessException(ErrorCodeEnum.TODO_004);
        }
        if (tenantId == null || tenantId.isBlank()) {
            LOGGER.warn("tenantId 为空");
            throw new BusinessException(ErrorCodeEnum.TODO_004);
        }

        // 构建数据对象
        TodoItemDO todoItem = new TodoItemDO();
        todoItem.setTitle(request.getTitle());
        todoItem.setDescription(request.getDescription());
        todoItem.setCreatorId(creatorId);
        todoItem.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now(TodoConstants.DEFAULT_ZONE_ID);
        todoItem.setGmtCreate(now);
        todoItem.setGmtModified(now);

        // 持久化
        int rows = todoItemMapper.insert(todoItem);
        if (rows != 1) {
            LOGGER.error("插入待办事项失败，影响行数={}", rows);
            throw new BusinessException(ErrorCodeEnum.TODO_005);
        }

        LOGGER.info("创建待办事项成功: id={}, title={}, creatorId={}",
                todoItem.getId(), todoItem.getTitle(), todoItem.getCreatorId());

        // 组装返回结果
        TodoCreateResult result = new TodoCreateResult();
        result.setId(todoItem.getId());
        result.setTitle(todoItem.getTitle());
        result.setDescription(todoItem.getDescription());
        result.setCreatorId(todoItem.getCreatorId());
        result.setTenantId(todoItem.getTenantId());
        result.setGmtCreate(todoItem.getGmtCreate());
        result.setGmtModified(todoItem.getGmtModified());

        return result;
    }
}