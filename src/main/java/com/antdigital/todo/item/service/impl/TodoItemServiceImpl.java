package com.antdigital.todo.item.service.impl;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.item.constant.TodoConstants;
import com.antdigital.todo.item.enums.TodoStatus;
import com.antdigital.todo.item.mapper.TodoItemMapper;
import com.antdigital.todo.item.model.CreateTodoRequest;
import com.antdigital.todo.item.model.CreateTodoResponse;
import com.antdigital.todo.item.model.TodoItemDO;
import com.antdigital.todo.item.service.TodoItemService;
import com.antdigital.todo.item.util.TodoTextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * 待办事项业务实现：文本规范化、业务规则校验、实体组装与落库。
 *
 * @author AiWork
 */
@Service
public class TodoItemServiceImpl implements TodoItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TodoItemServiceImpl.class);

    private static final int EXPECTED_AFFECTED_ROWS = 1;

    private final TodoItemMapper todoItemMapper;

    public TodoItemServiceImpl(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    @Override
    public CreateTodoResponse createTodo(CreateTodoRequest request, OperatorContext operator) {
        if (request == null || operator == null) {
            throw new BizException(ResultCode.TODO_001, "请求参数不能为空");
        }

        // R05/R06：文本规范化并拒绝非法控制字符
        String title = TodoTextNormalizer.normalize(request.getTitle());
        String description = TodoTextNormalizer.normalize(request.getDescription());
        checkIllegalText(title, description);

        // R02/R03：业务长度校验（按字符计，首尾空白已裁剪）
        checkTitleLength(title);
        checkDescriptionLength(description);

        // R07/R08：归属与状态只能由服务端注入
        TodoItemDO record = buildRecord(title, description, operator);
        int affectedRows = insertRecord(record);
        if (affectedRows != EXPECTED_AFFECTED_ROWS) {
            LOGGER.error("待办事项落库影响行数异常, affectedRows={}, tenantId={}, creatorId={}",
                    affectedRows, operator.getTenantId(), operator.getUserId());
            throw new BizException(ResultCode.TODO_004);
        }
        return buildResponse(record);
    }

    private void checkIllegalText(String title, String description) {
        if (TodoTextNormalizer.containsIllegalChar(title) || TodoTextNormalizer.containsIllegalChar(description)) {
            throw new BizException(ResultCode.TODO_005);
        }
    }

    private void checkTitleLength(String title) {
        int titleLength = TodoTextNormalizer.charLength(title);
        if (titleLength == 0) {
            throw new BizException(ResultCode.TODO_001, "事项名称不能为空");
        }
        if (titleLength > TodoConstants.TITLE_MAX_LEN) {
            throw new BizException(ResultCode.TODO_001, "事项名称不能超过100个字符");
        }
    }

    private void checkDescriptionLength(String description) {
        if (TodoTextNormalizer.charLength(description) > TodoConstants.DESC_MAX_LEN) {
            throw new BizException(ResultCode.TODO_001, "事项描述不能超过500个字符");
        }
    }

    private TodoItemDO buildRecord(String title, String description, OperatorContext operator) {
        LocalDateTime now = ZonedDateTime.now(TodoConstants.ZONE_UTC8).toLocalDateTime();
        TodoItemDO record = new TodoItemDO();
        record.setTenantId(operator.getTenantId());
        record.setTitle(title);
        record.setDescription(description);
        record.setStatus(TodoStatus.TODO.getCode());
        record.setCreatorId(operator.getUserId());
        record.setCreatorName(resolveCreatorName(operator));
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private String resolveCreatorName(OperatorContext operator) {
        String userName = operator.getUserName();
        if (userName == null || userName.trim().isEmpty()) {
            return operator.getUserId();
        }
        return userName;
    }

    private int insertRecord(TodoItemDO record) {
        try {
            return todoItemMapper.insert(record);
        } catch (DataAccessException ex) {
            // R10：持久化异常统一转 TODO_004，不对外暴露 SQL 细节
            LOGGER.error("待办事项写库失败, tenantId={}, creatorId={}", record.getTenantId(),
                    record.getCreatorId(), ex);
            throw new BizException(ResultCode.TODO_004);
        }
    }

    private CreateTodoResponse buildResponse(TodoItemDO record) {
        CreateTodoResponse response = new CreateTodoResponse();
        response.setId(record.getId());
        response.setTitle(record.getTitle());
        response.setStatus(record.getStatus());
        response.setGmtCreate(record.getGmtCreate().format(TodoConstants.DATE_TIME_FORMATTER));
        return response;
    }
}
