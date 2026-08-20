#!/usr/bin/env python3
"""
Bubble Sort Algorithm Implementation

冒泡排序（Bubble Sort）是一种简单的排序算法。
它重复地遍历要排序的列表，比较相邻元素并交换顺序错误的元素，
直到列表排序完成。

时间复杂度：O(n²) 最坏/平均，O(n) 最优（已排序列表 + 优化版本）
空间复杂度：O(1)
"""

from typing import List, TypeVar

T = TypeVar('T')


def bubble_sort(arr: List[T]) -> List[T]:
    """
    标准冒泡排序
    
    每次遍历将最大的元素"冒泡"到数组末尾。
    
    Args:
        arr: 待排序的列表
        
    Returns:
        排序后的列表（原地排序，同时返回原列表引用）
        
    Examples:
        >>> bubble_sort([5, 3, 8, 4, 2])
        [2, 3, 4, 5, 8]
        >>> bubble_sort([1, 2, 3])
        [1, 2, 3]
        >>> bubble_sort([])
        []
    """
    n = len(arr)
    for i in range(n):
        # 每次遍历后，最后 i 个元素已经排好序
        for j in range(0, n - i - 1):
            if arr[j] > arr[j + 1]:
                # 交换相邻元素
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
    return arr


def bubble_sort_optimized(arr: List[T]) -> List[T]:
    """
    优化版冒泡排序
    
    通过标志位检测是否发生了交换，如果某次遍历没有发生交换，
    说明列表已经有序，可以提前终止。
    
    Args:
        arr: 待排序的列表
        
    Returns:
        排序后的列表（原地排序）
        
    Examples:
        >>> bubble_sort_optimized([5, 1, 4, 2, 8])
        [1, 2, 4, 5, 8]
        >>> bubble_sort_optimized([1, 2, 3, 4, 5])
        [1, 2, 3, 4, 5]
    """
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        # 如果没有发生交换，列表已有序，提前退出
        if not swapped:
            break
    return arr


def bubble_sort_descending(arr: List[T]) -> List[T]:
    """
    降序冒泡排序
    
    Args:
        arr: 待排序的列表
        
    Returns:
        降序排序后的列表
        
    Examples:
        >>> bubble_sort_descending([3, 1, 4, 1, 5])
        [5, 4, 3, 1, 1]
    """
    n = len(arr)
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if arr[j] < arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
    return arr


# ==================== 测试用例 ====================

if __name__ == "__main__":
    import doctest
    
    # 运行文档测试
    print("运行 doctest...")
    doctest.testmod()
    
    # 额外测试用例
    test_cases = [
        ([5, 3, 8, 4, 2], [2, 3, 4, 5, 8]),
        ([1, 2, 3, 4, 5], [1, 2, 3, 4, 5]),
        ([5, 4, 3, 2, 1], [1, 2, 3, 4, 5]),
        ([], []),
        ([42], [42]),
        ([3, 1, 2], [1, 2, 3]),
        ([2, 2, 2, 2], [2, 2, 2, 2]),
        ([9, -3, 0, 7, -1], [-3, -1, 0, 7, 9]),
    ]
    
    all_passed = True
    for original, expected in test_cases:
        # 测试标准版
        result = bubble_sort(original.copy())
        if result != expected:
            print(f"FAIL [标准]: {original} -> {result}, expected {expected}")
            all_passed = False
        
        # 测试优化版
        result_opt = bubble_sort_optimized(original.copy())
        if result_opt != expected:
            print(f"FAIL [优化]: {original} -> {result_opt}, expected {expected}")
            all_passed = False
    
    if all_passed:
        print("所有测试用例通过！")
    else:
        print("部分测试用例失败！")
        exit(1)