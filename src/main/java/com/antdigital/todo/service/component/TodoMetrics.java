package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 创建链路指标埋点（G16.1）：系分 §6.5 五项指标与 §7.1 依赖/业务埋点的唯一出口。
 *
 * <p>指标清单：{@code todo_create_total{code}}、{@code todo_create_cost_ms}（P50/P99）、
 * {@code todo_create_idempotent_hit_total}、{@code todo_create_rate_limited_total}、
 * {@code todo_no_degrade_total}、{@code todo_create_gray_blocked_total}、
 * {@code todo_audit_degrade_total}。由 {@code /actuator/prometheus} 暴露，供告警规则消费。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoMetrics {

    /**
     * 创建调用总数（按错误码维度）
     */
    private static final String METRIC_CREATE_TOTAL = "todo_create_total";

    /**
     * 创建耗时（毫秒）
     */
    private static final String METRIC_CREATE_COST_MS = "todo_create_cost_ms";

    /**
     * 幂等命中次数
     */
    private static final String METRIC_IDEMPOTENT_HIT = "todo_create_idempotent_hit_total";

    /**
     * 限流次数
     */
    private static final String METRIC_RATE_LIMITED = "todo_create_rate_limited_total";

    /**
     * 取号降级次数
     */
    private static final String METRIC_NO_DEGRADE = "todo_no_degrade_total";

    /**
     * 灰度/开关拦截次数
     */
    private static final String METRIC_GRAY_BLOCKED = "todo_create_gray_blocked_total";

    /**
     * 审计降级次数
     */
    private static final String METRIC_AUDIT_DEGRADE = "todo_audit_degrade_total";

    /**
     * 错误码标签名
     */
    private static final String TAG_CODE = "code";

    /**
     * P50 分位
     */
    private static final double PERCENTILE_P50 = 0.5D;

    /**
     * P99 分位
     */
    private static final double PERCENTILE_P99 = 0.99D;

    /**
     * 指标注册表
     */
    private final MeterRegistry meterRegistry;

    /**
     * 创建耗时分布
     */
    private final DistributionSummary createCostSummary;

    /**
     * 幂等命中计数器
     */
    private final Counter idempotentHitCounter;

    /**
     * 限流计数器
     */
    private final Counter rateLimitedCounter;

    /**
     * 取号降级计数器
     */
    private final Counter noDegradeCounter;

    /**
     * 灰度拦截计数器
     */
    private final Counter grayBlockedCounter;

    /**
     * 审计降级计数器
     */
    private final Counter auditDegradeCounter;

    /**
     * 构造并注册指标。
     *
     * @param meterRegistry 指标注册表
     */
    public TodoMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.createCostSummary = DistributionSummary.builder(METRIC_CREATE_COST_MS)
                .description("待办创建耗时（毫秒）")
                .publishPercentiles(PERCENTILE_P50, PERCENTILE_P99)
                .register(meterRegistry);
        this.idempotentHitCounter = Counter.builder(METRIC_IDEMPOTENT_HIT)
                .description("待办创建幂等命中次数")
                .register(meterRegistry);
        this.rateLimitedCounter = Counter.builder(METRIC_RATE_LIMITED)
                .description("待办创建被限流次数")
                .register(meterRegistry);
        this.noDegradeCounter = Counter.builder(METRIC_NO_DEGRADE)
                .description("待办编号取号降级次数")
                .register(meterRegistry);
        this.grayBlockedCounter = Counter.builder(METRIC_GRAY_BLOCKED)
                .description("待办创建被开关/灰度拦截次数")
                .register(meterRegistry);
        this.auditDegradeCounter = Counter.builder(METRIC_AUDIT_DEGRADE)
                .description("待办审计投递降级次数")
                .register(meterRegistry);
    }

    /**
     * 记录一次创建成功。
     *
     * @param costMillis 耗时（毫秒）
     */
    public void recordCreateSuccess(long costMillis) {
        recordCreate(TodoConstants.RESULT_CODE_OK, costMillis);
    }

    /**
     * 记录一次创建失败，并按错误码补充专项计数。
     *
     * @param code       错误码
     * @param costMillis 耗时（毫秒）
     */
    public void recordCreateFailure(String code, long costMillis) {
        recordCreate(code, costMillis);
        if (TodoErrorCodeEnum.RATE_LIMITED.getCode().equals(code)) {
            rateLimitedCounter.increment();
        }
        if (TodoErrorCodeEnum.FEATURE_DISABLED.getCode().equals(code)) {
            grayBlockedCounter.increment();
        }
    }

    /**
     * 记录幂等命中。
     */
    public void recordIdempotentHit() {
        idempotentHitCounter.increment();
    }

    /**
     * 记录一次取号降级。
     */
    public void recordNoDegrade() {
        noDegradeCounter.increment();
    }

    /**
     * 记录一次审计降级，支撑 R10「ERROR 日志 + 监控告警」。
     */
    public void recordAuditDegrade() {
        auditDegradeCounter.increment();
    }

    /**
     * 记录创建总数与耗时。
     *
     * @param code       结果码
     * @param costMillis 耗时（毫秒）
     */
    private void recordCreate(String code, long costMillis) {
        Counter.builder(METRIC_CREATE_TOTAL)
                .description("待办创建调用总数")
                .tag(TAG_CODE, code)
                .register(meterRegistry)
                .increment();
        if (costMillis >= 0) {
            createCostSummary.record(costMillis);
        }
    }
}
