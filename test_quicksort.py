"""
QuickSort 算法单元测试

覆盖：空数组、单元素、已排序、逆序、重复元素、随机序列、降序模式、边界条件。
"""

import unittest
from quicksort import quicksort, quicksort_copy, partition


class TestPartition(unittest.TestCase):
    """测试分区函数。"""

    def test_partition_basic(self):
        arr = [3, 1, 4, 1, 5]
        pi = partition(arr, 0, len(arr) - 1)
        # pivot=5 在末尾，分区后 pivot 应在正确位置
        self.assertEqual(arr[pi], 5)
        for i in range(pi):
            self.assertLessEqual(arr[i], 5)
        for i in range(pi + 1, len(arr)):
            self.assertGreaterEqual(arr[i], 5)

    def test_partition_reverse(self):
        arr = [3, 1, 4, 1, 5]
        pi = partition(arr, 0, len(arr) - 1, reverse=True)
        self.assertEqual(arr[pi], 5)
        for i in range(pi):
            self.assertGreaterEqual(arr[i], 5)
        for i in range(pi + 1, len(arr)):
            self.assertLessEqual(arr[i], 5)


class TestQuicksort(unittest.TestCase):
    """测试 quicksort 原地排序函数。"""

    def test_empty(self):
        arr = []
        result = quicksort(arr)
        self.assertEqual(result, [])
        self.assertIs(arr, result)  # 原地排序，返回同一引用

    def test_single(self):
        arr = [42]
        result = quicksort(arr)
        self.assertEqual(result, [42])

    def test_sorted_ascending(self):
        arr = [1, 2, 3, 4, 5]
        result = quicksort(arr)
        self.assertEqual(result, [1, 2, 3, 4, 5])

    def test_sorted_descending_input(self):
        arr = [5, 4, 3, 2, 1]
        result = quicksort(arr)
        self.assertEqual(result, [1, 2, 3, 4, 5])

    def test_duplicates(self):
        arr = [3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5]
        result = quicksort(arr)
        self.assertEqual(result, [1, 1, 2, 3, 3, 4, 5, 5, 5, 6, 9])

    def test_random(self):
        arr = [9, 7, 5, 11, 12, 2, 14, 3, 10, 6]
        result = quicksort(arr)
        expected = sorted(arr)
        self.assertEqual(result, expected)

    def test_negative_numbers(self):
        arr = [-3, 1, -4, 1, -5, 9, -2, 6]
        result = quicksort(arr)
        self.assertEqual(result, [-5, -4, -3, -2, 1, 1, 6, 9])

    def test_reverse_sort(self):
        arr = [3, 1, 4, 1, 5, 9, 2, 6]
        result = quicksort(arr, reverse=True)
        self.assertEqual(result, [9, 6, 5, 4, 3, 2, 1, 1])

    def test_all_same(self):
        arr = [7, 7, 7, 7, 7]
        result = quicksort(arr)
        self.assertEqual(result, [7, 7, 7, 7, 7])

    def test_two_elements(self):
        arr = [2, 1]
        result = quicksort(arr)
        self.assertEqual(result, [1, 2])

    def test_two_elements_sorted(self):
        arr = [1, 2]
        result = quicksort(arr)
        self.assertEqual(result, [1, 2])

    def test_large_array(self):
        import random
        random.seed(42)
        arr = [random.randint(-1000, 1000) for _ in range(1000)]
        result = quicksort(arr)
        expected = sorted(arr)
        self.assertEqual(result, expected)

    def test_quicksort_copy(self):
        original = [3, 1, 4, 1, 5]
        result = quicksort_copy(original)
        self.assertEqual(result, [1, 1, 3, 4, 5])
        self.assertEqual(original, [3, 1, 4, 1, 5])  # 原数组不变

    def test_quicksort_copy_reverse(self):
        original = [3, 1, 4, 1, 5]
        result = quicksort_copy(original, reverse=True)
        self.assertEqual(result, [5, 4, 3, 1, 1])
        self.assertEqual(original, [3, 1, 4, 1, 5])

    def test_none_input(self):
        with self.assertRaises(ValueError):
            quicksort(None)
        with self.assertRaises(ValueError):
            quicksort_copy(None)


if __name__ == "__main__":
    unittest.main()