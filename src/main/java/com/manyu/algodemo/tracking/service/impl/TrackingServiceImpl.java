package com.manyu.algodemo.tracking.service.impl;

import com.manyu.algodemo.common.exception.BizException;
import com.manyu.algodemo.common.exception.ErrorCode;
import com.manyu.algodemo.tracking.dao.TrackingMapper;
import com.manyu.algodemo.tracking.model.dto.OverviewVO;
import com.manyu.algodemo.tracking.model.dto.PeriodVO;
import com.manyu.algodemo.tracking.model.dto.StatsItemVO;
import com.manyu.algodemo.tracking.model.dto.StatsVO;
import com.manyu.algodemo.tracking.model.dto.TopCallerVO;
import com.manyu.algodemo.tracking.model.dto.TrendPointVO;
import com.manyu.algodemo.tracking.model.dto.TrendVO;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import com.manyu.algodemo.tracking.model.enums.StatsDimension;
import com.manyu.algodemo.tracking.model.enums.TrendGranularity;
import com.manyu.algodemo.tracking.service.TrackingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 埋点统计服务实现。
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    /** 时间范围最大跨度（天）。 */
    private static final long MAX_RANGE_DAYS = 90L;

    private final TrackingMapper trackingMapper;

    /**
     * 构造器注入。
     *
     * @param trackingMapper 埋点 Mapper
     */
    public TrackingServiceImpl(TrackingMapper trackingMapper) {
        this.trackingMapper = trackingMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OverviewVO overview(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        Map<String, Object> agg = trackingMapper.selectOverview(start, end);
        long totalCalls = toLong(agg.get("totalCalls"));
        long totalCallers = toLong(agg.get("totalCallers"));
        long successCount = toLong(agg.get("successCount"));
        long avgCostTimeMs = toLong(agg.get("avgCostTimeMs"));

        OverviewVO vo = new OverviewVO();
        vo.setTotalCalls(totalCalls);
        vo.setTotalCallers(totalCallers);
        vo.setSuccessRate(percent(successCount, totalCalls, 2));
        vo.setAvgCostTimeMs(avgCostTimeMs);
        vo.setPeriod(buildPeriod(start, end));
        vo.setTopCaller(buildTopCaller(trackingMapper.selectTopCaller(start, end)));
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatsVO stats(String dimension, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        StatsDimension dim = parseDimension(dimension);
        DimensionColumns columns = columnsOf(dim);
        List<Map<String, Object>> rows = trackingMapper.selectStatsByDimension(
                columns.nameColumn(), columns.groupByColumn(), start, end);
        long total = rows.stream().mapToLong(r -> toLong(r.get("value"))).sum();
        StatsVO vo = new StatsVO();
        vo.setDimension(dim.name());
        List<StatsItemVO> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            StatsItemVO item = new StatsItemVO();
            item.setName(String.valueOf(row.get("name")));
            long value = toLong(row.get("value"));
            item.setValue(value);
            item.setPercent(total == 0 ? BigDecimal.ZERO : percent(value, total, 1));
            items.add(item);
        }
        vo.setItems(items);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrendVO trend(String granularity, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        TrendGranularity gran = parseGranularity(granularity);
        List<Map<String, Object>> rows = trackingMapper.selectTrend(timeExprOf(gran), start, end);
        TrendVO vo = new TrendVO();
        vo.setGranularity(gran.name());
        List<TrendPointVO> points = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            TrendPointVO point = new TrendPointVO();
            point.setTime(String.valueOf(row.get("time")));
            long calls = toLong(row.get("calls"));
            long successCount = toLong(row.get("successCount"));
            point.setCalls(calls);
            point.setSuccessRate(percent(successCount, calls, 1));
            points.add(point);
        }
        vo.setPoints(points);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CallRecordDO> pageRecords(String bizType, LocalDateTime start, LocalDateTime end, int limit) {
        validateRange(start, end);
        return trackingMapper.selectRecentRecords(bizType, start, end, limit);
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new BizException(ErrorCode.TRACKING_001);
        }
        if (Duration.between(start, end).toDays() > MAX_RANGE_DAYS) {
            throw new BizException(ErrorCode.TRACKING_003);
        }
    }

    private StatsDimension parseDimension(String dimension) {
        try {
            return StatsDimension.valueOf(dimension);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.TRACKING_002, "不支持的统计维度: " + dimension);
        }
    }

    private TrendGranularity parseGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return TrendGranularity.DAY;
        }
        try {
            return TrendGranularity.valueOf(granularity);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.TRACKING_002, "不支持的趋势粒度: " + granularity);
        }
    }

    private DimensionColumns columnsOf(StatsDimension dim) {
        return switch (dim) {
            case CALLER_TYPE -> new DimensionColumns("caller_type", "caller_type");
            case CALLER_LEVEL -> new DimensionColumns("caller_level", "caller_level");
            case CALLER_DEPT -> new DimensionColumns("caller_dept_name", "caller_dept_code");
            case BIZ_TYPE -> new DimensionColumns("biz_type", "biz_type");
        };
    }

    private String timeExprOf(TrendGranularity gran) {
        return switch (gran) {
            case HOUR -> "DATE_FORMAT(gmt_create, '%Y-%m-%d %H:00:00')";
            case DAY -> "DATE_FORMAT(gmt_create, '%Y-%m-%d')";
            case MONTH -> "DATE_FORMAT(gmt_create, '%Y-%m')";
        };
    }

    private PeriodVO buildPeriod(LocalDateTime start, LocalDateTime end) {
        PeriodVO period = new PeriodVO();
        period.setStartTime(start.toString());
        period.setEndTime(end.toString());
        return period;
    }

    private TopCallerVO buildTopCaller(Map<String, Object> row) {
        TopCallerVO topCaller = new TopCallerVO();
        if (row == null || row.get("name") == null) {
            topCaller.setName("-");
            topCaller.setCalls(0);
            return topCaller;
        }
        topCaller.setName(maskName(String.valueOf(row.get("name"))));
        topCaller.setCalls(toLong(row.get("calls")));
        return topCaller;
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name == null ? "-" : name + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private BigDecimal percent(long part, long total, int scale) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), scale, RoundingMode.HALF_UP);
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** 维度列白名单映射（防 SQL 注入）。 */
    private record DimensionColumns(String nameColumn, String groupByColumn) {
    }
}
