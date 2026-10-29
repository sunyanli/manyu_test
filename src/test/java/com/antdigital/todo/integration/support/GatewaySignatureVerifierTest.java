package com.antdigital.todo.integration.support;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link GatewaySignatureVerifier} 单元测试：覆盖签名通过、缺失、篡改、时钟越界与密钥缺失。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class GatewaySignatureVerifierTest {

    private static final String SECRET = "test-gateway-secret";

    private static final String PAYLOAD = "U1\n张三\n0\nTODO_CREATE";

    private static final long NOW = 1_793_266_272_000L;

    @Test
    @DisplayName("签名正确且在时间戳容差内时校验通过")
    void should_pass_when_signatureValid() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, SECRET, NOW);
        Map<String, String> headers = buildHeaders(String.valueOf(NOW), sign(String.valueOf(NOW) + "\n" + PAYLOAD));

        // Act & Assert
        assertThatCode(() -> verifier.verify(headers, PAYLOAD)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("签名缺失时返回 TODO_0005")
    void should_throwNotLogin_when_signatureMissing() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, SECRET, NOW);
        Map<String, String> headers = new HashMap<>();
        headers.put(TodoConstants.HEADER_GATEWAY_TIMESTAMP, String.valueOf(NOW));

        // Act & Assert
        assertThatThrownBy(() -> verifier.verify(headers, PAYLOAD))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getCode())
                                .isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("载荷被篡改时签名不匹配，返回 TODO_0005")
    void should_throwNotLogin_when_payloadTampered() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, SECRET, NOW);
        Map<String, String> headers = buildHeaders(String.valueOf(NOW), sign(String.valueOf(NOW) + "\n" + PAYLOAD));

        // Act & Assert
        assertThatThrownBy(() -> verifier.verify(headers, PAYLOAD + "\n*"))
                .isInstanceOf(TodoBizException.class);
    }

    @Test
    @DisplayName("时间戳超出容差视为重放，返回 TODO_0005")
    void should_throwNotLogin_when_timestampExpired() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, SECRET, NOW);
        long expired = NOW - 3_600_000L;
        Map<String, String> headers = buildHeaders(String.valueOf(expired),
                sign(String.valueOf(expired) + "\n" + PAYLOAD));

        // Act & Assert
        assertThatThrownBy(() -> verifier.verify(headers, PAYLOAD))
                .isInstanceOf(TodoBizException.class);
    }

    @Test
    @DisplayName("密钥未配置时 fail-closed，返回 TODO_0005")
    void should_throwNotLogin_when_secretBlank() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, " ", NOW);
        Map<String, String> headers = buildHeaders(String.valueOf(NOW), sign(String.valueOf(NOW) + "\n" + PAYLOAD));

        // Act & Assert
        assertThatThrownBy(() -> verifier.verify(headers, PAYLOAD))
                .isInstanceOf(TodoBizException.class);
    }

    @Test
    @DisplayName("时间戳非数字时返回 TODO_0005")
    void should_throwNotLogin_when_timestampNotNumeric() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(true, SECRET, NOW);
        Map<String, String> headers = buildHeaders("not-a-number", "deadbeef");

        // Act & Assert
        assertThatThrownBy(() -> verifier.verify(headers, PAYLOAD))
                .isInstanceOf(TodoBizException.class);
    }

    @Test
    @DisplayName("校验开关关闭时不校验签名")
    void should_skipVerify_when_disabled() {
        // Arrange
        GatewaySignatureVerifier verifier = GatewaySignatureVerifierTestSupport.of(false, SECRET, NOW);

        // Act & Assert
        assertThatCode(() -> verifier.verify(new HashMap<>(), PAYLOAD)).doesNotThrowAnyException();
    }

    private Map<String, String> buildHeaders(String timestamp, String signature) {
        Map<String, String> headers = new HashMap<>();
        headers.put(TodoConstants.HEADER_GATEWAY_TIMESTAMP, timestamp);
        headers.put(TodoConstants.HEADER_GATEWAY_SIGN, signature);
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
}
