package com.example.algorithm.model.dto;

import java.util.List;

/**
 * Response DTO for tracking statistics.
 */
public class TrackingStatisticsResponse {

    private List<StatisticsItem> data;

    public TrackingStatisticsResponse() {
    }

    public TrackingStatisticsResponse(List<StatisticsItem> data) {
        this.data = data;
    }

    public List<StatisticsItem> getData() {
        return data;
    }

    public void setData(List<StatisticsItem> data) {
        this.data = data;
    }

    /**
     * Statistics item.
     */
    public static class StatisticsItem {
        private String dimensionKey;
        private Long count;
        private String percentage;

        public StatisticsItem() {
        }

        public StatisticsItem(String dimensionKey, Long count) {
            this.dimensionKey = dimensionKey;
            this.count = count;
        }

        public StatisticsItem(String dimensionKey, Long count, String percentage) {
            this.dimensionKey = dimensionKey;
            this.count = count;
            this.percentage = percentage;
        }

        public String getDimensionKey() {
            return dimensionKey;
        }

        public void setDimensionKey(String dimensionKey) {
            this.dimensionKey = dimensionKey;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public String getPercentage() {
            return percentage;
        }

        public void setPercentage(String percentage) {
            this.percentage = percentage;
        }
    }
}
