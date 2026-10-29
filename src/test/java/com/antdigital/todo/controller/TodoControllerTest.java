package com.antdigital.todo.controller;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.model.ApiResult;
import com.antdigital.todo.integration.impl.GatewayAuthIntegrationImpl;
import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;
import com.antdigital.todo.model.vo.TodoCreateVO;
import com.antdigital.todo.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoController} 协议适配单元测试：覆盖身份解析、回执映射与响应结构。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoControllerTest {

    private static final String TODO_NO = "TD20261029000042";

    private final FakeTodoService todoService = new FakeTodoService();

    private final TodoController todoController =
            new TodoController(todoService, new GatewayAuthIntegrationImpl(false));

    @Test
    @DisplayName("创建成功：返回统一响应体，主键以字符串输出")
    void should_returnSuccessResult_when_validRequest() {
        // Arrange
        Map<String, String> headers = buildHeaders("U10086", "张三", "0", "TODO_CREATE");
        TodoCreateRequest request = buildRequest();

        // Act
        ApiResult<TodoCreateVO> response = todoController.create(request, headers);

        // Assert
        assertThat(response.getCode()).isEqualTo("OK");
        assertThat(response.getMsg()).isEqualTo("SUCCESS");
        TodoCreateVO data = response.getData();
        assertThat(data).isNotNull();
        assertThat(data.getId()).isEqualTo("100231");
        assertThat(data.getTodoNo()).isEqualTo(TODO_NO);
        assertThat(data.getStatus()).isZero();
        assertThat(data.getGmtCreate()).isEqualTo("2026-10-29 10:12:33");
        assertThat(data.isIdempotent()).isFalse();
        assertThat(todoService.lastOperator.getUserId()).isEqualTo("U10086");
        assertThat(todoService.lastOperator.getRoles()).containsExactly("TODO_CREATE");
    }

    @Test
    @DisplayName("身份缺失：未注入用户身份头时返回 TODO_0005")
    void should_throwNotLogin_when_userHeaderMissing() {
        // Arrange
        Map<String, String> headers = buildHeaders(null, null, "0", null);
        TodoCreateRequest request = buildRequest();

        // Act & Assert
        assertThatThrownBy(() -> todoController.create(request, headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("身份缺失：租户头缺省时回落为 0")
    void should_useDefaultTenant_when_tenantHeaderMissing() {
        // Arrange
        Map<String, String> headers = buildHeaders("U10086", null, null, "TODO_CREATE");

        // Act
        todoController.create(buildRequest(), headers);

        // Assert
        assertThat(todoService.lastOperator.getTenantId()).isEqualTo("0");
        assertThat(todoService.lastOperator.getUserName()).isEqualTo("U10086");
    }

    private Map<String, String> buildHeaders(String userId, String userName, String tenantId, String roles) {
        Map<String, String> headers = new HashMap<>();
        if (userId != null) {
            headers.put("X-User-Id", userId);
        }
        if (userName != null) {
            headers.put("X-User-Name", userName);
        }
        if (tenantId != null) {
            headers.put("X-Tenant-Id", tenantId);
        }
        if (roles != null) {
            headers.put("X-User-Roles", roles);
        }
        return headers;
    }

    private TodoCreateRequest buildRequest() {
        TodoCreateRequest request = new TodoCreateRequest();
        request.setRequestId("8f2b1c34-5d6e-4a7b-9c8d-0e1f2a3b4c5d");
        request.setItemName("整理季度经营分析材料");
        request.setItemDesc("汇总 Q3 数据");
        return request;
    }

    /**
     * 待办服务替身：返回固定回执，并记录入参操作人。
     */
    private static final class FakeTodoService implements TodoService {

        private OperatorContext lastOperator;

        @Override
        public TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator) {
            this.lastOperator = operator;
            TodoCreateResult result = new TodoCreateResult();
            result.setId(100231L);
            result.setTodoNo(TODO_NO);
            result.setItemName(request.getItemName());
            result.setItemDesc(request.getItemDesc());
            result.setStatus(0);
            result.setCreatorId(operator.getUserId());
            result.setGmtCreate(LocalDateTime.of(2026, 10, 29, 10, 12, 33));
            result.setIdempotent(false);
            return result;
        }

        @Override
        public TodoCreateResult queryByRequestId(String requestId, OperatorContext operator) {
            return null;
        }
    }
}
