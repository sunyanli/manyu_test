package com.antdigital.todo.common.metrics;

import com.antdigital.todo.common.api.ResultCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 新增待办接口的可观测指标埋点。
 *
 * <p>暴露两个指标，供 QPS、成功率、P99 与错误码分布告警使用：</p>
 * <ul>
 *     <li>{@code todo.create.requests}：按 {@code result}（OK 或 TODO_00x）分桶的调用量</li>
 *     <li>{@code todo.create.cost}：创建成功请求的耗时分布，用于计算 P99 RT</li>
 * </ul>
 *
 * @author AiWork
 */
@Component
public class TodoCreateMetrics {

    /** 调用量指标名。 */
    public static final String METRIC_REQUESTS = "todo.create.requests";

    /** 耗时指标名。 */
    public static final String METRIC_COST = "todo.create.cost";

    /** 结果码标签名。 */
    public static final String TAG_RESULT = "result";

    private final MeterRegistry meterRegistry;

    public TodoCreateMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录一次成功创建。
     *
     * @param costNanos 处理耗时（纳秒）
     */
    public void recordSuccess(long costNanos) {
        meterRegistry.counter(METRIC_REQUESTS, TAG_RESULT, ResultCode.OK.getCode()).increment();
        meterRegistry.timer(METRIC_COST).record(costNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 记录一次失败。
     *
     * @param resultCode 失败结果码，如 TODO_001
     */
    public void recordFailure(String resultCode) {
        if (resultCode == null || resultCode.isEmpty()) {
            return;
        }
        meterRegistry.counter(METRIC_REQUESTS, TAG_RESULT, resultCode).increment();
    }

    /**
     * 读取指定结果码的累计调用量，供单测与排障使用。
     *
     * @param resultCode 结果码
     * @return 累计次数
     */
    public double countOf(String resultCode) {
        return meterRegistry.counter(METRIC_REQUESTS, TAG_RESULT, resultCode).count();
    }

    /**
     * 读取成功耗时统计，供单测与排障使用。
     *
     * @return 耗时统计
     */
    public Timer costTimer() {
        return meterRegistry.timer(METRIC_COST);
    }
}
