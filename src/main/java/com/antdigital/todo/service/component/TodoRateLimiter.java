package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 单用户创建频率限制器（R11）：本地滑动窗口计数，默认 30 次/分钟。
 *
 * <p>说明：本地限流为单实例口径，网关联调时再叠加网关侧全局限流；
 * 窗口内时间戳队列按访问裁剪，因此无需定时清理线程。
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
     * 默认构造：30 次/分钟，系统时钟。
     */
    public TodoRateLimiter() {
        this(TodoConstants.DEFAULT_RATE_LIMIT_PER_MINUTE, System::currentTimeMillis);
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
