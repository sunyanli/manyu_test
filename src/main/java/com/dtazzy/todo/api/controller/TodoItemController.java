package com.dtazzy.todo.api.controller;

import com.dtazzy.todo.api.request.CreateTodoItemRequest;
import com.dtazzy.todo.common.exception.BusinessException;
import com.dtazzy.todo.dao.entity.TodoItemDO;
import com.dtazzy.todo.model.vo.TodoItemVO;
import com.dtazzy.todo.service.TodoItemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 待办事项控制器。
 */
@RestController
@RequestMapping("/api/todo-items")
public class TodoItemController {

    private static final Logger logger = LoggerFactory.getLogger(TodoItemController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TodoItemService todoItemService;

    public TodoItemController(TodoItemService todoItemService) {
        this.todoItemService = todoItemService;
    }

    /**
     * 新增待办事项。
     *
     * @param request 创建请求
     * @return 响应
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateTodoItemRequest request) {
        // 从上下文获取 tenant_id（当前硬编码，后续接入认证拦截器后替换）
        String tenantId = resolveTenantId();

        try {
            TodoItemDO todoItem = todoItemService.createTodoItem(tenantId, request);
            TodoItemVO vo = toVO(todoItem);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", "OK");
            result.put("msg", "SUCCESS");
            result.put("data", vo);
            return ResponseEntity.ok(result);
        } catch (BusinessException e) {
            logger.warn("创建待办事项失败, errorCode: {}, message: {}", e.getErrorCode(), e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", e.getErrorCode());
            result.put("msg", e.getMessage());
            result.put("data", null);
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 解析当前租户标识。TODO: 接入认证拦截器后从上下文获取。
     */
    private String resolveTenantId() {
        return "default";
    }

    private TodoItemVO toVO(TodoItemDO todoItem) {
        TodoItemVO vo = new TodoItemVO();
        vo.setId(todoItem.getId());
        vo.setName(todoItem.getName());
        vo.setDescription(todoItem.getDescription());
        if (todoItem.getGmtCreate() != null) {
            vo.setGmtCreate(todoItem.getGmtCreate().format(DATE_TIME_FORMATTER));
        }
        return vo;
    }
}