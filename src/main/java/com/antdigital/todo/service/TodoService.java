package com.antdigital.todo.service;

import com.antdigital.todo.model.dto.CreateTodoRequest;
import com.antdigital.todo.model.dto.TodoVO;

/**
 * 待办事项服务接口
 *
 * @author AiWork
 * @since 2026-09-09
 */
public interface TodoService {

    /**
     * 创建待办事项
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 待办事项视图对象
     */
    TodoVO createTodo(CreateTodoRequest request, Long userId);
}
