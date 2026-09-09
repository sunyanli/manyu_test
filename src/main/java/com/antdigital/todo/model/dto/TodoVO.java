package com.antdigital.todo.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 待办事项视图对象
 *
 * @author AiWork
 * @since 2026-09-09
 */
public class TodoVO implements Serializable {

    /**
     * 待办事项ID
     */
    @JsonProperty("todo_id")
    private Long todoId;

    public TodoVO() {
    }

    public TodoVO(Long todoId) {
        this.todoId = todoId;
    }

    public Long getTodoId() {
        return todoId;
    }

    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }
}
