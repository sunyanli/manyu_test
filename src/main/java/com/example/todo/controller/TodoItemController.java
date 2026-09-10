package com.example.todo.controller;

import com.example.todo.common.ApiResponse;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.entity.TodoItem;
import com.example.todo.service.TodoItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todo-items")
@RequiredArgsConstructor
public class TodoItemController {

    private final TodoItemService todoItemService;

    @PostMapping
    public ApiResponse<TodoItem> createTodo(@Valid @RequestBody TodoCreateRequest request) {
        TodoItem todoItem = todoItemService.createTodo(request);
        return ApiResponse.success(todoItem);
    }
}
