package com.example.algorithm.service;

import com.example.algorithm.model.dto.TrackingStatisticsResponse;
import com.example.algorithm.model.dto.TrendResponse;

/**
 * Tracking and statistics service.
 */
public interface TrackingService {

    /**
     * Record a call asynchronously.
     *
     * @param apiPath       the API path
     * @param algorithmType the algorithm type
     * @param params        request params JSON
     * @param result        result status
     * @param costTimeMs    cost time in ms
     */
    void recordCall(String apiPath, String algorithmType, String params, String result, int costTimeMs);

    /**
     * Query statistics by dimension.
     *
     * @param dimension the dimension
     * @param startTime start time
     * @param endTime   end time
     * @return statistics response
     */
    TrackingStatisticsResponse queryStatistics(String dimension, String startTime, String endTime);

    /**
     * Query trend data.
     *
     * @param timeGranularity time granularity
     * @param startTime       start time
     * @param endTime         end time
     * @param algorithmType   algorithm type filter
     * @return trend response
     */
    TrendResponse queryTrend(String timeGranularity, String startTime, String endTime, String algorithmType);

    /**
     * Query distribution by dimension.
     *
     * @param dimension the dimension
     * @param startTime start time
     * @param endTime   end time
     * @return distribution statistics
     */
    TrackingStatisticsResponse queryDistribution(String dimension, String startTime, String endTime);
}
