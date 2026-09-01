package com.example.demo.tracking.model.request;

import javax.validation.constraints.NotBlank;

/**
 * 维度统计请求
 *
 * @author AiWork
 */
public class DimensionStatsRequest {

    /** 开始时间 */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间 */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 聚合维度：user_type/user_level/user_department */
    @NotBlank(message = "维度不能为空")
    private String dimension;

    /** 图表类型：pie（默认）/bar */
    private String chartType;

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

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    @Override
    public String toString() {
        return "DimensionStatsRequest{startTime='" + startTime + "', endTime='" + endTime
                + "', dimension='" + dimension + "', chartType='" + chartType + "'}";
    }
}