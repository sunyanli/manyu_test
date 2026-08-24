package com.example.algorithmdemo.tracking.service;

import com.example.algorithmdemo.model.entity.CallRecord;

import java.util.List;
import java.util.Map;

/**
 * 埋点追踪服务接口
 */
public interface TrackingService {

    /**
     * 记录接口调用
     *
     * @param userId     用户ID
     * @param userName   用户姓名
     * @param userType   人员类型
     * @param userLevel  人员层级
     * @param userDeptId 部门ID
     * @param apiName    接口名称
     * @param result     调用结果
     */
    void recordCall(String userId, String userName, String userType,
                    String userLevel, Long userDeptId,
                    String apiName, String result);

    /**
     * 按维度统计调用次数
     *
     * @param dimension 统计维度 (user_type/user_level/user_dept)
     * @return 统计结果
     */
    List<Map<String, Object>> getStatisticsByDimension(String dimension);

    /**
     * 按时间范围统计调用次数
     */
    List<Map<String, Object>> getStatisticsByTimeRange(String startTime, String endTime);

    /**
     * 查询调用记录列表
     */
    List<CallRecord> getRecords(String apiName, String startTime, String endTime, int page, int size);

    /**
     * 查询总记录数
     */
    long getTotalCount(String apiName, String startTime, String endTime);
}