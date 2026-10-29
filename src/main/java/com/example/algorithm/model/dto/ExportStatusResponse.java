package com.example.algorithm.model.dto;

/**
 * Response DTO for export task status query.
 */
public class ExportStatusResponse {

    private String taskId;
    private String status;
    private String fileUrl;

    public ExportStatusResponse() {
    }

    public ExportStatusResponse(String taskId, String status, String fileUrl) {
        this.taskId = taskId;
        this.status = status;
        this.fileUrl = fileUrl;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
