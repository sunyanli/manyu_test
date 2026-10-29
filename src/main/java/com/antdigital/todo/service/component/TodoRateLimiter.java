package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 单用户创建频率限制器（R11）：本地滑动窗口计数，默认 30 次/分钟，阈值可由配置项覆盖。
 *
 * <p>口径说明：本实现为**单实例**限流，多副本部署时单用户实际额度约为阈值 × 副本数，
 * 集群级限额由网关层限流保证（系分 §6.3「本地 + 网关双层」），详见
 * {@code docs/modules/todo/README.md}。窗口内时间戳队列按访问裁剪，并对空闲用户条目做
 * 阈值触发的整体回收，避免用户维度 Map 无界增长。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoRateLimiter {

    /**
     * 每用户每分钟允许的创建次数
     */
    private final int limitPerMinute;

    /**
     * 当前毫秒时钟
     */
    private final LongSupplier clock;

    /**
     * 用户维度的时间戳队列
     */
    private final Map<String, Deque<Long>> timestamps = new ConcurrentHashMap<>();

    /**
     * 生产构造：阈值取自配置项 {@code todo.create.rate-limit-per-minute}。
     *
     * @param limitPerMinute 每分钟上限
     */
    @Autowired
    public TodoRateLimiter(@Value("${todo.create.rate-limit-per-minute:30}") int limitPerMinute) {
        this(limitPerMinute, System::currentTimeMillis);
    }

    /**
     * 全参构造，供配置注入与单测使用。
     *
     * @param limitPerMinute 每分钟上限，需大于 0
     * @param clock          当前毫秒时钟
     */
    public TodoRateLimiter(int limitPerMinute, LongSupplier clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    /**
     * 尝试获取一次创建许可。
     *
     * @param userId 可信用户 ID
     * @throws TodoBizException 超出频率限制时抛出 TODO_0009
     */
    public void acquire(String userId) {
        long now = clock.getAsLong();
        evictIdleUsers(now);
        Deque<Long> queue = timestamps.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (queue) {
            evictExpired(queue, now);
            if (queue.size() >= limitPerMinute) {
                throw new TodoBizException(TodoErrorCodeEnum.RATE_LIMITED);
            }
            queue.addLast(now);
        }
    }

    /**
     * 回收已完全滑出窗口的用户条目：仅在用户数超过阈值时触发，避免每次请求全量遍历。
     *
     * @param now 当前毫秒
     */
    private void evictIdleUsers(long now) {
        if (timestamps.size() < TodoConstants.RATE_LIMIT_IDLE_SCAN_THRESHOLD) {
            return;
        }
        for (Map.Entry<String, Deque<Long>> entry : timestamps.entrySet()) {
            Deque<Long> queue = entry.getValue();
            synchronized (queue) {
                evictExpired(queue, now);
                if (queue.isEmpty()) {
                    timestamps.remove(entry.getKey(), queue);
                }
            }
        }
    }

    /**
     * 清理滑出窗口的时间戳。
     *
     * @param queue 时间戳队列
     * @param now   当前毫秒
     */
    private void evictExpired(Deque<Long> queue, long now) {
        long threshold = now - TodoConstants.RATE_LIMIT_WINDOW_MILLIS;
        Iterator<Long> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Long timestamp = iterator.next();
            if (timestamp != null && timestamp > threshold) {
                break;
            }
            iterator.remove();
        }
    }
}
