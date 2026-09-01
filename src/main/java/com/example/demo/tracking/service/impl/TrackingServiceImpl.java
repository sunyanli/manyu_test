package com.example.demo.tracking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.tracking.dao.mapper.ApiCallLogMapper;
import com.example.demo.tracking.dao.mapper.UserInfoMapper;
import com.example.demo.tracking.model.entity.ApiCallLog;
import com.example.demo.tracking.model.entity.UserInfo;
import com.example.demo.tracking.model.request.CallStatsRequest;
import com.example.demo.tracking.model.request.DimensionStatsRequest;
import com.example.demo.tracking.model.vo.CallStatsVO;
import com.example.demo.tracking.model.vo.DimensionStatsVO;
import com.example.demo.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 埋点服务实现（异步记录到数据库）
 *
 * @author AiWork
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final ApiCallLogMapper apiCallLogMapper;
    private final UserInfoMapper userInfoMapper;

    @Value("${tracking.enabled:true}")
    private boolean trackingEnabled;

    public TrackingServiceImpl(ApiCallLogMapper apiCallLogMapper, UserInfoMapper userInfoMapper) {
        this.apiCallLogMapper = apiCallLogMapper;
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    @Async
    public void recordCall(String apiName, String userId) {
        if (!trackingEnabled) {
            return;
        }
        try {
            ApiCallLog log = new ApiCallLog();
            log.setApiName(apiName);
            log.setUserId(userId);
            log.setResponseCode("OK");
            log.setDurationMs(0);

            // 从 user_info 表获取用户维度信息
            UserInfo userInfo = userInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>()
                            .eq(UserInfo::getUserId, userId));
            if (userInfo != null) {
                log.setUserName(userInfo.getUserName());
                log.setUserType(userInfo.getUserType());
                log.setUserLevel(userInfo.getUserLevel());
                log.setUserDepartment(userInfo.getUserDepartment());
            } else {
                log.setUserName(null);
                log.setUserType("unknown");
                log.setUserLevel("unknown");
                log.setUserDepartment("unknown");
            }

            apiCallLogMapper.insert(log);
            logger.debug("埋点记录成功: api={}, user={}", apiName, userId);
        } catch (Exception e) {
            logger.error("埋点记录失败: api={}, user={}", apiName, userId, e);
        }
    }

    @Override
    public CallStatsVO queryCallStats(CallStatsRequest request) {
        String startTime = request.getStartTime() + " 00:00:00";
        String endTime = request.getEndTime() + " 23:59:59";

        List<Map<String, Object>> rows = apiCallLogMapper.callStatsByDay(
                null, startTime, endTime,
                request.getDimension(), request.getDimensionValue());

        List<CallStatsVO.SeriesPoint> series = new ArrayList<CallStatsVO.SeriesPoint>();
        long total = 0L;
        for (Map<String, Object> row : rows) {
            String time = (String) row.get("time");
            long count = ((Number) row.get("count")).longValue();
            series.add(new CallStatsVO.SeriesPoint(time, count));
            total += count;
        }

        logger.info("调用统计查询完成, total: {}", total);
        return new CallStatsVO(series, total);
    }

    @Override
    public DimensionStatsVO queryDimensionStats(DimensionStatsRequest request) {
        String startTime = request.getStartTime() + " 00:00:00";
        String endTime = request.getEndTime() + " 23:59:59";

        String dimension = request.getDimension();
        List<Map<String, Object>> rows = apiCallLogMapper.dimensionStats(
                dimension, startTime, endTime);

        long totalCount = 0L;
        for (Map<String, Object> row : rows) {
            totalCount += ((Number) row.get("count")).longValue();
        }

        List<DimensionStatsVO.DimensionItem> items = new ArrayList<DimensionStatsVO.DimensionItem>();
        for (Map<String, Object> row : rows) {
            String label = (String) row.get("label");
            if (label == null) {
                label = "unknown";
            }
            long count = ((Number) row.get("count")).longValue();
            double percentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0.0;
            items.add(new DimensionStatsVO.DimensionItem(label, count, percentage));
        }

        logger.info("维度统计查询完成, dimension: {}, items: {}", dimension, items.size());
        return new DimensionStatsVO(items);
    }
}