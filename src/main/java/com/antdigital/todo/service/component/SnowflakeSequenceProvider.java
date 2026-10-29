package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 基于雪花算法的本地序号提供者（取号降级实现，无外部依赖）。
 *
 * <p>节点参数 {@code workerId}/{@code datacenterId} 必须由配置或环境变量注入，且同一集群内
 * 每个实例的组合唯一（系分 §5.1 支撑层「保证多实例下不重复」）；未注入时保持 0/0 并输出
 * WARN，多副本高并发下可能生成相同序列，仅用于本地开发。时钟回拨在阈值内短休眠等待，
 * 超出阈值快速失败，避免长时间自旋占用 CPU。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class SnowflakeSequenceProvider implements TodoSequenceProvider {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeSequenceProvider.class);

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
     * 指定节点参数构造：生产环境由配置中心注入唯一组合。
     *
     * @param workerId     工作节点 ID，取值范围 [0, 32)
     * @param datacenterId 数据中心 ID，取值范围 [0, 32)
     */
    public SnowflakeSequenceProvider(@Value("${todo.no.worker-id:0}") long workerId,
                                     @Value("${todo.no.datacenter-id:0}") long datacenterId) {
        if (workerId < 0 || workerId >= TodoConstants.SNOWFLAKE_WORKER_ID_MAX) {
            throw new IllegalArgumentException("workerId out of range: " + workerId);
        }
        if (datacenterId < 0 || datacenterId >= TodoConstants.SNOWFLAKE_DATACENTER_ID_MAX) {
            throw new IllegalArgumentException("datacenterId out of range: " + datacenterId);
        }
        if (workerId == 0L && datacenterId == 0L) {
            LOGGER.warn("snowflake node id not configured (workerId=0, datacenterId=0), "
                    + "multi-instance deployment may produce duplicate todo_no");
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
            long backward = lastTimestamp - timestamp;
            if (backward > TodoConstants.SNOWFLAKE_MAX_BACKWARD_WAIT_MILLIS) {
                LOGGER.error("clock moved backwards beyond tolerance, backward={}ms", backward);
                throw new IllegalStateException("clock moved backwards: " + backward + "ms");
            }
            LOGGER.warn("clock moved backwards within tolerance, wait. backward={}ms", backward);
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
     * 本实现为取号降级路径，需上报 {@code todo_no_degrade_total}。
     *
     * @return 恒为 true
     */
    @Override
    public boolean isDegraded() {
        return true;
    }

    /**
     * 短休眠等待至目标毫秒，替代纯自旋忙等。
     *
     * @param targetTimestamp 目标毫秒时间戳
     * @return 实际到达的时间戳
     */
    private long waitUntil(long targetTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetTimestamp) {
            long sleepMillis = Math.min(targetTimestamp - timestamp,
                    TodoConstants.SNOWFLAKE_MAX_WAIT_SLEEP_MILLIS);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting snowflake clock", e);
            }
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
