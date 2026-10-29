package com.example.algorithm.service.impl;

import com.example.algorithm.mapper.AlgorithmCallRecordMapper;
import com.example.algorithm.model.dto.TrackingStatisticsResponse;
import com.example.algorithm.model.dto.TrendResponse;
import com.example.algorithm.model.entity.AlgorithmCallRecord;
import com.example.algorithm.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    public void recordCall(String apiPath, String algorithmType, String params, String result, int costTimeMs) {
        try {
            AlgorithmCallRecord record = new AlgorithmCallRecord();
            record.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            record.setApiPath(apiPath);
            record.setAlgorithmType(algorithmType);
            record.setCallerId(getCallerIdFromHeader());
            record.setCallerName(getHeaderValue("X-Caller-Name"));
            record.setCallerType(getHeaderValue("X-Caller-Type"));
            record.setCallerLevel(getHeaderValue("X-Caller-Level"));
            record.setCallerDept(getHeaderValue("X-Caller-Dept"));
            record.setRequestParams(params);
            record.setResponseResult(result);
            record.setCostTimeMs(costTimeMs);
            callRecordMapper.insert(record);
        } catch (Exception e) {
            LOG.error("Failed to record call tracking", e);
        }
    }

    private String getCallerIdFromHeader() {
        String callerId = getHeaderValue("X-User-Id");
        return callerId != null ? callerId : "UNKNOWN";
    }

    private String getHeaderValue(String headerName) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader(headerName);
            }
        } catch (Exception e) {
            LOG.debug("Unable to get header value for {}", headerName);
        }
        return null;
    }

    @Override
    public TrackingStatisticsResponse queryStatistics(String dimension, String startTime, String endTime) {
        List<Map<String, Object>> rows = callRecordMapper.selectStatisticsByDimension(dimension, startTime, endTime);
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
        List<Map<String, Object>> rows = callRecordMapper.selectStatisticsByDimension(dimension, startTime, endTime);
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
