package com.manyu.algodemo.demo.model.dto;

import java.util.List;

/**
 * 冒泡排序接口出参。
 */
public class SortVO {

    /** 入参数组大小。 */
    private int originalSize;
    /** 排序结果（最多返回前 100 元素，完整结果由导出获取）。 */
    private List<Double> sorted;
    /** 交换次数。 */
    private long swaps;
    /** 处理耗时（毫秒）。 */
    private long costTimeMs;
    /** 算法版本。 */
    private String algorithmVersion;

    public int getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(int originalSize) {
        this.originalSize = originalSize;
    }

    public List<Double> getSorted() {
        return sorted;
    }

    public void setSorted(List<Double> sorted) {
        this.sorted = sorted;
    }

    public long getSwaps() {
        return swaps;
    }

    public void setSwaps(long swaps) {
        this.swaps = swaps;
    }

    public long getCostTimeMs() {
        return costTimeMs;
    }

    public void setCostTimeMs(long costTimeMs) {
        this.costTimeMs = costTimeMs;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }
}
