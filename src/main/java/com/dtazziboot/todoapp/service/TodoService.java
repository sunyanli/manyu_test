package com.dtazziboot.todoapp.service;

import com.dtazziboot.todoapp.model.dto.TodoCreateRequest;
import com.dtazziboot.todoapp.model.dto.TodoCreateResult;

/**
 * 待办事项业务服务接口
 *
 * @author AiWork
 * @date 2026/09/09
 */
public interface TodoService {

    /**
     * 新增待办事项
     *
     * @param request 新增请求，包含事项名称和描述
     * @return 创建结果，包含生成的主键 ID 及事项详情
     */
    TodoCreateResult createTodo(TodoCreateRequest request);
}