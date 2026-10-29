#!/usr/bin/env python3
"""
Quick Sort Algorithm Implementation（快速排序）

对同类型可比较元素执行原地升序快速排序，返回排序后的同一列表引用。

实现要点：
- 枢轴选择：三数取中（首/中/尾），规避已序/近序/反向有序输入的最坏 O(n²) 退化
- 分区：Lomuto 分区，枢轴归位后递归左右子区
- 小规模优化：子区长度 <= INSERTION_THRESHOLD 时切换插入排序，降低递归开销

复杂度：
- 平均 O(n log n)，最坏 O(n²)
- 空间 O(log n) 递归栈
- 不稳定排序（相等元素相对顺序不保证）

纯内存计算：无外部依赖、无持久化、无 I/O。

@author AiWork
@date 2026/10/29
"""

from typing import List, TypeVar

T = TypeVar("T")

# 小规模子区切换插入排序的阈值（降低递归开销）
INSERTION_THRESHOLD = 10


def quick_sort(data: List[T]) -> List[T]:
    """
    快速排序（默认升序）：三数取中选枢轴 + Lomuto 分区递归，小规模切插入排序。

    Args:
        data: 待排序列表，元素须相互可比较（支持 `<`）

    Returns:
        原地排序后的同一列表引用

    Examples:
        >>> quick_sort([5, 3, 8, 4, 2])
        [2, 3, 4, 5, 8]
        >>> quick_sort([])
        []
    """
    if len(data) < 2:
        return data
    _quick_sort_range(data, 0, len(data) - 1)
    return data


def _quick_sort_range(data: List[T], low: int, high: int) -> None:
    """对 data[low..high] 区间递归快速排序。"""
    # 小规模子区切换插入排序，降低递归开销
    if high - low + 1 <= INSERTION_THRESHOLD:
        _insertion_sort(data, low, high)
        return
    if low < high:
        pivot_index = _partition(data, low, high)
        _quick_sort_range(data, low, pivot_index - 1)
        _quick_sort_range(data, pivot_index + 1, high)


def _median_of_three(data: List[T], low: int, high: int) -> None:
    """将首/中/尾三个位置排序，使中位数落在 mid 位置，规避枢轴极端取值。"""
    mid = (low + high) // 2
    if data[mid] < data[low]:
        data[low], data[mid] = data[mid], data[low]
    if data[high] < data[low]:
        data[low], data[high] = data[high], data[low]
    if data[high] < data[mid]:
        data[mid], data[high] = data[high], data[mid]


def _partition(data: List[T], low: int, high: int) -> int:
    """Lomuto 分区：三数取中选枢轴，返回枢轴最终归位下标。"""
    _median_of_three(data, low, high)
    mid = (low + high) // 2
    pivot = data[mid]
    # 将枢轴移到区间末尾，随后对 [low, high-1] 完整分区
    data[mid], data[high] = data[high], data[mid]
    i = low
    for j in range(low, high):
        if data[j] < pivot:
            data[i], data[j] = data[j], data[i]
            i += 1
    # 将枢轴归位到 i 处
    data[i], data[high] = data[high], data[i]
    return i


def _insertion_sort(data: List[T], low: int, high: int) -> None:
    """对 data[low..high] 区间执行插入排序（小规模子区优化，减少递归栈开销）。"""
    for i in range(low + 1, high + 1):
        key = data[i]
        j = i - 1
        while j >= low and key < data[j]:
            data[j + 1] = data[j]
            j -= 1
        data[j + 1] = key