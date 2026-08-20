package com.manyu.algodemo.demo.algorithm;

import com.manyu.algodemo.demo.algorithm.BubbleSortAlgorithm.SortResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 冒泡排序算法测试：覆盖标准版/优化版/降序/边界场景，逻辑对齐 bubble_sort.py。
 */
class BubbleSortAlgorithmTest {

    @Test
    @DisplayName("标准版升序排序正确")
    void should_sortAscending_withStandardAlgorithm() {
        SortResult result = BubbleSortAlgorithm.sort(List.of(5.0, 3.0, 8.0, 4.0, 2.0), true, false);
        assertThat(result.getSorted()).containsExactly(2.0, 3.0, 4.0, 5.0, 8.0);
        assertThat(result.getSwaps()).isEqualTo(6);
        assertThat(result.getAlgorithmVersion()).isEqualTo("v1.0-standard");
    }

    @Test
    @DisplayName("优化版升序排序正确")
    void should_sortAscending_withOptimizedAlgorithm() {
        SortResult result = BubbleSortAlgorithm.sort(List.of(5.0, 1.0, 4.0, 2.0, 8.0), true, true);
        assertThat(result.getSorted()).containsExactly(1.0, 2.0, 4.0, 5.0, 8.0);
        assertThat(result.getAlgorithmVersion()).isEqualTo("v1.0-optimized");
    }

    @Test
    @DisplayName("已排序数组优化版提前终止且无交换")
    void should_breakEarly_whenAlreadySorted() {
        SortResult result = BubbleSortAlgorithm.sort(List.of(1.0, 2.0, 3.0, 4.0, 5.0), true, true);
        assertThat(result.getSorted()).containsExactly(1.0, 2.0, 3.0, 4.0, 5.0);
        assertThat(result.getSwaps()).isZero();
    }

    @Test
    @DisplayName("降序排序正确")
    void should_sortDescending() {
        SortResult result = BubbleSortAlgorithm.sort(List.of(3.0, 1.0, 4.0, 1.0, 5.0), false, true);
        assertThat(result.getSorted()).containsExactly(5.0, 4.0, 3.0, 1.0, 1.0);
    }

    @Test
    @DisplayName("空数组与单元素数组")
    void should_handleEmptyAndSingleElement() {
        assertThat(BubbleSortAlgorithm.sort(List.of(), true, true).getSorted()).isEmpty();
        assertThat(BubbleSortAlgorithm.sort(List.of(42.0), true, true).getSorted()).containsExactly(42.0);
    }

    @Test
    @DisplayName("重复元素稳定排序")
    void should_sortDuplicateElements() {
        SortResult result = BubbleSortAlgorithm.sort(List.of(2.0, 2.0, 2.0, 2.0), true, true);
        assertThat(result.getSorted()).containsExactly(2.0, 2.0, 2.0, 2.0);
    }

    @Test
    @DisplayName("不修改入参列表")
    void should_notMutateInput() {
        List<Double> input = new java.util.ArrayList<>(List.of(5.0, 3.0, 8.0));
        BubbleSortAlgorithm.sort(input, true, false);
        assertThat(input).containsExactly(5.0, 3.0, 8.0);
    }
}
