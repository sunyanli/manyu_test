package com.example.demo.tracking.model.vo;

import java.util.List;

/**
 * 维度统计响应（饼图/柱状图）
 *
 * @author AiWork
 */
public class DimensionStatsVO {

    /** 维度统计项 */
    private List<DimensionItem> items;

    public DimensionStatsVO() {
    }

    public DimensionStatsVO(List<DimensionItem> items) {
        this.items = items;
    }

    public List<DimensionItem> getItems() {
        return items;
    }

    public void setItems(List<DimensionItem> items) {
        this.items = items;
    }

    /**
     * 维度统计项
     */
    public static class DimensionItem {

        /** 标签 */
        private String label;

        /** 调用次数 */
        private long count;

        /** 占比 */
        private double percentage;

        public DimensionItem() {
        }

        public DimensionItem(String label, long count, double percentage) {
            this.label = label;
            this.count = count;
            this.percentage = percentage;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }

        @Override
        public String toString() {
            return "DimensionItem{label='" + label + "', count=" + count
                    + ", percentage=" + percentage + "}";
        }
    }

    @Override
    public String toString() {
        return "DimensionStatsVO{items=" + items + "}";
    }
}