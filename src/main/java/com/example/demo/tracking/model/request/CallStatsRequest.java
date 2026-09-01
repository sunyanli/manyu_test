package com.example.demo.tracking.model.request;

import javax.validation.constraints.NotBlank;

/**
 * 调用统计请求
 *
 * @author AiWork
 */
public class CallStatsRequest {

    /** 开始时间（yyyy-MM-dd） */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间（yyyy-MM-dd） */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 粒度：day（默认）/hour */
    private String granularity;

    /** 筛选维度：user_type/user_level/user_department */
    private String dimension;

    /** 维度值 */
    private String dimensionValue;

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

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getDimensionValue() {
        return dimensionValue;
    }

    public void setDimensionValue(String dimensionValue) {
        this.dimensionValue = dimensionValue;
    }

    @Override
    public String toString() {
        return "CallStatsRequest{startTime='" + startTime + "', endTime='" + endTime
                + "', granularity='" + granularity + "', dimension='" + dimension
                + "', dimensionValue='" + dimensionValue + "'}";
    }
}