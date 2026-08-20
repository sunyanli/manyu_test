package com.manyu.algodemo.tracking.model.entity;

import java.time.LocalDateTime;

/**
 * call_record 埋点调用记录数据对象。
 */
public class CallRecordDO {

    /** 系统自增主键。 */
    private Long id;
    /** 业务类型。 */
    private String bizType;
    /** 调用人ID。 */
    private String callerId;
    /** 调用人姓名。 */
    private String callerName;
    /** 人员类型。 */
    private String callerType;
    /** 人员层级。 */
    private String callerLevel;
    /** 人员部门编码。 */
    private String callerDeptCode;
    /** 人员部门名称。 */
    private String callerDeptName;
    /** 入参摘要。 */
    private String reqSummary;
    /** 出参摘要。 */
    private String respSummary;
    /** 处理耗时（毫秒）。 */
    private Long costTimeMs;
    /** 结果状态。 */
    private String resultStatus;
    /** 失败错误码。 */
    private String errorCode;
    /** 创建时间（调用时间）。 */
    private LocalDateTime gmtCreate;
    /** 修改时间。 */
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
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

    public String getCallerDeptCode() {
        return callerDeptCode;
    }

    public void setCallerDeptCode(String callerDeptCode) {
        this.callerDeptCode = callerDeptCode;
    }

    public String getCallerDeptName() {
        return callerDeptName;
    }

    public void setCallerDeptName(String callerDeptName) {
        this.callerDeptName = callerDeptName;
    }

    public String getReqSummary() {
        return reqSummary;
    }

    public void setReqSummary(String reqSummary) {
        this.reqSummary = reqSummary;
    }

    public String getRespSummary() {
        return respSummary;
    }

    public void setRespSummary(String respSummary) {
        this.respSummary = respSummary;
    }

    public Long getCostTimeMs() {
        return costTimeMs;
    }

    public void setCostTimeMs(Long costTimeMs) {
        this.costTimeMs = costTimeMs;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
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
