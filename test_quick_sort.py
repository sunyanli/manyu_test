#!/usr/bin/env python3
"""
Quick Sort 单元测试

遵循 AAA（Arrange-Act-Assert）三段式结构，使用标准库 unittest，零第三方依赖。
运行方式：

    python3 -m unittest test_quick_sort -v

@author AiWork
@date 2026/10/29
"""

import random
import unittest

from quick_sort import INSERTION_THRESHOLD, quick_sort


class TestQuickSort(unittest.TestCase):
    """快速排序测试：正常路径、边界值、防退化、稳定性说明、原地语义。"""

    def test_should_return_ascending_order(self):
        # Arrange
        data = [9, -3, 0, 7, -1]
        # Act
        result = quick_sort(data)
        # Assert
        self.assertEqual([-3, -1, 0, 7, 9], result)

    def test_should_keep_empty_and_single(self):
        self.assertEqual([], quick_sort([]))
        self.assertEqual([42], quick_sort([42]))

    def test_should_keep_already_sorted(self):
        self.assertEqual([1, 2, 3, 4, 5], quick_sort([1, 2, 3, 4, 5]))

    def test_should_swap_two_element_reverse_pair(self):
        self.assertEqual([1, 2], quick_sort([2, 1]))

    def test_should_handle_duplicates(self):
        # Arrange
        data = [5, 2, 8, 2, 5, 8]
        # Act
        result = quick_sort(data)
        # Assert
        self.assertEqual([2, 2, 5, 5, 8, 8], result)

    def test_should_handle_all_equal_elements(self):
        self.assertEqual([2, 2, 2, 2, 2], quick_sort([2, 2, 2, 2, 2]))

    def test_should_not_degrade_on_reverse_ordered(self):
        # 三数取中规避反向有序（最坏）退化，结果仍正确
        n = 100
        self.assertEqual(list(range(n)), quick_sort(list(range(n - 1, -1, -1))))

    def test_should_handle_negative_and_positive_mixed(self):
        data = [-7, 100, 0, -100, 50, -1]
        self.assertEqual([-100, -7, -1, 0, 50, 100], quick_sort(data))

    def test_should_sort_in_place_and_return_same_reference(self):
        # Arrange
        data = [3, 1, 2]
        # Act
        result = quick_sort(data)
        # Assert：原地排序且返回同一引用
        self.assertIs(data, result)

    def test_should_handle_large_random_input(self):
        # Arrange
        data = [random.randint(-1000, 1000) for _ in range(500)]
        expected = sorted(data.copy())
        # Act
        result = quick_sort(data)
        # Assert
        self.assertEqual(expected, result)

    def test_should_define_insertion_threshold_constant(self):
        # 常量存在且为正值，用于小规模子区切换插入排序
        self.assertGreaterEqual(INSERTION_THRESHOLD, 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)