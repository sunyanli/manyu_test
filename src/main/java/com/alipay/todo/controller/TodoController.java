package com.alipay.todo.controller;

import com.alipay.todo.common.exception.TodoException;
import com.alipay.todo.model.dto.TodoCreateRequest;
import com.alipay.todo.model.dto.TodoItemDTO;
import com.alipay.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 待办事项控制器
 *
 * @author AiWork
 */
@RestController
@RequestMapping("/api/todo")
@Tag(name = "待办事项", description = "待办事项管理接口")
public class TodoController {

    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping("/create")
    @Operation(summary = "新增待办事项", description = "创建一条待办事项记录，返回创建结果")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "401", description = "未登录或登录态失效")
    @ApiResponse(responseCode = "500", description = "系统异常")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody TodoCreateRequest request) {
        try {
            TodoItemDTO result = todoService.create(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", result.getId());
            data.put("name", result.getName());
            data.put("description", result.getDescription());
            data.put("status", result.getStatus());
            data.put("creator", result.getCreator());
            data.put("gmtCreate", result.getGmtCreate() != null ? result.getGmtCreate().toString() : null);

            Map<String, Object> response = new HashMap<>();
            response.put("code", "OK");
            response.put("msg", "SUCCESS");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (TodoException e) {
            logger.error("create todo error, errorCode: {}, message: {}", e.getErrorCode(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("code", e.getErrorCode());
            response.put("msg", e.getMessage());
            response.put("data", null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("create todo system error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "TODO_003");
            response.put("msg", "系统异常，请稍后重试");
            response.put("data", null);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}