package com.manyu.algodemo.tracking.model.dto;

import java.util.List;

/**
 * 维度统计视图对象（W06，饼图/柱状图数据源）。
 */
public class StatsVO {

    /** 统计维度。 */
    private String dimension;
    /** 维度分布项列表。 */
    private List<StatsItemVO> items;

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public List<StatsItemVO> getItems() {
        return items;
    }

    public void setItems(List<StatsItemVO> items) {
        this.items = items;
    }
}
