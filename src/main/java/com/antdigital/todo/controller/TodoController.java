package com.antdigital.todo.controller;

import com.antdigital.todo.common.result.Result;
import com.antdigital.todo.model.dto.CreateTodoRequest;
import com.antdigital.todo.model.dto.TodoVO;
import com.antdigital.todo.service.TodoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待办事项控制器
 *
 * @author AiWork
 * @since 2026-09-09
 */
@RestController
@RequestMapping("/api/todo")
public class TodoController {

    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

    private final TodoService todoService;

    /**
     * 构造注入服务
     *
     * @param todoService 待办事项服务
     */
    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 新增待办事项
     *
     * @param request 创建请求
     * @return 创建结果，包含todoId
     */
    @PostMapping("/create")
    public Result<TodoVO> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        logger.info("收到创建待办事项请求: title={}", request.getTitle());
        TodoVO todoVO = todoService.createTodo(request, 1L);
        logger.info("创建待办事项成功: todoId={}", todoVO.getTodoId());
        return Result.success("创建成功", todoVO);
    }
}
