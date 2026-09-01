"""排序算法单元测试"""

import doctest
import unittest

import sorting.bubble_sort
import sorting._interface


class TestSortingDoctest(unittest.TestCase):
    def test_doctests(self):
        """运行模块的 doctest"""
        results = doctest.testmod(sorting.bubble_sort)
        self.assertEqual(results.failed, 0, f"doctest 失败: {results.failed}")


class TestBubbleSorter(unittest.TestCase):
    def setUp(self):
        self.sorter = sorting.bubble_sort.BubbleSorter()

    def test_algorithm_name(self):
        self.assertEqual(self.sorter.algorithm_name, "bubble_sort")

    def test_time_complexity(self):
        self.assertEqual(self.sorter.time_complexity, "O(n²)")

    def test_space_complexity(self):
        self.assertEqual(self.sorter.space_complexity, "O(1)")

    def test_sort_unsorted(self):
        self.assertEqual(self.sorter.sort([3, 1, 2]), [1, 2, 3])

    def test_sort_already_sorted(self):
        self.assertEqual(self.sorter.sort([1, 2, 3, 4, 5]), [1, 2, 3, 4, 5])

    def test_sort_reverse_sorted(self):
        self.assertEqual(self.sorter.sort([5, 4, 3, 2, 1]), [1, 2, 3, 4, 5])

    def test_sort_empty(self):
        self.assertEqual(self.sorter.sort([]), [])

    def test_sort_single_element(self):
        self.assertEqual(self.sorter.sort([42]), [42])

    def test_sort_duplicates(self):
        self.assertEqual(self.sorter.sort([2, 2, 2, 2]), [2, 2, 2, 2])

    def test_sort_negative_numbers(self):
        self.assertEqual(
            self.sorter.sort([9, -3, 0, 7, -1]), [-3, -1, 0, 7, 9]
        )

    def test_sort_strings(self):
        self.assertEqual(
            self.sorter.sort(["banana", "apple", "cherry"]),
            ["apple", "banana", "cherry"],
        )

    def test_sort_returns_same_reference(self):
        """验证 sort 返回的是同一个列表引用（原地排序）"""
        items = [3, 1, 2]
        result = self.sorter.sort(items)
        self.assertIs(result, items)

    def test_protocol_compatibility(self):
        """验证 BubbleSorter 符合 SortInterface"""
        sorter: sorting._interface.SortInterface = self.sorter
        self.assertIsNotNone(sorter)


if __name__ == "__main__":
    unittest.main()