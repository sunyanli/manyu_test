package com.antdigital.todo.controller;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.model.ApiResult;
import com.antdigital.todo.integration.AuthIntegration;
import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;
import com.antdigital.todo.model.vo.TodoCreateVO;
import com.antdigital.todo.service.TodoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 待办 Web 控制台接口（oneapi 层）。
 *
 * <p>仅做协议适配：解析网关注入的身份头、委托业务层、组装统一响应体；
 * 业务校验、幂等、限流均在 Service 层收口，便于后续内部批量场景复用。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@RestController
@RequestMapping("/api/todo/item")
public class TodoController {

    /**
     * 响应时间格式化器
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TodoConstants.DATE_TIME_PATTERN);

    /**
     * 待办业务服务
     */
    private final TodoService todoService;

    /**
     * 登录态与身份解析
     */
    private final AuthIntegration authIntegration;

    /**
     * 构造注入依赖。
     *
     * @param todoService     待办业务服务
     * @param authIntegration 登录态与身份解析
     */
    public TodoController(TodoService todoService, AuthIntegration authIntegration) {
        this.todoService = todoService;
        this.authIntegration = authIntegration;
    }

    /**
     * 新增待办事项（W01）。
     *
     * @param request 创建入参
     * @param headers 请求头（含网关注入的身份信息）
     * @return 统一响应体，data 为创建回执
     */
    @PostMapping("/create")
    public ApiResult<TodoCreateVO> create(@Valid @RequestBody TodoCreateRequest request,
                                          @RequestHeader Map<String, String> headers) {
        OperatorContext operator = authIntegration.resolveOperator(headers);
        TodoCreateResult result = todoService.createTodo(request, operator);
        return ApiResult.success(toVO(result));
    }

    /**
     * 业务结果转视图对象。
     *
     * @param result 创建结果
     * @return 创建回执
     */
    private TodoCreateVO toVO(TodoCreateResult result) {
        TodoCreateVO vo = new TodoCreateVO();
        // 主键以 String 返回，避免前端 Number 精度丢失
        vo.setId(result.getId() == null ? null : String.valueOf(result.getId()));
        vo.setTodoNo(result.getTodoNo());
        vo.setItemName(result.getItemName());
        vo.setItemDesc(result.getItemDesc());
        vo.setStatus(result.getStatus());
        vo.setCreatorId(result.getCreatorId());
        vo.setGmtCreate(result.getGmtCreate() == null ? null : DATE_TIME_FORMATTER.format(result.getGmtCreate()));
        vo.setIdempotent(result.isIdempotent());
        return vo;
    }
}
