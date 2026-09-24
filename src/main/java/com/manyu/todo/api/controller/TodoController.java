package com.manyu.todo.api.controller;

import com.manyu.todo.common.model.Result;
import com.manyu.todo.model.dto.TodoCreateRequest;
import com.manyu.todo.model.vo.TodoVO;
import com.manyu.todo.service.TodoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 待办事项控制器。
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 新增待办事项。
     *
     * @param request 新增请求（name 必填，description 选填）
     * @return 统一响应，data 为创建成功的待办事项
     */
    @PostMapping("/create")
    public Result<TodoVO> createTodo(@Valid @RequestBody TodoCreateRequest request) {
        return Result.success(todoService.createTodo(request));
    }
}
