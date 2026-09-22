package com.antdigital.sort.impl;

import com.antdigital.sort.SortingAlgorithm;

import java.util.List;

/**
 * 降序冒泡排序实现（降序 + 提前终止，F03）。
 *
 * <p>与优化版逻辑一致，仅比较条件取反（前 &lt; 后时交换）。
 * 空间复杂度 O(1)，稳定排序。
 *
 * @author AiWork
 * @date 2026/09/22
 */
public class DescendingBubbleSort implements SortingAlgorithm {

    /**
     * 对列表进行原地降序排序。
     *
     * @param list 待排序列表
     * @param <T>  元素类型
     * @return 已排好序（降序）的同一列表引用
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
                // 相邻比较：前 < 后则交换（降序）
                if (list.get(j).compareTo(list.get(j + 1)) < 0) {
                    swap(list, j, j + 1);
                    swapped = true;
                }
            }
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