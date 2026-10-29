package com.antdigital.todo.integration.impl;

import com.antdigital.todo.integration.AuditIntegration;
import com.antdigital.todo.service.component.TodoMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 创建审计日志投递实现（I02）：有界队列 + 单线程异步消费。
 *
 * <p>可用性策略（系分 6.1）：队列满或消费异常时降级为本地 ERROR 日志**并计数告警**
 * （{@code todo_audit_degrade_total}），绝不抛出异常影响创建结果。停机时先停止拉取、
 * 等待队列排空，超时再中断，并记录未投递事件数，避免静默丢审计。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class AuditIntegrationImpl implements AuditIntegration {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditIntegrationImpl.class);

    /**
     * 审计事件队列容量
     */
    private static final int QUEUE_CAPACITY = 1024;

    /**
     * 消费线程优雅停止的等待秒数
     */
    private static final long SHUTDOWN_WAIT_SECONDS = 3L;

    /**
     * 消费线程轮询间隔秒数
     */
    private static final long POLL_INTERVAL_SECONDS = 1L;

    /**
     * 审计事件队列（有界，防止队列堆积拖垮内存）
     */
    private final BlockingQueue<String> eventQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    /**
     * 消费线程运行标记
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * 指标埋点
     */
    private final TodoMetrics todoMetrics;

    /**
     * 消费线程
     */
    private final Thread consumer;

    /**
     * 构造并启动消费线程（守护线程，不阻止应用退出）。
     *
     * @param todoMetrics 指标埋点
     */
    public AuditIntegrationImpl(TodoMetrics todoMetrics) {
        this.todoMetrics = todoMetrics;
        this.consumer = new Thread(this::consume, "todo-audit-consumer");
        this.consumer.setDaemon(true);
        this.consumer.start();
    }

    /**
     * 记录一次创建动作：入队即返回，队列满则降级为本地 ERROR 日志并计数。
     *
     * @param requestId  幂等键
     * @param todoNo     待办编号
     * @param operatorId 操作人用户 ID
     * @param success    是否创建成功
     */
    @Override
    public void recordCreate(String requestId, String todoNo, String operatorId, boolean success) {
        String event = buildEvent(requestId, todoNo, operatorId, success);
        boolean offered = eventQueue.offer(event);
        if (!offered) {
            todoMetrics.recordAuditDegrade();
            LOGGER.error("audit queue full, degrade to local log. event={}", event);
        }
    }

    /**
     * 停止消费线程：先停止拉取并等待排空，超时才中断，未投递事件计数并告警。
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        try {
            consumer.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_WAIT_SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("interrupted while waiting audit consumer shutdown", e);
        }
        if (consumer.isAlive()) {
            consumer.interrupt();
        }
        int remaining = eventQueue.size();
        if (remaining > 0) {
            todoMetrics.recordAuditDegrade();
            LOGGER.error("audit queue not drained on shutdown, dropped={}", remaining);
        }
    }

    /**
     * 消费循环：逐条投递，单条失败不影响后续事件。
     */
    private void consume() {
        while (running.get() || !eventQueue.isEmpty()) {
            try {
                String event = eventQueue.poll(POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
                if (event != null) {
                    LOGGER.info("todo audit event: {}", event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                int remaining = eventQueue.size();
                if (remaining > 0) {
                    todoMetrics.recordAuditDegrade();
                    LOGGER.error("audit consumer interrupted, dropped={}", remaining);
                }
                return;
            } catch (RuntimeException e) {
                // 弱依赖：投递异常降级为本地日志并计数，不影响创建主链路
                todoMetrics.recordAuditDegrade();
                LOGGER.error("audit dispatch failed, degrade to local log", e);
            }
        }
    }

    /**
     * 组装审计事件文本；事项名称与描述不进入审计事件，避免敏感信息外泄。
     *
     * @param requestId  幂等键
     * @param todoNo     待办编号
     * @param operatorId 操作人用户 ID
     * @param success    是否创建成功
     * @return 审计事件文本
     */
    private String buildEvent(String requestId, String todoNo, String operatorId, boolean success) {
        return "action=TODO_CREATE,requestId=" + requestId
                + ",todoNo=" + todoNo
                + ",operatorId=" + operatorId
                + ",success=" + success;
    }
}
