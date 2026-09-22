package com.antdigital.sort;

import com.antdigital.sort.impl.BubbleSort;
import com.antdigital.sort.impl.DescendingBubbleSort;
import com.antdigital.sort.impl.OptimizedBubbleSort;

import java.util.List;

/**
 * 排序门面：对外暴露静态排序入口，屏蔽具体实现细节。
 *
 * <p>后续如需替换算法（如快速/归并），仅需修改内部实现映射，调用方无感知。
 *
 * @author AiWork
 * @date 2026/09/22
 */
public final class SortUtil {

    /** 标准升序实现（F01 最简基线） */
    private static final SortingAlgorithm STANDARD_SORT = new BubbleSort();

    /** 优化版升序实现（F02 提前终止） */
    private static final SortingAlgorithm OPTIMIZED_SORT = new OptimizedBubbleSort();

    /** 降序实现（F03） */
    private static final SortingAlgorithm DESCENDING_SORT = new DescendingBubbleSort();

    private SortUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 标准升序排序（S01）。
     *
     * @param list 待排序列表；null 抛 {@link IllegalArgumentException}
     * @param <T>  元素类型
     * @return 已排序的同一列表引用
     */
    public static <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        return STANDARD_SORT.sort(list);
    }

    /**
     * 优化版升序排序（S02，无交换提前终止，最优 O(n)）。
     *
     * @param list 待排序列表；null 抛 {@link IllegalArgumentException}
     * @param <T>  元素类型
     * @return 已排序的同一列表引用
     */
    public static <T extends Comparable<? super T>> List<T> sortOptimized(List<T> list) {
        return OPTIMIZED_SORT.sort(list);
    }

    /**
     * 降序排序（S03）。
     *
     * @param list 待排序列表；null 抛 {@link IllegalArgumentException}
     * @param <T>  元素类型
     * @return 已排好序（降序）的同一列表引用
     */
    public static <T extends Comparable<? super T>> List<T> sortDescending(List<T> list) {
        return DESCENDING_SORT.sort(list);
    }
}