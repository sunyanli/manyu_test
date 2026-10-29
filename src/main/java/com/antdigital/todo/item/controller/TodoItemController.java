package com.antdigital.todo.item.controller;

import com.antdigital.todo.common.api.ApiResponse;
import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.item.model.CreateTodoRequest;
import com.antdigital.todo.item.model.CreateTodoResponse;
import com.antdigital.todo.item.service.TodoItemService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 待办事项 oneapi：新增待办。
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/todo/item")
public class TodoItemController {

    private final TodoItemService todoItemService;

    public TodoItemController(TodoItemService todoItemService) {
        this.todoItemService = todoItemService;
    }

    /**
     * W01 新增待办事项。
     *
     * @param request 新增请求
     * @return 统一出参，data 为新建记录信息
     */
    @PostMapping("/create")
    public ApiResponse<CreateTodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        OperatorContext operator = OperatorContextHolder.get();
        if (operator == null) {
            throw new BizException(ResultCode.TODO_002);
        }
        return ApiResponse.ok(todoItemService.createTodo(request, operator));
    }
}
