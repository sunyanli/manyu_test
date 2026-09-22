package com.antdigital.sort.impl;

import com.antdigital.sort.SortingAlgorithm;

import java.util.List;

/**
 * 冒泡排序实现公共基类：统一提供空引用校验与相邻元素交换能力，
 * 消除标准版/优化版/降序版三实现之间重复的 {@code swap} 与判空代码。
 *
 * @author AiWork
 * @date 2026/09/22
 */
abstract class AbstractBubbleSort implements SortingAlgorithm {

    /**
     * 校验入参列表不为 null，否则抛出 {@link IllegalArgumentException}。
     *
     * @param list 待排序列表
     * @param <T>  元素类型
     */
    protected final <T> void checkNotNull(List<T> list) {
        if (list == null) {
            throw new IllegalArgumentException("list must not be null");
        }
    }

    /**
     * 交换列表中两个位置的元素。
     *
     * @param list 目标列表
     * @param i    位置下标
     * @param j    位置下标
     * @param <T>  元素类型
     */
    protected final <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }
}
