package com.example.todo.service.impl;

import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.entity.TodoItem;
import com.example.todo.mapper.TodoItemMapper;
import com.example.todo.service.TodoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TodoItemServiceImpl implements TodoItemService {

    private final TodoItemMapper todoItemMapper;

    @Override
    public TodoItem createTodo(TodoCreateRequest request) {
        TodoItem todoItem = new TodoItem();
        todoItem.setTitle(request.getTitle());
        todoItem.setDescription(request.getDescription());
        todoItem.setCreateTime(LocalDateTime.now());
        todoItem.setUpdateTime(LocalDateTime.now());
        todoItemMapper.insert(todoItem);
        return todoItem;
    }
}
