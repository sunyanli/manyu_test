package com.example.algorithmdemo.model.entity;

import java.time.LocalDateTime;

/**
 * 接口调用记录实体
 */
public class CallRecord {

    private Long id;

    /** 调用人用户ID */
    private String userId;

    /** 调用人姓名 */
    private String userName;

    /** 人员类型（正式/实习/外包） */
    private String userType;

    /** 人员层级（P5/P6/P7/M1等） */
    private String userLevel;

    /** 人员所属部门ID */
    private Long userDeptId;

    /** 调用的接口名称 */
    private String apiName;

    /** 调用结果（SUCCESS/FAIL） */
    private String callResult;

    /** 调用时间 */
    private LocalDateTime callTime;

    /** 创建时间 */
    private LocalDateTime gmtCreate;

    /** 修改时间 */
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getUserLevel() { return userLevel; }
    public void setUserLevel(String userLevel) { this.userLevel = userLevel; }

    public Long getUserDeptId() { return userDeptId; }
    public void setUserDeptId(Long userDeptId) { this.userDeptId = userDeptId; }

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public String getCallResult() { return callResult; }
    public void setCallResult(String callResult) { this.callResult = callResult; }

    public LocalDateTime getCallTime() { return callTime; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}