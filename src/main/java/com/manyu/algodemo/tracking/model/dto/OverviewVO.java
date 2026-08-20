package com.manyu.algodemo.tracking.model.dto;

import java.math.BigDecimal;

/**
 * 调用概况视图对象（W05）。
 */
public class OverviewVO {

    /** 总调用次数。 */
    private long totalCalls;
    /** 调用人数。 */
    private long totalCallers;
    /** 成功率（0-100，两位小数）。 */
    private BigDecimal successRate;
    /** 平均耗时（毫秒）。 */
    private long avgCostTimeMs;
    /** 统计时间范围。 */
    private PeriodVO period;
    /** 调用最多的人。 */
    private TopCallerVO topCaller;

    public long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public long getTotalCallers() {
        return totalCallers;
    }

    public void setTotalCallers(long totalCallers) {
        this.totalCallers = totalCallers;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public long getAvgCostTimeMs() {
        return avgCostTimeMs;
    }

    public void setAvgCostTimeMs(long avgCostTimeMs) {
        this.avgCostTimeMs = avgCostTimeMs;
    }

    public PeriodVO getPeriod() {
        return period;
    }

    public void setPeriod(PeriodVO period) {
        this.period = period;
    }

    public TopCallerVO getTopCaller() {
        return topCaller;
    }

    public void setTopCaller(TopCallerVO topCaller) {
        this.topCaller = topCaller;
    }
}
