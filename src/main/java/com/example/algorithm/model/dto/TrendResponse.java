package com.example.algorithm.model.dto;

import java.util.List;

/**
 * Response DTO for tracking trend query.
 */
public class TrendResponse {

    private List<TrendItem> data;

    public TrendResponse() {
    }

    public TrendResponse(List<TrendItem> data) {
        this.data = data;
    }

    public List<TrendItem> getData() {
        return data;
    }

    public void setData(List<TrendItem> data) {
        this.data = data;
    }

    public static class TrendItem {
        private String timeLabel;
        private Long count;

        public TrendItem() {
        }

        public TrendItem(String timeLabel, Long count) {
            this.timeLabel = timeLabel;
            this.count = count;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public void setTimeLabel(String timeLabel) {
            this.timeLabel = timeLabel;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}
