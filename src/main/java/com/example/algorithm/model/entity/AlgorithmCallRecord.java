package com.example.algorithm.model.entity;

import java.time.LocalDateTime;

/**
 * Entity for algorithm call record (tracking).
 */
public class AlgorithmCallRecord {

    private Long id;
    private String requestId;
    private String apiPath;
    private String algorithmType;
    private String callerId;
    private String callerName;
    private String callerType;
    private String callerLevel;
    private String callerDept;
    private String requestParams;
    private String responseResult;
    private Integer costTimeMs;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getCallerType() {
        return callerType;
    }

    public void setCallerType(String callerType) {
        this.callerType = callerType;
    }

    public String getCallerLevel() {
        return callerLevel;
    }

    public void setCallerLevel(String callerLevel) {
        this.callerLevel = callerLevel;
    }

    public String getCallerDept() {
        return callerDept;
    }

    public void setCallerDept(String callerDept) {
        this.callerDept = callerDept;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseResult() {
        return responseResult;
    }

    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }

    public Integer getCostTimeMs() {
        return costTimeMs;
    }

    public void setCostTimeMs(Integer costTimeMs) {
        this.costTimeMs = costTimeMs;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }
}
