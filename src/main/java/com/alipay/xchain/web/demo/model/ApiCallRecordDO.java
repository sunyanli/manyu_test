package com.alipay.xchain.web.demo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 接口调用埋点记录实体
 *
 * @author AiWork
 * @since 2026-10-30
 */
@TableName("api_call_record")
public class ApiCallRecordDO {

    /** 系统自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接口名（hello/hash/sort） */
    private String apiName;

    /** 调用人工号 */
    private String callerId;

    /** 调用人姓名 */
    private String callerName;

    /** 人员类型 */
    private String personType;

    /** 人员层级 */
    private String personLevel;

    /** 人员部门 */
    private String personDept;

    /** 调用时间 */
    private LocalDateTime callTime;

    /** 调用结果（success/failure） */
    private String resultStatus;

    /** 记录创建时间 */
    private LocalDateTime gmtCreate;

    /** 记录修改时间 */
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
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

    public String getPersonType() {
        return personType;
    }

    public void setPersonType(String personType) {
        this.personType = personType;
    }

    public String getPersonLevel() {
        return personLevel;
    }

    public void setPersonLevel(String personLevel) {
        this.personLevel = personLevel;
    }

    public String getPersonDept() {
        return personDept;
    }

    public void setPersonDept(String personDept) {
        this.personDept = personDept;
    }

    public LocalDateTime getCallTime() {
        return callTime;
    }

    public void setCallTime(LocalDateTime callTime) {
        this.callTime = callTime;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
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

    @Override
    public String toString() {
        return "ApiCallRecordDO{" +
                "id=" + id +
                ", apiName='" + apiName + '\'' +
                ", callerId='" + callerId + '\'' +
                ", callerName='" + callerName + '\'' +
                ", personType='" + personType + '\'' +
                ", personLevel='" + personLevel + '\'' +
                ", personDept='" + personDept + '\'' +
                ", callTime=" + callTime +
                ", resultStatus='" + resultStatus + '\'' +
                '}';
    }
}