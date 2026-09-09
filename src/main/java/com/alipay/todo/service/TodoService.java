package com.alipay.todo.service;

import com.alipay.todo.model.dto.TodoCreateRequest;
import com.alipay.todo.model.dto.TodoItemDTO;

/**
 * 待办事项业务服务接口
 *
 * @author AiWork
 */
public interface TodoService {

    /**
     * 创建待办事项
     *
     * @param request 创建请求
     * @return 创建成功的待办事项DTO
     */
    TodoItemDTO create(TodoCreateRequest request);
}