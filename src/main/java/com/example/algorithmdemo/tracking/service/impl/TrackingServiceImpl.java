package com.example.algorithmdemo.tracking.service.impl;

import com.example.algorithmdemo.dao.mapper.CallRecordMapper;
import com.example.algorithmdemo.model.entity.CallRecord;
import com.example.algorithmdemo.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 埋点追踪服务实现
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final CallRecordMapper callRecordMapper;

    public TrackingServiceImpl(CallRecordMapper callRecordMapper) {
        this.callRecordMapper = callRecordMapper;
    }

    @Async("trackingExecutor")
    @Override
    public void recordCall(String userId, String userName, String userType,
                           String userLevel, Long userDeptId,
                           String apiName, String result) {
        try {
            CallRecord record = new CallRecord();
            record.setUserId(userId != null ? userId : "anonymous");
            record.setUserName(userName != null ? userName : "anonymous");
            record.setUserType(userType != null ? userType : "unknown");
            record.setUserLevel(userLevel != null ? userLevel : "unknown");
            record.setUserDeptId(userDeptId != null ? userDeptId : 0L);
            record.setApiName(apiName);
            record.setCallResult(result);
            record.setCallTime(LocalDateTime.now());

            callRecordMapper.insert(record);
            log.debug("埋点记录成功: apiName={}, userId={}", apiName, userId);
        } catch (Exception e) {
            log.error("埋点记录失败: apiName={}, userId={}", apiName, userId, e);
        }
    }

    @Override
    public List<Map<String, Object>> getStatisticsByDimension(String dimension) {
        return callRecordMapper.countByDimension(dimension);
    }

    @Override
    public List<Map<String, Object>> getStatisticsByTimeRange(String startTime, String endTime) {
        return callRecordMapper.countByTimeRange(startTime, endTime);
    }

    @Override
    public List<CallRecord> getRecords(String apiName, String startTime, String endTime, int page, int size) {
        int offset = (page - 1) * size;
        return callRecordMapper.selectList(apiName, startTime, endTime, offset, size);
    }

    @Override
    public long getTotalCount(String apiName, String startTime, String endTime) {
        return callRecordMapper.countTotal(apiName, startTime, endTime);
    }
}