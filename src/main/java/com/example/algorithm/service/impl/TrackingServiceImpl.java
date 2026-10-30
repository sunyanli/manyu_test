package com.example.algorithm.service.impl;

import com.example.algorithm.mapper.AlgorithmCallRecordMapper;
import com.example.algorithm.mapper.AlgorithmCallRecordSqlProvider;
import com.example.algorithm.model.dto.TrackingStatisticsResponse;
import com.example.algorithm.model.dto.TrendResponse;
import com.example.algorithm.model.entity.AlgorithmCallRecord;
import com.example.algorithm.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of tracking and statistics service.
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger LOG = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final AlgorithmCallRecordMapper callRecordMapper;

    public TrackingServiceImpl(AlgorithmCallRecordMapper callRecordMapper) {
        this.callRecordMapper = callRecordMapper;
    }

    @Override
    @Async
    public void recordCall(String apiPath, String algorithmType, String params,
                             String result, int costTimeMs, String callerId,
                             String callerName, String callerType, String callerLevel,
                             String callerDept) {
        try {
            AlgorithmCallRecord record = new AlgorithmCallRecord();
            record.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            record.setApiPath(apiPath);
            record.setAlgorithmType(algorithmType);
            record.setCallerId(callerId != null ? callerId : "UNKNOWN");
            record.setCallerName(callerName);
            record.setCallerType(callerType);
            record.setCallerLevel(callerLevel);
            record.setCallerDept(callerDept);
            record.setRequestParams(params);
            record.setResponseResult(result);
            record.setCostTimeMs(costTimeMs);
            callRecordMapper.insert(record);
        } catch (Exception e) {
            LOG.error("Failed to record call tracking", e);
        }
    }

    @Override
    public TrackingStatisticsResponse queryStatistics(String dimension, String startTime, String endTime) {
        String validatedDimension = AlgorithmCallRecordSqlProvider.validateDimension(dimension);
        List<Map<String, Object>> rows = callRecordMapper.selectStatisticsByDimension(
                validatedDimension, startTime, endTime);
        List<TrackingStatisticsResponse.StatisticsItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String key = row.get("dimension_key") != null ? row.get("dimension_key").toString() : "UNKNOWN";
            Long count = ((Number) row.get("count")).longValue();
            items.add(new TrackingStatisticsResponse.StatisticsItem(key, count));
        }
        return new TrackingStatisticsResponse(items);
    }

    @Override
    public TrendResponse queryTrend(String timeGranularity, String startTime, String endTime, String algorithmType) {
        String format = switch (timeGranularity.toUpperCase()) {
            case "DAY" -> "%Y-%m-%d";
            case "WEEK" -> "%Y-%u";
            case "MONTH" -> "%Y-%m";
            default -> "%Y-%m-%d";
        };
        List<Map<String, Object>> rows = callRecordMapper.selectTrend(format, startTime, endTime, algorithmType);
        List<TrendResponse.TrendItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String label = row.get("time_label") != null ? row.get("time_label").toString() : "";
            Long count = ((Number) row.get("count")).longValue();
            items.add(new TrendResponse.TrendItem(label, count));
        }
        return new TrendResponse(items);
    }

    @Override
    public TrackingStatisticsResponse queryDistribution(String dimension, String startTime, String endTime) {
        String validatedDimension = AlgorithmCallRecordSqlProvider.validateDimension(dimension);
        List<Map<String, Object>> rows = callRecordMapper.selectStatisticsByDimension(
                validatedDimension, startTime, endTime);
        List<TrackingStatisticsResponse.StatisticsItem> items = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> row : rows) {
            total += ((Number) row.get("count")).longValue();
        }
        for (Map<String, Object> row : rows) {
            String key = row.get("dimension_key") != null ? row.get("dimension_key").toString() : "UNKNOWN";
            Long count = ((Number) row.get("count")).longValue();
            String percentage = total > 0
                    ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                            .toString() + "%"
                    : "0%";
            items.add(new TrackingStatisticsResponse.StatisticsItem(key, count, percentage));
        }
        return new TrackingStatisticsResponse(items);
    }
}
