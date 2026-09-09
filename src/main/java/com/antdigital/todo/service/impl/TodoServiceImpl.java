package com.antdigital.todo.service.impl;

import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.dao.mapper.TodoItemMapper;
import com.antdigital.todo.model.dto.CreateTodoRequest;
import com.antdigital.todo.model.dto.TodoVO;
import com.antdigital.todo.model.entity.TodoItemDO;
import com.antdigital.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 待办事项服务实现类
 *
 * @author AiWork
 * @since 2026-09-09
 */
@Service
public class TodoServiceImpl implements TodoService {

    private static final Logger logger = LoggerFactory.getLogger(TodoServiceImpl.class);

    /**
     * 错误码：事项名称不能为空
     */
    private static final String ERROR_TITLE_EMPTY = "TODO_001";

    /**
     * 错误码：事项名称长度超限
     */
    private static final String ERROR_TITLE_TOO_LONG = "TODO_002";

    /**
     * 错误码：事项描述长度超限
     */
    private static final String ERROR_DESCRIPTION_TOO_LONG = "TODO_003";

    /**
     * 错误码：系统异常
     */
    private static final String ERROR_SYSTEM = "TODO_004";

    /**
     * 事项名称最大长度
     */
    private static final int TITLE_MAX_LENGTH = 200;

    /**
     * 描述最大长度
     */
    private static final int DESCRIPTION_MAX_LENGTH = 2000;

    private final TodoItemMapper todoItemMapper;

    /**
     * 构造注入Mapper
     *
     * @param todoItemMapper 待办事项Mapper
     */
    public TodoServiceImpl(TodoItemMapper todoItemMapper) {
        this.todoItemMapper = todoItemMapper;
    }

    /**
     * 创建待办事项
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 待办事项视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoVO createTodo(CreateTodoRequest request, Long userId) {
        // 参数校验
        validateRequest(request);

        // 构建数据对象
        LocalDateTime now = LocalDateTime.now();
        TodoItemDO todoItem = new TodoItemDO();
        todoItem.setTitle(request.getTitle());
        todoItem.setDescription(request.getDescription());
        todoItem.setUserId(userId);
        todoItem.setGmtCreate(now);
        todoItem.setGmtModified(now);

        // 持久化
        try {
            todoItemMapper.insert(todoItem);
            logger.info("创建待办事项成功: id={}, title={}, userId={}", todoItem.getId(), todoItem.getTitle(), userId);
        } catch (Exception e) {
            logger.error("创建待办事项失败: title={}, userId={}", request.getTitle(), userId, e);
            throw new BizException(ERROR_SYSTEM, "系统异常，请稍后重试");
        }

        return new TodoVO(todoItem.getId());
    }

    /**
     * 校验创建请求参数
     *
     * @param request 创建请求
     */
    private void validateRequest(CreateTodoRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ERROR_TITLE_EMPTY, "事项名称不能为空");
        }
        if (request.getTitle().length() > TITLE_MAX_LENGTH) {
            throw new BizException(ERROR_TITLE_TOO_LONG, "事项名称长度不能超过200字符");
        }
        if (request.getDescription() != null && request.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            throw new BizException(ERROR_DESCRIPTION_TOO_LONG, "事项描述长度不能超过2000字符");
        }
    }
}
