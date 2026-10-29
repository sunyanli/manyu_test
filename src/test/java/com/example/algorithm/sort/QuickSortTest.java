package com.example.algorithm.sort;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link QuickSort} 单元测试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class QuickSortTest {

    @Test
    void should_sortCorrectly_when_givenRandomArray() {
        // Arrange
        int[] array = new int[] {3, 6, 8, 10, 1, 2, 1};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(1, 1, 2, 3, 6, 8, 10);
    }

    @Test
    void should_returnEmpty_when_givenEmptyArray() {
        // Arrange
        int[] array = new int[] {};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).isEmpty();
    }

    @Test
    void should_returnSameElement_when_givenSingleElementArray() {
        // Arrange
        int[] array = new int[] {42};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(42);
    }

    @Test
    void should_returnSameOrder_when_givenAlreadySortedArray() {
        // Arrange
        int[] array = new int[] {1, 2, 3, 4, 5};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void should_sortCorrectly_when_givenReverseSortedArray() {
        // Arrange
        int[] array = new int[] {5, 4, 3, 2, 1};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void should_sortCorrectly_when_givenArrayWithDuplicates() {
        // Arrange
        int[] array = new int[] {3, 3, 3, 1, 2, 3, 2};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(1, 2, 2, 3, 3, 3, 3);
    }

    @Test
    void should_sortCorrectly_when_givenArrayWithNegativeNumbers() {
        // Arrange
        int[] array = new int[] {-3, 10, -1, 0, 5, -10};

        // Act
        QuickSort.sort(array);

        // Assert
        assertThat(array).containsExactly(-10, -3, -1, 0, 5, 10);
    }

    @Test
    void should_throwException_when_givenNullArray() {
        // Act & Assert
        assertThatThrownBy(() -> QuickSort.sort(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("array must not be null");
    }
}
