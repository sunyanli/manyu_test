package com.alipay.xchain.web.demo.response;

import java.util.List;

/**
 * 统计结果 VO
 *
 * @author AiWork
 * @since 2026-10-30
 */
public class StatsResultVO {

    /** 统计维度：personType/personLevel/personDept */
    private String dimension;

    /** 分桶明细 */
    private List<StatsItem> items;

    /** 日趋势 */
    private List<TrendItem> trend;

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public List<StatsItem> getItems() {
        return items;
    }

    public void setItems(List<StatsItem> items) {
        this.items = items;
    }

    public List<TrendItem> getTrend() {
        return trend;
    }

    public void setTrend(List<TrendItem> trend) {
        this.trend = trend;
    }

    /**
     * 分桶统计项
     */
    public static class StatsItem {
        private String name;
        private Integer callCount;

        public StatsItem() {}

        public StatsItem(String name, Integer callCount) {
            this.name = name;
            this.callCount = callCount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCallCount() {
            return callCount;
        }

        public void setCallCount(Integer callCount) {
            this.callCount = callCount;
        }
    }

    /**
     * 日趋势项
     */
    public static class TrendItem {
        private String date;
        private Integer callCount;

        public TrendItem() {}

        public TrendItem(String date, Integer callCount) {
            this.date = date;
            this.callCount = callCount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getCallCount() {
            return callCount;
        }

        public void setCallCount(Integer callCount) {
            this.callCount = callCount;
        }
    }

    @Override
    public String toString() {
        return "StatsResultVO{dimension='" + dimension + "'}";
    }
}