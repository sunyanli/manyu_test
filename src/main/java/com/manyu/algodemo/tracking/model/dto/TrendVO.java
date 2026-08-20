package com.manyu.algodemo.tracking.model.dto;

import java.util.List;

/**
 * 时间趋势视图对象（W07，折线图数据源）。
 */
public class TrendVO {

    /** 粒度。 */
    private String granularity;
    /** 趋势点列表。 */
    private List<TrendPointVO> points;

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public List<TrendPointVO> getPoints() {
        return points;
    }

    public void setPoints(List<TrendPointVO> points) {
        this.points = points;
    }
}
