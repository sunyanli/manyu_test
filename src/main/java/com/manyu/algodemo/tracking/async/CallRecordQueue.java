package com.manyu.algodemo.tracking.async;

import com.manyu.algodemo.tracking.dao.TrackingMapper;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 埋点异步批量写入队列（F06）。
 *
 * <p>非阻塞入队 + 定时批量 flush；写库失败静默降级为日志与计数，不影响主流程。</p>
 */
@Component
public class CallRecordQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallRecordQueue.class);

    private final TrackingMapper trackingMapper;
    private final boolean enabled;
    private final int batchSize;
    private final LinkedBlockingQueue<CallRecordDO> queue;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong writeFailCount = new AtomicLong(0);

    /**
     * 构造器注入。
     *
     * @param trackingMapper  埋点 Mapper
     * @param enabled         埋点总开关
     * @param queueCapacity   队列容量
     * @param batchSize       批量写条数
     * @param flushIntervalMs 定时冲刷间隔
     */
    public CallRecordQueue(
            TrackingMapper trackingMapper,
            @Value("${tracking.enabled:true}") boolean enabled,
            @Value("${tracking.queue-capacity:10000}") int queueCapacity,
            @Value("${tracking.batch-size:500}") int batchSize,
            @Value("${tracking.flush-interval-ms:200}") long flushIntervalMs) {
        this.trackingMapper = trackingMapper;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "call-record-flusher");
            t.setDaemon(true);
            return t;
        });
        if (enabled) {
            scheduler.scheduleWithFixedDelay(this::flush, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 非阻塞入队；队列满时静默丢弃并计数。
     *
     * @param record 调用记录
     */
    public void offer(CallRecordDO record) {
        if (!enabled) {
            return;
        }
        if (!queue.offer(record)) {
            LOGGER.warn("埋点队列已满，丢弃记录: bizType={}, callerId={}", record.getBizType(), record.getCallerId());
            droppedCount.incrementAndGet();
        }
    }

    private void flush() {
        List<CallRecordDO> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) {
            return;
        }
        try {
            trackingMapper.batchInsert(batch);
        } catch (Exception e) {
            LOGGER.error("埋点批量写失败，批次大小={}", batch.size(), e);
            writeFailCount.incrementAndGet();
        }
    }

    /**
     * 应用关闭时冲刷剩余记录并释放资源。
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        flush();
        LOGGER.info("埋点队列关闭，累计丢弃={}, 写失败批次数={}", droppedCount.get(), writeFailCount.get());
    }
}
