"""堆排序算法测试用例。"""

import pytest
from heap_sort import heap_sort, heapify


class TestHeapify:
    """测试 heapify 函数 — 维护最大堆性质。"""

    def test_heapify_small_max_heap(self):
        """对一个小的非最大堆子树执行 heapify 后应恢复最大堆性质。"""
        arr = [3, 5, 1]
        heapify(arr, 3, 0)
        assert arr == [5, 3, 1]

    def test_heapify_already_heap(self):
        """已经是最大堆时不应改变。"""
        arr = [10, 7, 8, 3, 1]
        heapify(arr, 5, 1)
        assert arr == [10, 7, 8, 3, 1]

    def test_heapify_leaf(self):
        """对叶节点执行 heapify 不应改变数组。"""
        arr = [1, 3, 2]
        heapify(arr, 3, 2)  # 索引 2 是叶节点
        assert arr == [1, 3, 2]


class TestHeapSort:
    """测试 heap_sort 函数 — 堆排序主流程。"""

    def test_empty_array(self):
        """空数组应返回空数组。"""
        assert heap_sort([]) == []

    def test_single_element(self):
        """单元素数组应保持不变。"""
        assert heap_sort([42]) == [42]

    def test_already_sorted(self):
        """已排序数组应保持不变。"""
        assert heap_sort([1, 2, 3, 4, 5]) == [1, 2, 3, 4, 5]

    def test_reverse_sorted(self):
        """逆序数组应排序为升序。"""
        assert heap_sort([5, 4, 3, 2, 1]) == [1, 2, 3, 4, 5]

    def test_duplicate_elements(self):
        """包含重复元素的数组应正确排序。"""
        assert heap_sort([3, 1, 3, 2, 1, 2]) == [1, 1, 2, 2, 3, 3]

    def test_random_order(self):
        """随机顺序数组应正确排序。"""
        assert heap_sort([4, 10, 3, 5, 1, 8, 7, 2, 9, 6]) == list(range(1, 11))

    def test_negative_numbers(self):
        """包含负数的数组应正确排序。"""
        assert heap_sort([-3, 0, 5, -8, 2, -1]) == [-8, -3, -1, 0, 2, 5]

    def test_in_place_modification(self):
        """heap_sort 应原地修改数组。"""
        arr = [3, 1, 2]
        result = heap_sort(arr)
        assert result is arr
        assert arr == [1, 2, 3]

    def test_string_sorting(self):
        """应支持字符串排序。"""
        assert heap_sort(["c", "a", "b"]) == ["a", "b", "c"]

    def test_float_sorting(self):
        """应支持浮点数排序。"""
        assert heap_sort([3.5, 1.2, 2.8, 1.1]) == [1.1, 1.2, 2.8, 3.5]

    def test_large_array(self):
        """大数据量排序应正确。"""
        import random
        random.seed(42)
        data = [random.randint(0, 10000) for _ in range(500)]
        result = heap_sort(data)
        assert result == sorted(data)

    def test_all_equal(self):
        """所有元素相等时应保持不变。"""
        assert heap_sort([7, 7, 7, 7]) == [7, 7, 7, 7]