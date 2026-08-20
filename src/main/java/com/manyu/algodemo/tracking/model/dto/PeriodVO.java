package com.manyu.algodemo.tracking.model.dto;

/**
 * 时间范围视图对象。
 */
public class PeriodVO {

    /** 起始时间（ISO-8601）。 */
    private String startTime;
    /** 截止时间（ISO-8601）。 */
    private String endTime;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
