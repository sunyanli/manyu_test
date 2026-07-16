"""
QuickSort 快速排序算法实现

支持原地排序（in-place），使用 Lomuto 分区方案。
时间复杂度：平均 O(n log n)，最坏 O(n²)
空间复杂度：O(log n) 递归栈
"""

from typing import List, Optional, Callable


def partition(arr: List[int], low: int, high: int, reverse: bool = False) -> int:
    """
    Lomuto 分区：选取最右元素为 pivot，将数组分为小于/大于 pivot 的两部分。
    
    Args:
        arr: 待分区数组
        low: 子数组左边界索引
        high: 子数组右边界索引
        reverse: 是否降序排序
    
    Returns:
        pivot 最终位置的索引
    """
    pivot = arr[high]
    i = low - 1  # 较小（或较大）元素的边界

    for j in range(low, high):
        if reverse:
            # 降序：arr[j] >= pivot 时放在左侧
            should_swap = arr[j] >= pivot
        else:
            # 升序：arr[j] <= pivot 时放在左侧
            should_swap = arr[j] <= pivot

        if should_swap:
            i += 1
            arr[i], arr[j] = arr[j], arr[i]

    # 将 pivot 放到正确位置
    arr[i + 1], arr[high] = arr[high], arr[i + 1]
    return i + 1


def _quicksort(arr: List[int], low: int, high: int, reverse: bool = False) -> None:
    """递归快速排序内部实现。"""
    if low < high:
        pi = partition(arr, low, high, reverse)
        _quicksort(arr, low, pi - 1, reverse)
        _quicksort(arr, pi + 1, high, reverse)


def quicksort(arr: List[int], reverse: bool = False) -> List[int]:
    """
    对整数列表进行快速排序（原地排序）。
    
    Args:
        arr: 待排序的整数列表
        reverse: 默认 False 为升序；True 为降序
    
    Returns:
        排序后的列表（原地修改，同时返回原列表引用以便链式调用）
    
    Examples:
        >>> quicksort([3, 1, 4, 1, 5, 9, 2, 6])
        [1, 1, 2, 3, 4, 5, 6, 9]
        >>> quicksort([3, 1, 4, 1, 5], reverse=True)
        [5, 4, 3, 1, 1]
        >>> quicksort([])
        []
        >>> quicksort([42])
        [42]
    """
    if arr is None:
        raise ValueError("输入数组不能为 None")
    _quicksort(arr, 0, len(arr) - 1, reverse)
    return arr


def quicksort_copy(arr: List[int], reverse: bool = False) -> List[int]:
    """
    对整数列表进行快速排序（非原地，返回新列表）。
    
    Args:
        arr: 待排序的整数列表
        reverse: 默认 False 为升序；True 为降序
    
    Returns:
        排序后的新列表，原列表保持不变
    """
    if arr is None:
        raise ValueError("输入数组不能为 None")
    result = arr.copy()
    _quicksort(result, 0, len(result) - 1, reverse)
    return result