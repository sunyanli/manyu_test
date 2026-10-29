package com.antdigital.todo.integration.support;

import java.util.function.LongSupplier;

/**
 * {@link GatewaySignatureVerifier} 测试构造辅助：固定时钟，便于签名用例稳定复现。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public final class GatewaySignatureVerifierTestSupport {

    private GatewaySignatureVerifierTestSupport() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 构造固定时钟的签名校验器。
     *
     * @param enabled 是否开启校验
     * @param secret  共享密钥
     * @param now     固定当前毫秒时间戳
     * @return 签名校验器
     */
    public static GatewaySignatureVerifier of(boolean enabled, String secret, long now) {
        LongSupplier clock = () -> now;
        return new GatewaySignatureVerifier(enabled, secret, 300L, clock);
    }

    /**
     * 构造固定时钟的签名校验器，并指定时间戳容差。
     *
     * @param enabled          是否开启校验
     * @param secret           共享密钥
     * @param toleranceSeconds 时间戳允许偏差（秒）
     * @param now              固定当前毫秒时间戳
     * @return 签名校验器
     */
    public static GatewaySignatureVerifier of(boolean enabled, String secret, long toleranceSeconds, long now) {
        LongSupplier clock = () -> now;
        return new GatewaySignatureVerifier(enabled, secret, toleranceSeconds, clock);
    }
}
