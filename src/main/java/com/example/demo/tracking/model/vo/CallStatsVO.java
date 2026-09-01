package com.example.demo.tracking.model.vo;

import java.util.List;

/**
 * 调用统计响应（时序数据）
 *
 * @author AiWork
 */
public class CallStatsVO {

    /** 时序数据点 */
    private List<SeriesPoint> series;

    /** 总调用次数 */
    private long total;

    public CallStatsVO() {
    }

    public CallStatsVO(List<SeriesPoint> series, long total) {
        this.series = series;
        this.total = total;
    }

    public List<SeriesPoint> getSeries() {
        return series;
    }

    public void setSeries(List<SeriesPoint> series) {
        this.series = series;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 时序数据点
     */
    public static class SeriesPoint {

        /** 时间 */
        private String time;

        /** 调用次数 */
        private long count;

        public SeriesPoint() {
        }

        public SeriesPoint(String time, long count) {
            this.time = time;
            this.count = count;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "SeriesPoint{time='" + time + "', count=" + count + "}";
        }
    }

    @Override
    public String toString() {
        return "CallStatsVO{series=" + series + ", total=" + total + "}";
    }
}