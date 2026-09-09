package com.dtazziboot.todoapp.controller;

import com.dtazziboot.todoapp.common.model.ApiResult;
import com.dtazziboot.todoapp.model.dto.TodoCreateRequest;
import com.dtazziboot.todoapp.model.dto.TodoCreateResult;
import com.dtazziboot.todoapp.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待办事项控制器，提供 Web 控制台 oneapi 接口
 *
 * @author AiWork
 * @date 2026/09/09
 */
@RestController
@RequestMapping("/api/todo")
public class TodoController {

    /**
     * 待办事项业务服务
     */
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 新增待办事项（W01）
     *
     * @param request 新增请求，包含事项名称和描述
     * @return 创建结果
     */
    @PostMapping("/create")
    public ApiResult<TodoCreateResult> createTodo(@Valid @RequestBody TodoCreateRequest request) {
        TodoCreateResult result = todoService.createTodo(request);
        return ApiResult.success(result);
    }
}