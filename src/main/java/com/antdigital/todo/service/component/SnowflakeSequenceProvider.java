package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import org.springframework.stereotype.Component;

/**
 * 基于雪花算法的本地序号提供者（无外部依赖，多实例部署需注入不同 workerId）。
 *
 * <p>作为取号降级实现：Redis 不可用或未接入时直接使用，进程内线程安全。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class SnowflakeSequenceProvider implements TodoSequenceProvider {

    /**
     * 工作节点 ID 位移量
     */
    private static final int WORKER_ID_SHIFT =
            TodoConstants.SNOWFLAKE_SEQUENCE_BITS + TodoConstants.SNOWFLAKE_DATACENTER_ID_BITS;

    /**
     * 数据中心 ID 位移量
     */
    private static final int DATACENTER_ID_SHIFT = TodoConstants.SNOWFLAKE_SEQUENCE_BITS;

    /**
     * 序列号掩码
     */
    private static final long SEQUENCE_MASK = -1L ^ (-1L << TodoConstants.SNOWFLAKE_SEQUENCE_BITS);

    /**
     * 工作节点 ID
     */
    private final long workerId;

    /**
     * 数据中心 ID
     */
    private final long datacenterId;

    /**
     * 上一次生成 ID 的毫秒时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 当前毫秒内的序列号
     */
    private long sequence = 0L;

    /**
     * 使用默认节点参数构造。
     */
    public SnowflakeSequenceProvider() {
        this(0L, 0L);
    }

    /**
     * 指定节点参数构造。
     *
     * @param workerId     工作节点 ID，取值范围 [0, 32)
     * @param datacenterId 数据中心 ID，取值范围 [0, 32)
     */
    public SnowflakeSequenceProvider(long workerId, long datacenterId) {
        if (workerId < 0 || workerId >= TodoConstants.SNOWFLAKE_WORKER_ID_MAX) {
            throw new IllegalArgumentException("workerId out of range: " + workerId);
        }
        if (datacenterId < 0 || datacenterId >= TodoConstants.SNOWFLAKE_DATACENTER_ID_MAX) {
            throw new IllegalArgumentException("datacenterId out of range: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个雪花 ID；日期键仅用于接口语义对齐，雪花 ID 全局递增。
     *
     * @param dateKey 日期键，格式 yyyyMMdd，本实现不参与计算
     * @return 雪花 ID（正整数）
     */
    @Override
    public synchronized long nextSequence(String dateKey) {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨：等待至上一毫秒，避免生成重复 ID
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - TodoConstants.SNOWFLAKE_EPOCH_MILLIS) << WORKER_ID_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << TodoConstants.SNOWFLAKE_SEQUENCE_BITS)
                | sequence;
    }

    /**
     * 自旋等待至目标毫秒。
     *
     * @param targetTimestamp 目标毫秒时间戳
     * @return 实际到达的时间戳
     */
    private long waitUntil(long targetTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetTimestamp) {
            Thread.yield();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
