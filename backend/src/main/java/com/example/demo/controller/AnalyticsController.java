package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.AnalyticsResponse;
import com.example.demo.dto.ApiResponse;
import com.example.demo.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Traceable(apiName = "analytics")
    public ApiResponse<AnalyticsResponse> getSummary(
            @RequestParam(defaultValue = "personType") String dimension,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        if (startTime == null) startTime = LocalDateTime.now().minusDays(7);
        if (endTime == null) endTime = LocalDateTime.now();

        AnalyticsResponse response = analyticsService.getSummary(dimension, startTime, endTime);
        return ApiResponse.success(response);
    }
}