package com.antdigital.todo.controller;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.model.ApiResult;
import com.antdigital.todo.integration.impl.GatewayAuthIntegrationImpl;
import com.antdigital.todo.integration.support.GatewaySignatureVerifier;
import com.antdigital.todo.integration.support.GatewaySignatureVerifierTestSupport;
import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;
import com.antdigital.todo.model.vo.TodoCreateVO;
import com.antdigital.todo.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoController} 协议适配单元测试：覆盖身份解析、网关签名校验、回执映射与响应结构。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoControllerTest {

    private static final String TODO_NO = "TD20261029000042";

    private static final String SECRET = "test-gateway-secret";

    private static final long FIXED_NOW = 1_793_266_272_000L;

    private static final String USER_ID = "U10086";

    private static final String USER_NAME = "张三";

    private static final String TENANT_ID = "0";

    private static final String PERMISSIONS = "TODO_CREATE";

    private final FakeTodoService todoService = new FakeTodoService();

    private final TodoController todoController = new TodoController(todoService,
            new GatewayAuthIntegrationImpl(buildVerifier(true, SECRET)));

    @Test
    @DisplayName("创建成功：返回统一响应体，主键以字符串输出")
    void should_returnSuccessResult_when_validRequest() {
        // Arrange
        Map<String, String> headers = buildSignedHeaders(USER_ID, USER_NAME, TENANT_ID, PERMISSIONS);
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
        assertThat(todoService.lastOperator.getUserId()).isEqualTo(USER_ID);
        assertThat(todoService.lastOperator.getRoles()).containsExactly("TODO_CREATE");
    }

    @Test
    @DisplayName("身份缺失：未注入用户身份头时返回 TODO_0005")
    void should_throwNotLogin_when_userHeaderMissing() {
        // Arrange
        Map<String, String> headers = new HashMap<>();

        // Act & Assert
        assertThatThrownBy(() -> todoController.create(buildRequest(), headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("身份缺失：租户头缺省时返回 TODO_0005，不再回落为默认租户")
    void should_throwNotLogin_when_tenantHeaderMissing() {
        // Arrange
        Map<String, String> headers = buildSignedHeaders(USER_ID, USER_NAME, "", PERMISSIONS);

        // Act & Assert
        assertThatThrownBy(() -> todoController.create(buildRequest(), headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("签名缺失：客户端伪造身份头无网关签名时返回 TODO_0005")
    void should_throwNotLogin_when_signatureMissing() {
        // Arrange
        Map<String, String> headers = new HashMap<>();
        headers.put("X-User-Id", USER_ID);
        headers.put("X-Tenant-Id", TENANT_ID);
        headers.put("X-User-Permissions", PERMISSIONS);

        // Act & Assert
        assertThatThrownBy(() -> todoController.create(buildRequest(), headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("签名被篡改：签名与载荷不一致时返回 TODO_0005")
    void should_throwNotLogin_when_signatureMismatch() {
        // Arrange
        Map<String, String> headers = buildSignedHeaders(USER_ID, USER_NAME, TENANT_ID, PERMISSIONS);
        headers.put("X-User-Permissions", "TODO_CREATE,*");

        // Act & Assert
        assertThatThrownBy(() -> todoController.create(buildRequest(), headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("签名密钥未配置：fail-closed 返回 TODO_0005")
    void should_throwNotLogin_when_secretNotConfigured() {
        // Arrange
        TodoController controller = new TodoController(todoService,
                new GatewayAuthIntegrationImpl(buildVerifier(true, "")));
        Map<String, String> headers = buildSignedHeaders(USER_ID, USER_NAME, TENANT_ID, PERMISSIONS);

        // Act & Assert
        assertThatThrownBy(() -> controller.create(buildRequest(), headers))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("用户展示名缺省时回落为用户 ID")
    void should_useUserIdAsName_when_userNameMissing() {
        // Arrange
        Map<String, String> headers = buildSignedHeaders(USER_ID, "", TENANT_ID, PERMISSIONS);

        // Act
        todoController.create(buildRequest(), headers);

        // Assert
        assertThat(todoService.lastOperator.getUserName()).isEqualTo(USER_ID);
    }

    private GatewaySignatureVerifier buildVerifier(boolean enabled, String secret) {
        return GatewaySignatureVerifierTestSupport.of(enabled, secret, FIXED_NOW);
    }

    private Map<String, String> buildSignedHeaders(String userId, String userName, String tenantId,
                                                   String permissions) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-User-Id", userId);
        headers.put("X-User-Name", userName);
        headers.put("X-Tenant-Id", tenantId);
        headers.put("X-User-Permissions", permissions);
        String timestamp = String.valueOf(FIXED_NOW);
        headers.put("X-Gateway-Timestamp", timestamp);
        headers.put("X-Gateway-Sign", sign(timestamp + "\n" + userId + "\n" + userName + "\n" + tenantId
                + "\n" + permissions));
        return headers;
    }

    private String sign(String plaintext) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value & 0xFF));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("sign failed", e);
        }
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
