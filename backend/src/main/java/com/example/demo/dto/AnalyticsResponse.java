package com.example.demo.dto;

import java.util.List;

public class AnalyticsResponse {
    private String dimension;
    private List<SeriesItem> series;
    private long totalCalls;

    public AnalyticsResponse() {}

    public AnalyticsResponse(String dimension, List<SeriesItem> series, long totalCalls) {
        this.dimension = dimension;
        this.series = series;
        this.totalCalls = totalCalls;
    }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public List<SeriesItem> getSeries() { return series; }
    public void setSeries(List<SeriesItem> series) { this.series = series; }
    public long getTotalCalls() { return totalCalls; }
    public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }

    public static class SeriesItem {
        private String label;
        private long value;

        public SeriesItem() {}

        public SeriesItem(String label, long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
    }
}