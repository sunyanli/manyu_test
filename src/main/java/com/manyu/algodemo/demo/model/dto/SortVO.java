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

    /**
     * 摘要字符串：含排序结果前 10 元素，供埋点出参摘要记录（不含完整数组，避免超长落库）。
     *
     * @return 形如 {@code originalSize=5,sorted=[2.0, 3.0, ...],swaps=6}
     */
    @Override
    public String toString() {
        String sortedPreview = preview(sorted, 10);
        return "originalSize=" + originalSize + ",sorted=" + sortedPreview + ",swaps=" + swaps;
    }

    private String preview(List<Double> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<Double> head = values.size() <= limit ? values : values.subList(0, limit);
        return head + (values.size() > limit ? "..." : "");
    }
}
