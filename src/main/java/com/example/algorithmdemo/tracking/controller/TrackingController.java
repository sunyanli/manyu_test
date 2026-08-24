package com.example.algorithmdemo.tracking.controller;

import com.example.algorithmdemo.common.exception.BusinessException;
import com.example.algorithmdemo.common.exception.GlobalExceptionHandler;
import com.example.algorithmdemo.tracking.service.TrackingService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 埋点统计控制器
 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * W05 - 调用统计报表接口
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(
            @RequestParam String dimension,
            @RequestParam(required = false, defaultValue = "bar") String chartType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        if (!List.of("user_type", "user_level", "user_dept").contains(dimension)) {
            throw BusinessException.unsupportedDimension();
        }

        List<Map<String, Object>> seriesData;
        List<String> labels;

        if ("line".equalsIgnoreCase(chartType)) {
            // 折线图 - 按时间趋势
            List<Map<String, Object>> timeData = trackingService.getStatisticsByTimeRange(startTime, endTime);
            labels = new ArrayList<>();
            List<Object> counts = new ArrayList<>();
            for (Map<String, Object> item : timeData) {
                labels.add(String.valueOf(item.get("label")));
                counts.add(item.get("count"));
            }
            Map<String, Object> series = new HashMap<>();
            series.put("name", "调用次数");
            series.put("data", counts);
            seriesData = List.of(series);
        } else {
            // 饼图/柱状图 - 按维度聚合
            List<Map<String, Object>> dimData = trackingService.getStatisticsByDimension(dimension);
            labels = new ArrayList<>();
            List<Object> counts = new ArrayList<>();
            for (Map<String, Object> item : dimData) {
                labels.add(String.valueOf(item.get("label")));
                counts.add(item.get("count"));
            }
            Map<String, Object> series = new HashMap<>();
            series.put("name", "调用次数");
            series.put("data", counts);
            seriesData = List.of(series);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimension", dimension);
        data.put("chartType", chartType);
        data.put("labels", labels);
        data.put("series", seriesData);

        return GlobalExceptionHandler.buildResult("SUCCESS", "操作成功", data);
    }

    /**
     * W06 - 调用记录明细接口
     */
    @GetMapping("/records")
    public Map<String, Object> getRecords(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        List<?> records = trackingService.getRecords(apiName, startTime, endTime, page, size);
        long total = trackingService.getTotalCount(apiName, startTime, endTime);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", page);
        data.put("size", size);
        data.put("total", total);
        data.put("records", records);

        return GlobalExceptionHandler.buildResult("SUCCESS", "操作成功", data);
    }
}