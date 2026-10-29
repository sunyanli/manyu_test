package com.antdigital.todo.common.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内令牌桶限流器，按 key 维度维护，用于保护单实例容量。
 *
 * <p>多副本部署时为近似限流，精度要求低的用户级限流场景可接受，且不引入 Redis 依赖。</p>
 *
 * @author AiWork
 */
public class TokenBucketRateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket>();

    /**
     * 尝试获取一个令牌。
     *
     * @param key      限流维度，如用户 ID
     * @param permits  本次所需令牌数
     * @param qps      每秒补充令牌数
     * @return 获取成功返回 true
     */
    public boolean tryAcquire(String key, int permits, int qps) {
        if (qps <= 0) {
            return true;
        }
        // 单桶容量取 1 秒的令牌量，允许不超过 1 秒的突发
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(qps));
        return bucket.tryAcquire(permits, qps);
    }

    /**
     * 令牌桶实现。
     */
    private static final class Bucket {

        private final int capacity;

        private double tokens;

        private long lastRefillNanos;

        Bucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryAcquire(int permits, int qps) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / (double) NANOS_PER_SECOND;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * qps);
                lastRefillNanos = now;
            }
            if (tokens < permits) {
                return false;
            }
            tokens -= permits;
            return true;
        }
    }

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
}
