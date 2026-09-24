package com.manyu.todo.service;

import com.manyu.todo.model.dto.TodoCreateRequest;
import com.manyu.todo.model.vo.TodoVO;

/**
 * 待办事项业务服务接口。
 *
 * @author AiWork
 */
public interface TodoService {

    /**
     * 新增待办事项。
     *
     * @param request 新增请求（事项名称、描述）
     * @return 创建成功的待办事项视图对象，含回填的自增 id
     */
    TodoVO createTodo(TodoCreateRequest request);
}
