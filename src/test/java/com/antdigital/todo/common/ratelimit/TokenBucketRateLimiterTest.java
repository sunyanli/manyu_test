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

    @Test
    @DisplayName("空闲桶按周期淘汰：长期未访问的 key 不再占用内存")
    void should_evictIdleBuckets_when_evictIntervalReached() {
        // Arrange：扫描间隔为 0、空闲阈值为负数，
        // 保证下一次请求必然触发淘汰，避免依赖时钟精度
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(0L, -1L);
        rateLimiter.tryAcquire("U1001", 1, QPS);
        rateLimiter.tryAcquire("U1002", 1, QPS);

        // Act
        rateLimiter.tryAcquire("U1003", 1, QPS);

        // Assert：历史空闲桶被清理，仅保留本次访问的 key
        assertThat(rateLimiter.bucketCount()).isEqualTo(1);
    }
}
