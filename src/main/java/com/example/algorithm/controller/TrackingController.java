package com.example.algorithm.controller;

import com.example.algorithm.model.dto.ApiResponse;
import com.example.algorithm.model.dto.TrackingStatisticsResponse;
import com.example.algorithm.model.dto.TrendResponse;
import com.example.algorithm.service.TrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for tracking and statistics APIs.
 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/statistics")
    public ApiResponse<TrackingStatisticsResponse> queryStatistics(
            @RequestParam String dimension,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        TrackingStatisticsResponse response = trackingService.queryStatistics(dimension, startTime, endTime);
        return ApiResponse.ok(response);
    }

    @GetMapping("/trend")
    public ApiResponse<TrendResponse> queryTrend(
            @RequestParam String timeGranularity,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String algorithmType) {
        TrendResponse response = trackingService.queryTrend(timeGranularity, startTime, endTime, algorithmType);
        return ApiResponse.ok(response);
    }

    @GetMapping("/distribution")
    public ApiResponse<TrackingStatisticsResponse> queryDistribution(
            @RequestParam String dimension,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        TrackingStatisticsResponse response = trackingService.queryDistribution(dimension, startTime, endTime);
        return ApiResponse.ok(response);
    }
}
