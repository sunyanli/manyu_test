package com.antdigital.todo.common.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内令牌桶限流器，按 key 维度维护，用于保护单实例容量。
 *
 * <p>多副本部署时为近似限流，精度要求低的用户级限流场景可接受，
 * 且不引入 Redis 依赖。空闲桶按固定周期淘汰，避免历史用户累积导致 Map 无界增长。</p>
 *
 * @author AiWork
 */
public class TokenBucketRateLimiter {

    /** 空闲桶存活时间（纳秒），超过该时长未访问即被淘汰。 */
    private static final long IDLE_EVICT_NANOS = 300L * 1_000_000_000L;

    /** 两次淘汰扫描的最小间隔（纳秒）。 */
    private static final long EVICT_INTERVAL_NANOS = 60L * 1_000_000_000L;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket>();

    private final long evictIntervalNanos;

    private final long idleEvictNanos;

    private volatile long lastEvictNanos = System.nanoTime();

    public TokenBucketRateLimiter() {
        this(EVICT_INTERVAL_NANOS, IDLE_EVICT_NANOS);
    }

    /**
     * 供测试定制的构造器。
     *
     * @param evictIntervalNanos 两次淘汰扫描的最小间隔（纳秒）
     * @param idleEvictNanos     空闲桶存活时间（纳秒）
     */
    TokenBucketRateLimiter(long evictIntervalNanos, long idleEvictNanos) {
        this.evictIntervalNanos = evictIntervalNanos;
        this.idleEvictNanos = idleEvictNanos;
    }

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
        evictIdleBuckets();
        // 单桶容量取 1 秒的令牌量，允许不超过 1 秒的突发
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(qps));
        return bucket.tryAcquire(permits, qps);
    }

    /**
     * 当前维护的令牌桶数量，供单测与排障使用。
     *
     * @return 桶数量
     */
    int bucketCount() {
        return buckets.size();
    }

    /**
     * 淘汰长期未访问的桶；扫描间隔受限，避免高频请求下反复全量遍历。
     */
    private void evictIdleBuckets() {
        long now = System.nanoTime();
        if (now - lastEvictNanos < evictIntervalNanos) {
            return;
        }
        lastEvictNanos = now;
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessNanos() > idleEvictNanos);
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

        long lastAccessNanos() {
            return lastRefillNanos;
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
