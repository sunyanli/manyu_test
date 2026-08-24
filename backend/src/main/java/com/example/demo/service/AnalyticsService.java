package com.example.demo.service;

import com.example.demo.dto.AnalyticsResponse;
import com.example.demo.repository.CallLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private CallLogRepository callLogRepository;

    public AnalyticsResponse getSummary(String dimension, LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> rawData;
        switch (dimension) {
            case "personType":
                rawData = callLogRepository.countByPersonType(startTime, endTime);
                break;
            case "personLevel":
                rawData = callLogRepository.countByPersonLevel(startTime, endTime);
                break;
            case "department":
                rawData = callLogRepository.countByDepartment(startTime, endTime);
                break;
            case "timeTrend":
                rawData = callLogRepository.countByTimeTrend(startTime, endTime);
                break;
            default:
                rawData = callLogRepository.countByPersonType(startTime, endTime);
                dimension = "personType";
        }

        List<AnalyticsResponse.SeriesItem> series = rawData.stream()
                .map(row -> new AnalyticsResponse.SeriesItem(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        long totalCalls = series.stream().mapToLong(AnalyticsResponse.SeriesItem::getValue).sum();

        return new AnalyticsResponse(dimension, series, totalCalls);
    }
}