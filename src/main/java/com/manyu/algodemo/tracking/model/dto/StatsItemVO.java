package com.manyu.algodemo.tracking.model.dto;

import java.math.BigDecimal;

/**
 * 维度统计项视图对象。
 */
public class StatsItemVO {

    /** 维度取值名称。 */
    private String name;
    /** 调用次数。 */
    private long value;
    /** 占比（0-100，一位小数）。 */
    private BigDecimal percent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }
}
