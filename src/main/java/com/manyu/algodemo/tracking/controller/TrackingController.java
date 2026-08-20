package com.manyu.algodemo.tracking.controller;

import com.manyu.algodemo.common.web.CommonResponse;
import com.manyu.algodemo.tracking.model.dto.OverviewVO;
import com.manyu.algodemo.tracking.model.dto.StatsVO;
import com.manyu.algodemo.tracking.model.dto.TrendVO;
import com.manyu.algodemo.tracking.service.TrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * tracking 模块控制器：W05 调用概况 / W06 维度统计 / W07 时间趋势。
 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    /** 与数据源 serverTimezone=Asia/Shanghai 保持一致，避免 JVM 默认时区漂移（M016）。 */
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private final TrackingService trackingService;

    /**
     * 构造器注入。
     *
     * @param trackingService 埋点统计服务
     */
    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * W05 调用概况查询。
     *
     * @param startTime 起始时间（默认近 30 天）
     * @param endTime   截止时间（默认当前）
     * @return 概况数据
     */
    @GetMapping("/overview")
    public CommonResponse<OverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LocalDateTime end = endTime == null ? LocalDateTime.now(APP_ZONE) : endTime;
        LocalDateTime start = startTime == null ? end.minusDays(30) : startTime;
        return CommonResponse.ok(trackingService.overview(start, end));
    }

    /**
     * W06 维度统计查询。
     *
     * @param dimension 维度：CALLER_TYPE/CALLER_LEVEL/CALLER_DEPT/BIZ_TYPE
     * @param startTime 起始时间
     * @param endTime   截止时间
     * @return 维度分布数据
     */
    @GetMapping("/stats")
    public CommonResponse<StatsVO> stats(
            @RequestParam String dimension,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LocalDateTime end = endTime == null ? LocalDateTime.now(APP_ZONE) : endTime;
        LocalDateTime start = startTime == null ? end.minusDays(30) : startTime;
        return CommonResponse.ok(trackingService.stats(dimension, start, end));
    }

    /**
     * W07 时间趋势查询。
     *
     * @param granularity 粒度：HOUR/DAY/MONTH
     * @param dimension   可选维度细分（如 CALLER_TYPE=EMPLOYEE）
     * @param startTime   起始时间
     * @param endTime     截止时间
     * @return 趋势数据
     */
    @GetMapping("/trend")
    public CommonResponse<TrendVO> trend(
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LocalDateTime end = endTime == null ? LocalDateTime.now(APP_ZONE) : endTime;
        LocalDateTime start = startTime == null ? end.minusDays(30) : startTime;
        return CommonResponse.ok(trackingService.trend(granularity, dimension, start, end));
    }
}
