package com.manyu.algodemo.common.context;

/**
 * 调用人信息：调用发生时从登录上下文解析，并冗余快照进 call_record 支撑多维度报表统计。
 */
public class CallerInfo {

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
}
