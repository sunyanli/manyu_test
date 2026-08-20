package com.manyu.algodemo.export.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 页面结果导出入参（W04）。
 */
public class ExportRequest {

    /** 导出目标：HELLO_WORLD/HASH/BUBBLE_SORT/REPORT。 */
    @NotBlank(message = "target 不能为空")
    private String target;

    /** 导出格式：CSV，默认 CSV。 */
    private String format;

    /** 记录时间范围起点（ISO-8601）。 */
    private String startTime;

    /** 记录时间范围终点（ISO-8601）。 */
    private String endTime;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

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
