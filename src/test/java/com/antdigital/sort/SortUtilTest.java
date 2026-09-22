package com.antdigital.sort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SortUtil} 排序门面单元测试。
 *
 * <p>覆盖标准升序、优化升序、降序三变体，以及空/单元素/有序/逆序/全等/含负数/稳定性/非法入参等场景。
 *
 * @author AiWork
 * @date 2026/09/22
 */
class SortUtilTest {

    // ==================== sort（标准升序）测试 ====================

    @Test
    void should_sortAscending_when_unsortedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(5, 3, 8, 4, 2));

        // Act
        List<Integer> result = SortUtil.sort(input);

        // Assert
        assertThat(result).containsExactly(2, 3, 4, 5, 8);
        assertThat(result).isSameAs(input);
    }

    @Test
    void should_keepOrder_when_alreadySorted() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        // Act
        List<Integer> result = SortUtil.sort(input);

        // Assert
        assertThat(result).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void should_sortAscending_when_reversedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(5, 4, 3, 2, 1));

        // Act
        List<Integer> result = SortUtil.sort(input);

        // Assert
        assertThat(result).containsExactly(1, 2, 3, 4, 5);
    }

    // ==================== sortOptimized（优化升序）测试 ====================

    @Test
    void should_sortAscending_when_optimizedWithUnsortedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(9, -3, 0, 7, -1));

        // Act
        List<Integer> result = SortUtil.sortOptimized(input);

        // Assert
        assertThat(result).containsExactly(-3, -1, 0, 7, 9);
    }

    @Test
    void should_terminateEarly_when_optimizedWithAlreadySortedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        // Act
        List<Integer> result = SortUtil.sortOptimized(input);

        // Assert
        assertThat(result).containsExactly(1, 2, 3, 4, 5);
    }

    // ==================== sortDescending（降序）测试 ====================

    @Test
    void should_sortDescending_when_unsortedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));

        // Act
        List<Integer> result = SortUtil.sortDescending(input);

        // Assert
        assertThat(result).containsExactly(5, 4, 3, 1, 1);
    }

    @Test
    void should_sortDescending_when_reversedList() {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        // Act
        List<Integer> result = SortUtil.sortDescending(input);

        // Assert
        assertThat(result).containsExactly(5, 4, 3, 2, 1);
    }

    // ==================== 边界值测试（三变体共用） ====================

    @ParameterizedTest
    @MethodSource("provideVariants")
    void should_returnSameList_when_emptyList(Variant variant) {
        // Arrange
        List<Integer> input = new ArrayList<>();

        // Act
        List<Integer> result = variant.call(input);

        // Assert
        assertThat(result).isEmpty();
        assertThat(result).isSameAs(input);
    }

    @ParameterizedTest
    @MethodSource("provideVariants")
    void should_returnSameList_when_singleElement(Variant variant) {
        // Arrange
        List<Integer> input = new ArrayList<>(Collections.singletonList(42));

        // Act
        List<Integer> result = variant.call(input);

        // Assert
        assertThat(result).containsExactly(42);
        assertThat(result).isSameAs(input);
    }

    @ParameterizedTest
    @MethodSource("provideVariants")
    void should_keepEqualElements_when_allEqual(Variant variant) {
        // Arrange
        List<Integer> input = new ArrayList<>(Arrays.asList(2, 2, 2, 2));

        // Act
        List<Integer> result = variant.call(input);

        // Assert
        assertThat(result).containsExactly(2, 2, 2, 2);
    }

    @ParameterizedTest
    @MethodSource("provideVariants")
    void should_throwException_when_listIsNull(Variant variant) {
        // Act & Assert
        assertThatThrownBy(() -> variant.call(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== 稳定性测试 ====================

    @Test
    void should_keepRelativeOrder_when_equalKeysInStandardSort() {
        // Arrange
        List<ComparableKey> input = new ArrayList<>(Arrays.asList(
                new ComparableKey(2, 1),
                new ComparableKey(1, 1),
                new ComparableKey(2, 2),
                new ComparableKey(1, 2)));

        // Act
        List<ComparableKey> result = SortUtil.sort(input);

        // Assert - key 相等元素（同 key 的 seq）保持相对次序
        assertThat(result).extracting(k -> k.key).containsExactly(1, 1, 2, 2);
        assertThat(result.get(0).seq).isEqualTo(1);
        assertThat(result.get(1).seq).isEqualTo(2);
        assertThat(result.get(2).seq).isEqualTo(1);
        assertThat(result.get(3).seq).isEqualTo(2);
    }

    @Test
    void should_keepRelativeOrder_when_equalKeysInOptimizedSort() {
        // Arrange
        List<ComparableKey> input = new ArrayList<>(Arrays.asList(
                new ComparableKey(5, 1),
                new ComparableKey(3, 1),
                new ComparableKey(5, 2)));

        // Act
        List<ComparableKey> result = SortUtil.sortOptimized(input);

        // Assert
        assertThat(result).extracting(k -> k.key).containsExactly(3, 5, 5);
        assertThat(result.get(1).seq).isEqualTo(1);
        assertThat(result.get(2).seq).isEqualTo(2);
    }

    // ==================== 测试数据 ====================

    /** 键相等、带序号的不可变元素，用于验证稳定排序（相等元素相对次序不变）。 */
    private static final class ComparableKey implements Comparable<ComparableKey> {
        private final int key;
        private final int seq;

        ComparableKey(int key, int seq) {
            this.key = key;
            this.seq = seq;
        }

        @Override
        public int compareTo(ComparableKey other) {
            return Integer.compare(this.key, other.key);
        }
    }

    static Stream<Variant> provideVariants() {
        return Stream.of(
                new Variant("standard", SortUtil::sort),
                new Variant("optimized", SortUtil::sortOptimized),
                new Variant("descending", input -> {
                    // 降序对相同输入 {2,2,2,2} 与升序结果一致，只用于空/单元素/null 断言
                    return SortUtil.sortDescending(input);
                }));
    }

    /** 函数式测试载体，用于对三种变体统一驱动边界用例。 */
    private static final class Variant {
        private final String name;
        private final java.util.function.Function<List<Integer>, List<Integer>> fn;

        Variant(String name, java.util.function.Function<List<Integer>, List<Integer>> fn) {
            this.name = name;
            this.fn = fn;
        }

        List<Integer> call(List<Integer> list) {
            return fn.apply(list);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}