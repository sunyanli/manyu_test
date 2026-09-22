#!/usr/bin/env python3
"""
sortkit 排序算法单元测试

遵循 AAA（Arrange-Act-Assert）三段式结构，使用标准库 unittest，零第三方依赖。
运行方式：

    python3 -m unittest test_sortkit -v

@author AiWork
@date 2026/09/22
"""

import random
import unittest

from sortkit import (
    ALGORITHMS,
    InvalidInputError,
    NotComparableError,
    bubble_sort,
    bubble_sort_optimized,
    merge_sort,
    quick_sort,
    sort,
)


class _Tagged:
    """带标签的仅按 key 比较的元素，用于验证稳定排序（相等键保持相对顺序）。"""

    def __init__(self, key, tag):
        self.key = key
        self.tag = tag

    def __lt__(self, other):
        return self.key < other.key

    def __eq__(self, other):
        return self.key == other.key


class TestBubbleSort(unittest.TestCase):
    """标准冒泡排序测试。"""

    def test_should_return_ascending_order(self):
        # Arrange
        data = [5, 3, 8, 4, 2]
        # Act
        result = bubble_sort(data)
        # Assert
        self.assertEqual([2, 3, 4, 5, 8], result)

    def test_should_keep_empty_and_single(self):
        self.assertEqual([], bubble_sort([]))
        self.assertEqual([42], bubble_sort([42]))

    def test_should_handle_duplicates(self):
        self.assertEqual([2, 2, 2, 2], bubble_sort([2, 2, 2, 2]))

    def test_should_handle_reverse_ordered(self):
        self.assertEqual([1, 2, 3, 4, 5], bubble_sort([5, 4, 3, 2, 1]))


class TestBubbleSortOptimized(unittest.TestCase):
    """优化冒泡排序测试。"""

    def test_should_return_ascending_order(self):
        # Arrange
        data = [5, 1, 4, 2, 8]
        # Act
        result = bubble_sort_optimized(data)
        # Assert
        self.assertEqual([1, 2, 4, 5, 8], result)

    def test_should_keep_already_sorted(self):
        self.assertEqual([1, 2, 3, 4, 5], bubble_sort_optimized([1, 2, 3, 4, 5]))

    def test_should_handle_empty_and_duplicates(self):
        self.assertEqual([], bubble_sort_optimized([]))
        self.assertEqual([1, 1, 1], bubble_sort_optimized([1, 1, 1]))


class TestQuickSort(unittest.TestCase):
    """快速排序测试（默认算法）。"""

    def test_should_return_ascending_order(self):
        # Arrange
        data = [9, -3, 0, 7, -1]
        # Act
        result = quick_sort(data)
        # Assert
        self.assertEqual([-3, -1, 0, 7, 9], result)

    def test_should_handle_empty_and_single(self):
        self.assertEqual([], quick_sort([]))
        self.assertEqual([42], quick_sort([42]))

    def test_should_not_degrade_on_reverse_ordered(self):
        # 三数取中规避最坏退化，结果正确
        self.assertEqual(list(range(20)), quick_sort(list(range(19, -1, -1))))

    def test_should_handle_large_random_input(self):
        # Arrange
        data = [random.randint(-1000, 1000) for _ in range(200)]
        expected = sorted(data.copy())
        # Act
        result = quick_sort(data)
        # Assert
        self.assertEqual(expected, result)


class TestMergeSort(unittest.TestCase):
    """归并排序测试（稳定排序）。"""

    def test_should_return_ascending_order(self):
        self.assertEqual([1, 2, 3, 4, 5], merge_sort([5, 3, 1, 4, 2]))

    def test_should_handle_empty_and_single(self):
        self.assertEqual([], merge_sort([]))
        self.assertEqual([7], merge_sort([7]))

    def test_should_be_stable(self):
        # Arrange：两个 key=2 的元素，标签 a 应先于 c
        data = [_Tagged(2, "a"), _Tagged(1, "b"), _Tagged(2, "c")]
        # Act
        result = merge_sort(data)
        # Assert：key 升序，且相等 key 保持 [a, c] 相对顺序
        self.assertEqual([1, 2, 2], [t.key for t in result])
        self.assertEqual(["b", "a", "c"], [t.tag for t in result])

    def test_should_handle_large_random_input(self):
        data = [random.randint(-1000, 1000) for _ in range(200)]
        self.assertEqual(sorted(data.copy()), merge_sort(data))


class TestSortFacade(unittest.TestCase):
    """统一排序入口 sort() 测试。"""

    def test_should_default_to_quick_sort(self):
        # Arrange
        data = [5, 1, 4, 2, 8]
        # Act
        result = sort(data)
        # Assert
        self.assertEqual([1, 2, 4, 5, 8], result)

    def test_should_sort_in_place_and_return_same_reference(self):
        # Arrange
        data = [3, 1, 2]
        # Act
        result = sort(data)
        # Assert：原地排序且返回同一引用
        self.assertIs(data, result)

    def test_should_sort_descending_when_reverse_true(self):
        # Arrange
        data = [5, 1, 4, 2, 8]
        # Act
        result = sort(data, reverse=True)
        # Assert
        self.assertEqual([8, 5, 4, 2, 1], result)

    def test_should_support_each_registered_algorithm(self):
        # Arrange
        for name in ["bubble", "bubble_optimized", "quick", "merge"]:
            data = [6, 2, 9, 1, 4]
            # Act
            result = sort(data, algorithm=name)
            # Assert
            self.assertEqual([1, 2, 4, 6, 9], result, msg=name)

    def test_should_reject_none_data(self):
        # Act & Assert
        with self.assertRaises(InvalidInputError):
            sort(None)

    def test_should_reject_non_list_data(self):
        with self.assertRaises(InvalidInputError):
            sort((1, 2, 3))

    def test_should_reject_unknown_algorithm(self):
        with self.assertRaises(InvalidInputError):
            sort([3, 1, 2], algorithm="bubble_sort_typo")

    def test_should_wrap_incomparable_elements(self):
        # int 与 str 混排，比较时 TypeError 应被包装为 NotComparableError
        with self.assertRaises(NotComparableError):
            sort([1, "a", 2])

    def test_algorithms_registry_matches_implementations(self):
        # Arrange
        expected = ["bubble", "bubble_optimized", "quick", "merge"]
        # Assert
        self.assertEqual(expected, ALGORITHMS)


if __name__ == "__main__":
    unittest.main(verbosity=2)