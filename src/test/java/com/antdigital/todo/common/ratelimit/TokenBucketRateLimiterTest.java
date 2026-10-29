package com.antdigital.todo.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TokenBucketRateLimiter} 单元测试。
 *
 * @author AiWork
 */
class TokenBucketRateLimiterTest {

    private static final int QPS = 10;

    @Test
    @DisplayName("同一 key 超过 1 秒令牌量后被拒绝")
    void should_reject_when_tokensExhausted() {
        // Arrange
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        // Act & Assert
        for (int i = 0; i < QPS; i++) {
            assertThat(rateLimiter.tryAcquire("U1001", 1, QPS)).as("第 %s 次请求应被放行", i + 1).isTrue();
        }
        assertThat(rateLimiter.tryAcquire("U1001", 1, QPS)).isFalse();
    }

    @Test
    @DisplayName("不同 key 之间互不影响")
    void should_isolateBuckets_when_keyDiffers() {
        // Arrange
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        for (int i = 0; i < QPS; i++) {
            rateLimiter.tryAcquire("U1001", 1, QPS);
        }

        // Act
        boolean acquired = rateLimiter.tryAcquire("U1002", 1, QPS);

        // Assert
        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("qps 非正数时不做限制")
    void should_passAll_when_qpsNotPositive() {
        // Arrange
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();

        // Act & Assert
        assertThat(rateLimiter.tryAcquire("U1001", 1, 0)).isTrue();
    }
}
