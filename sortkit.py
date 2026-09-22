#!/usr/bin/env python3
"""
sortkit 排序算法工具库

提供对同类型可比较元素的通用排序能力，支持原地排序与返回有序结果。

- 统一入口：sort(data, algorithm='quick', reverse=False)
- 算法实现：冒泡（标准/优化）、快速（默认，三数取中 + 小规模切插入排序）、归并（稳定）
- 异常体系：SortError -> InvalidInputError / NotComparableError

纯内存计算：无外部依赖、无持久化、无 I/O。

@author AiWork
@date 2026/09/22
"""

from functools import total_ordering
from typing import List, TypeVar

T = TypeVar("T")

# 算法名注册表：统一入口接受的有效取值（顺序即文档顺序）
ALGORITHMS = ["bubble", "bubble_optimized", "quick", "merge"]

# 快速排序小规模子区切换插入排序的阈值（降低递归开销）
INSERTION_THRESHOLD = 10


class SortError(Exception):
    """排序工具基础异常。"""


class InvalidInputError(SortError):
    """非法入参异常：data 非可变序列、算法名不在注册表等。"""


class NotComparableError(SortError):
    """元素不可比较异常：元素间比较运算抛出 TypeError 时包装。"""


@total_ordering
class _Descending:
    """逆序比较包装器：通过取反比较实现降序排序。"""

    __slots__ = ("value",)

    def __init__(self, value: T) -> None:
        self.value = value

    def __lt__(self, other: "_Descending") -> bool:
        return self.value > other.value

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, _Descending):
            return NotImplemented
        return self.value == other.value


def _validate_sequence(data) -> None:
    """校验入参为可变序列（list）。"""
    if data is None:
        raise InvalidInputError("入参不能为空")
    if not isinstance(data, list):
        raise InvalidInputError("入参必须是可变序列（list）")


def bubble_sort(data: List[T]) -> List[T]:
    """
    标准冒泡排序：每轮将未排序区最大元素冒泡至末尾。

    Args:
        data: 待排序列表

    Returns:
        原地排序后的同一列表引用
    """
    n = len(data)
    for i in range(n):
        # 已排序区位于末尾 i 个位置
        for j in range(0, n - i - 1):
            if data[j + 1] < data[j]:
                data[j], data[j + 1] = data[j + 1], data[j]
    return data


def bubble_sort_optimized(data: List[T]) -> List[T]:
    """
    优化冒泡排序：某轮无交换说明已有序，提前终止。

    Args:
        data: 待排序列表

    Returns:
        原地排序后的同一列表引用
    """
    n = len(data)
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if data[j + 1] < data[j]:
                data[j], data[j + 1] = data[j + 1], data[j]
                swapped = True
        # 本轮无交换，列表已有序，提前退出
        if not swapped:
            break
    return data


def quick_sort(data: List[T]) -> List[T]:
    """
    快速排序（默认算法）：三数取中选枢轴 + 分区递归，小规模切插入排序。

    平均 O(n log n)，空间 O(log n) 栈，不稳定。

    Args:
        data: 待排序列表

    Returns:
        原地排序后的同一列表引用
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


def _partition(data: List[T], low: int, high: int) -> int:
    """Lomuto 分区，三数取中（首/中/尾）选枢轴，返回枢轴最终位置。"""
    mid = (low + high) // 2
    # 三数取中：将首/中/尾排序，使中位数（data[mid]）为枢轴，规避已序/近序退化
    if data[mid] < data[low]:
        data[low], data[mid] = data[mid], data[low]
    if data[high] < data[low]:
        data[low], data[high] = data[high], data[low]
    if data[high] < data[mid]:
        data[mid], data[high] = data[high], data[mid]
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
    """对 data[low..high] 区间执行插入排序（小规模优化）。"""
    for i in range(low + 1, high + 1):
        key = data[i]
        j = i - 1
        while j >= low and key < data[j]:
            data[j + 1] = data[j]
            j -= 1
        data[j + 1] = key


def merge_sort(data: List[T]) -> List[T]:
    """
    归并排序（稳定）：二分递归 + 借助辅助数组合并。

    O(n log n)，空间 O(n)，稳定。

    Args:
        data: 待排序列表

    Returns:
        原地排序后的同一列表引用
    """
    if len(data) < 2:
        return data
    _merge_sort_range(data, 0, len(data) - 1)
    return data


def _merge_sort_range(data: List[T], low: int, high: int) -> None:
    """对 data[low..high] 区间递归归并排序。"""
    if low >= high:
        return
    mid = (low + high) // 2
    _merge_sort_range(data, low, mid)
    _merge_sort_range(data, mid + 1, high)
    _merge(data, low, mid, high)


def _merge(data: List[T], low: int, mid: int, high: int) -> None:
    """合并两个有序区间 [low, mid] 与 [mid+1, high]，左区元素优先保证稳定。"""
    left = data[low:mid + 1]
    right = data[mid + 1:high + 1]
    i = j = 0
    k = low
    while i < len(left) and j < len(right):
        # 仅当右区严格更小时取右区，相等取左区保持原相对顺序（稳定）
        if right[j] < left[i]:
            data[k] = right[j]
            j += 1
        else:
            data[k] = left[i]
            i += 1
        k += 1
    while i < len(left):
        data[k] = left[i]
        i += 1
        k += 1
    while j < len(right):
        data[k] = right[j]
        j += 1
        k += 1


# 算法名 -> 实现函数 的注册表（算法选择器）
_ALGORITHM_IMPL = {
    "bubble": bubble_sort,
    "bubble_optimized": bubble_sort_optimized,
    "quick": quick_sort,
    "merge": merge_sort,
}


def sort(data: List[T], algorithm: str = "quick", reverse: bool = False) -> List[T]:
    """
    统一排序入口：校验入参、解析算法、分发执行并返回结果。

    Args:
        data: 待排序的可变序列（list），原地排序
        algorithm: 算法名，取值 bubble / bubble_optimized / quick / merge，默认 quick
        reverse: 是否降序，默认 False（升序）

    Returns:
        排序后的同一序列（原地排序，同时返回引用）

    Raises:
        InvalidInputError: data 非可变序列或算法名不在注册表
        NotComparableError: 元素间不可比较
    """
    _validate_sequence(data)
    if algorithm not in ALGORITHMS:
        raise InvalidInputError(
            f"未知算法名：{algorithm}，合法取值：{ALGORITHMS}"
        )
    try:
        if reverse:
            # 逆序比较包装：包装每个元素使比较取反，排序后解包写回原序列
            wrapped = [_Descending(v) for v in data]
            _ALGORITHM_IMPL[algorithm](wrapped)
            for i, item in enumerate(wrapped):
                data[i] = item.value
        else:
            _ALGORITHM_IMPL[algorithm](data)
    except TypeError as exc:
        # 元素不可比较（如 int 与 str 混排），包装为 NotComparableError
        raise NotComparableError("元素间不可比较") from exc
    return data