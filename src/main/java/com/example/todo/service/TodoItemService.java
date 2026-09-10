package com.example.todo.service;

import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.entity.TodoItem;

public interface TodoItemService {

    TodoItem createTodo(TodoCreateRequest request);
}
