package com.antdigital.sort.impl;

import com.antdigital.sort.SortingAlgorithm;

import java.util.List;

/**
 * 优化版冒泡排序实现（升序 + 提前终止，F02）。
 *
 * <p>通过 {@code swapped} 标志检测每轮是否发生交换；
 * 若某轮无任何交换，说明列表已有序，提前退出，最优时间复杂度 O(n)。
 * 空间复杂度 O(1)，稳定排序。
 *
 * @author AiWork
 * @date 2026/09/22
 */
public class OptimizedBubbleSort implements SortingAlgorithm {

    /**
     * 对列表进行优化版原地升序排序。
     *
     * @param list 待排序列表
     * @param <T>  元素类型
     * @return 已排序的同一列表引用
     */
    @Override
    public <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        if (list == null) {
            throw new IllegalArgumentException("list must not be null");
        }
        int n = list.size();
        for (int i = 0; i < n; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    swap(list, j, j + 1);
                    swapped = true;
                }
            }
            // 本轮无交换，说明已有序，提前终止
            if (!swapped) {
                break;
            }
        }
        return list;
    }

    /**
     * 交换列表中两个位置的元素。
     */
    private static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }
}