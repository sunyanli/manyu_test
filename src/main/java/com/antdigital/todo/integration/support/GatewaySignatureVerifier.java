package com.antdigital.todo.integration.support;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.util.TodoTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 网关请求签名校验器（S8.1）：身份与权限头必须由网关用共享密钥签名，客户端无法伪造。
 *
 * <p>签名算法：{@code HMAC-SHA256(secret, timestamp + "\n" + canonicalPayload)}，十六进制小写；
 * 校验过程同时检查时间戳偏差，超出容差视为重放请求。校验失败统一抛 TODO_0005（fail-closed），
 * 密钥未配置时同样拒绝，避免「默认信任客户端头」的安全红线。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class GatewaySignatureVerifier {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewaySignatureVerifier.class);

    /**
     * 签名算法
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 十六进制字符表
     */
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /**
     * 签名载荷分隔符
     */
    private static final String PAYLOAD_SEPARATOR = "\n";

    /**
     * 是否开启签名校验
     */
    private final boolean verifyEnabled;

    /**
     * 网关共享密钥（由配置中心注入）
     */
    private final String signSecret;

    /**
     * 时间戳允许偏差（毫秒）
     */
    private final long toleranceMillis;

    /**
     * 当前毫秒时钟
     */
    private final LongSupplier clock;

    /**
     * 生产构造：签名校验默认开启，密钥由配置中心注入。
     *
     * @param verifyEnabled     是否开启签名校验
     * @param signSecret        网关共享密钥
     * @param toleranceSeconds  时间戳允许偏差（秒）
     */
    public GatewaySignatureVerifier(@Value("${auth.gateway.verify.enabled:true}") boolean verifyEnabled,
                                    @Value("${auth.gateway.sign-secret:}") String signSecret,
                                    @Value("${auth.gateway.timestamp-tolerance-seconds:300}") long toleranceSeconds) {
        this(verifyEnabled, signSecret, toleranceSeconds, System::currentTimeMillis);
    }

    /**
     * 全参构造，供单测注入固定时钟。
     *
     * @param verifyEnabled    是否开启签名校验
     * @param signSecret       网关共享密钥
     * @param toleranceSeconds 时间戳允许偏差（秒）
     * @param clock            当前毫秒时钟
     */
    GatewaySignatureVerifier(boolean verifyEnabled, String signSecret, long toleranceSeconds, LongSupplier clock) {
        this.verifyEnabled = verifyEnabled;
        this.signSecret = signSecret;
        this.toleranceMillis = TimeUnit.SECONDS.toMillis(toleranceSeconds);
        this.clock = clock;
    }

    /**
     * 校验网关注入的身份与权限载荷签名。
     *
     * @param headers          请求头
     * @param canonicalPayload 规范化的身份与权限载荷
     * @throws TodoBizException 签名缺失、密钥未配置、时间戳越界或签名不匹配时抛出 TODO_0005
     */
    public void verify(Map<String, String> headers, String canonicalPayload) {
        if (!verifyEnabled) {
            return;
        }
        String secret = TodoTextUtil.trimWhitespace(signSecret);
        if (secret.isEmpty()) {
            LOGGER.error("gateway sign secret is not configured, reject request to avoid trusting client headers");
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        String signature = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_GATEWAY_SIGN));
        String timestampText = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_GATEWAY_TIMESTAMP));
        if (signature.isEmpty() || timestampText.isEmpty()) {
            LOGGER.warn("gateway signature header missing, signPresent={}, timestampPresent={}",
                    !signature.isEmpty(), !timestampText.isEmpty());
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        long timestamp = parseTimestamp(timestampText);
        long now = clock.getAsLong();
        if (Math.abs(now - timestamp) > toleranceMillis) {
            LOGGER.warn("gateway signature timestamp out of tolerance, deviation={}ms", now - timestamp);
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
        String expected = sign(secret, timestampText + PAYLOAD_SEPARATOR + canonicalPayload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            LOGGER.warn("gateway signature mismatch, reject request");
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
    }

    /**
     * 解析时间戳头，非法格式按未登录处理。
     *
     * @param timestampText 时间戳文本
     * @return 毫秒时间戳
     */
    private long parseTimestamp(String timestampText) {
        try {
            return Long.parseLong(timestampText);
        } catch (NumberFormatException e) {
            LOGGER.warn("gateway timestamp header is not a number, value={}", timestampText);
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
    }

    /**
     * 计算 HMAC-SHA256 十六进制小写签名。
     *
     * @param secret    共享密钥
     * @param plaintext 待签名明文
     * @return 十六进制小写签名
     */
    private String sign(String secret, String plaintext) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return toHex(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            LOGGER.error("gateway signature calculate failed, degrade to reject request", e);
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
    }

    /**
     * 字节数组转十六进制小写字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xFF;
            chars[index * 2] = HEX_DIGITS[value >>> 4];
            chars[index * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return new String(chars);
    }

}
