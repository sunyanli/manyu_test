package com.antdigital.todo.common.interceptor;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.config.TodoCreateProperties;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.security.TrustedGatewayProperties;
import com.antdigital.todo.item.constant.TodoConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LoginInterceptor} 单元测试。
 *
 * @author AiWork
 */
class LoginInterceptorTest {

    private TodoCreateProperties createProperties;

    private TrustedGatewayProperties gatewayProperties;

    private LoginInterceptor interceptor;

    @BeforeEach
    void setUp() {
        createProperties = new TodoCreateProperties();
        gatewayProperties = new TrustedGatewayProperties();
        interceptor = new LoginInterceptor(gatewayProperties, createProperties);
    }

    @AfterEach
    void tearDown() {
        OperatorContextHolder.clear();
    }

    @Test
    @DisplayName("正常路径：注入操作人上下文后放行")
    void should_passAndInjectContext_when_headersValid() {
        // Arrange
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");

        // Act
        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // Assert
        assertThat(passed).isTrue();
        OperatorContext operator = OperatorContextHolder.get();
        assertThat(operator).isNotNull();
        assertThat(operator.getUserId()).isEqualTo("U1001");
        assertThat(operator.getUserName()).isEqualTo("张三");
        assertThat(operator.getTenantId()).isEqualTo("T01");
    }

    @Test
    @DisplayName("展示名缺失：以用户 ID 兜底")
    void should_useUserId_when_userNameHeaderMissing() {
        // Arrange
        MockHttpServletRequest request = buildRequest("U1001", null, "T01");

        // Act
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // Assert
        assertThat(OperatorContextHolder.get().getUserName()).isEqualTo("U1001");
    }

    @Test
    @DisplayName("登录 Header 缺失：抛 TODO_002")
    void should_throwTodo002_when_userIdHeaderMissing() {
        // Arrange
        MockHttpServletRequest request = buildRequest(" ", "张三", "T01");

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_002);
    }

    @Test
    @DisplayName("租户参数与登录态不一致：抛 TODO_003")
    void should_throwTodo003_when_tenantParamMismatch() {
        // Arrange
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");
        request.setParameter("tenantId", "T99");

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_003);
    }

    @Test
    @DisplayName("来源 IP 不在白名单：抛 TODO_002")
    void should_throwTodo002_when_sourceIpNotTrusted() {
        // Arrange
        gatewayProperties.setTrustedGatewayIps(Collections.singletonList("10.0.0.1"));
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");
        request.setRemoteAddr("192.168.1.9");

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_002);
    }

    @Test
    @DisplayName("总开关关闭：抛 TODO_007")
    void should_throwTodo007_when_switchOff() {
        // Arrange
        createProperties.setEnabled(false);
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_007);
    }

    @Test
    @DisplayName("灰度未命中：抛 TODO_006")
    void should_throwTodo006_when_tenantSuffixNotInGrayList() {
        // Arrange
        createProperties.setGrayEnabled(true);
        createProperties.setGrayTenantSuffix(Arrays.asList("0", "1"));
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T07");

        // Act & Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_006);
    }

    @Test
    @DisplayName("灰度命中：放行")
    void should_pass_when_tenantSuffixInGrayList() {
        // Arrange
        createProperties.setGrayEnabled(true);
        createProperties.setGrayTenantSuffix(Arrays.asList("0", "1"));
        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");

        // Act
        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // Assert
        assertThat(passed).isTrue();
    }

    private MockHttpServletRequest buildRequest(String userId, String userName, String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/todo/item/create");
        if (userId != null) {
            request.addHeader(TodoConstants.HEADER_USER_ID, userId);
        }
        if (userName != null) {
            request.addHeader(TodoConstants.HEADER_USER_NAME, userName);
        }
        if (tenantId != null) {
            request.addHeader(TodoConstants.HEADER_TENANT_ID, tenantId);
        }
        return request;
    }
}
