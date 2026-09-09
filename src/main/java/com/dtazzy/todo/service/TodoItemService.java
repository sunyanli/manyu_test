package com.dtazzy.todo.service;

import com.dtazzy.todo.api.request.CreateTodoItemRequest;
import com.dtazzy.todo.dao.entity.TodoItemDO;

/**
 * 待办事项业务服务接口。
 */
public interface TodoItemService {

    /**
     * 创建待办事项。
     *
     * @param tenantId 租户/用户标识
     * @param request  创建请求
     * @return 创建的待办事项数据对象
     */
    TodoItemDO createTodoItem(String tenantId, CreateTodoItemRequest request);
}