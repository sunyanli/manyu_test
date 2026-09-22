package com.antdigital.sort.impl;

import java.util.List;

/**
 * 标准冒泡排序实现（升序，F01）。
 *
 * <p>每次遍历将最大的元素"冒泡"到数组末尾。
 * 时间复杂度 O(n²)（最坏/平均），空间复杂度 O(1)，稳定排序。
 *
 * @author AiWork
 * @date 2026/09/22
 */
public class BubbleSort extends AbstractBubbleSort {

    /**
     * 对列表进行原地升序排序。
     *
     * @param list 待排序列表
     * @param <T>  元素类型
     * @return 已排序的同一列表引用
     */
    @Override
    public <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        checkNotNull(list);
        int n = list.size();
        for (int i = 0; i < n; i++) {
            // 每轮结束后，末尾 i 个元素已有序，内层范围收窄
            for (int j = 0; j < n - i - 1; j++) {
                // 相邻比较：前 > 后则交换（升序）
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    swap(list, j, j + 1);
                }
            }
        }
        return list;
    }
}