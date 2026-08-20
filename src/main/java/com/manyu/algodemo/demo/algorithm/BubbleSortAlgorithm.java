package com.manyu.algodemo.demo.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * 冒泡排序算法（Java 进程内实现，逻辑对齐 manyu_test 仓 {@code bubble_sort.py} 三变体）。
 *
 * <p>标准版每轮完整遍历；优化版通过 swapped 标志提前终止；降序按反向比较。</p>
 */
public final class BubbleSortAlgorithm {

    private BubbleSortAlgorithm() {
    }

    /**
     * 执行冒泡排序。
     *
     * @param input     待排序数组（不修改入参，返回新列表）
     * @param ascending 是否升序；false 表示降序
     * @param optimized 是否启用优化版（提前终止）
     * @return 排序结果（列表 + 交换次数）
     */
    public static SortResult sort(List<Double> input, boolean ascending, boolean optimized) {
        List<Double> arr = new ArrayList<>(input);
        int n = arr.size();
        long swaps = 0;
        for (int i = 0; i < n; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                boolean needSwap = ascending
                        ? arr.get(j) > arr.get(j + 1)
                        : arr.get(j) < arr.get(j + 1);
                if (needSwap) {
                    Double tmp = arr.get(j);
                    arr.set(j, arr.get(j + 1));
                    arr.set(j + 1, tmp);
                    swaps++;
                    swapped = true;
                }
            }
            if (optimized && !swapped) {
                break;
            }
        }
        return new SortResult(arr, swaps, optimized ? "v1.0-optimized" : "v1.0-standard");
    }

    /**
     * 排序结果。
     */
    public static final class SortResult {

        private final List<Double> sorted;
        private final long swaps;
        private final String algorithmVersion;

        private SortResult(List<Double> sorted, long swaps, String algorithmVersion) {
            this.sorted = sorted;
            this.swaps = swaps;
            this.algorithmVersion = algorithmVersion;
        }

        public List<Double> getSorted() {
            return sorted;
        }

        public long getSwaps() {
            return swaps;
        }

        public String getAlgorithmVersion() {
            return algorithmVersion;
        }
    }
}
