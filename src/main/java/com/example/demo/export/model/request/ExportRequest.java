package com.example.demo.export.model.request;

import javax.validation.constraints.NotBlank;

/**
 * 导出请求
 *
 * @author AiWork
 */
public class ExportRequest {

    /** 导出类型：helloworld/hash/bubble_sort */
    @NotBlank(message = "导出类型不能为空")
    private String exportType;

    /** 开始时间（yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 结束时间（yyyy-MM-dd HH:mm:ss） */
    private String endTime;

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
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

    @Override
    public String toString() {
        return "ExportRequest{exportType='" + exportType + "', startTime='" + startTime
                + "', endTime='" + endTime + "'}";
    }
}