package com.example.algorithm.sort;

import java.util.Objects;

/**
 * 快速排序工具类。
 *
 * <p>提供基于 Hoare 分区方案（partition scheme）的快速排序实现，
 * 对 {@code int[]} 进行原地（in-place）排序，平均时间复杂度 O(n log n)。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public final class QuickSort {

    private QuickSort() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 对整数数组进行快速排序。
     *
     * <p>排序结果按非递减顺序排列，排序过程为原地操作。
     *
     * @param array 待排序数组，允许为空数组但不可为 {@code null}
     * @throws NullPointerException 当 {@code array} 为 {@code null} 时抛出
     */
    public static void sort(int[] array) {
        Objects.requireNonNull(array, "array must not be null");
        if (array.length < 2) {
            return;
        }
        sort(array, 0, array.length - 1);
    }

    /**
     * 在指定索引范围内对整数数组进行快速排序。
     *
     * @param array 待排序数组
     * @param low   排序范围左边界（包含）
     * @param high  排序范围右边界（包含）
     */
    private static void sort(int[] array, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(array, low, high);
            sort(array, low, pivotIndex - 1);
            sort(array, pivotIndex + 1, high);
        }
    }

    /**
     * 对数组进行分区，使 pivot 左侧元素均小于等于 pivot，右侧元素均大于等于 pivot。
     *
     * <p>采用 Lomuto 分区方案，选取当前区间最后一个元素作为 pivot。
     *
     * @param array 待分区数组
     * @param low   分区区间左边界（包含）
     * @param high  分区区间右边界（包含）
     * @return pivot 最终位置
     */
    private static int partition(int[] array, int low, int high) {
        int pivot = array[high];
        int storeIndex = low - 1;
        for (int i = low; i < high; i++) {
            if (array[i] <= pivot) {
                storeIndex++;
                swap(array, storeIndex, i);
            }
        }
        swap(array, storeIndex + 1, high);
        return storeIndex + 1;
    }

    /**
     * 交换数组中两个位置的元素。
     *
     * @param array 待交换数组
     * @param i     第一个元素索引
     * @param j     第二个元素索引
     */
    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
