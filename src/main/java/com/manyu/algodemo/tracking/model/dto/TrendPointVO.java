package com.manyu.algodemo.tracking.model.dto;

import java.math.BigDecimal;

/**
 * 趋势点视图对象。
 */
public class TrendPointVO {

    /** 时间标识（如 2026-08-19）。 */
    private String time;
    /** 调用次数。 */
    private long calls;
    /** 成功率（0-100，一位小数）。 */
    private BigDecimal successRate;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }
}
