package com.antdigital.todo.item.service;

import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.item.model.CreateTodoRequest;
import com.antdigital.todo.item.model.CreateTodoResponse;

/**
 * 待办事项业务服务。
 *
 * @author AiWork
 */
public interface TodoItemService {

    /**
     * 创建一条待办事项。
     *
     * @param request  新增请求
     * @param operator 登录态操作人
     * @return 新建记录信息
     */
    CreateTodoResponse createTodo(CreateTodoRequest request, OperatorContext operator);
}
