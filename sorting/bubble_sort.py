"""冒泡排序实现（面向接口版本）"""

from typing import TypeVar

T = TypeVar("T")


class BubbleSorter:
    """冒泡排序实现，适配 SortInterface

    标准冒泡排序算法，每次遍历将最大的元素"冒泡"到数组末尾。

    Examples:
        >>> sorter = BubbleSorter()
        >>> sorter.algorithm_name
        'bubble_sort'
        >>> sorter.time_complexity
        'O(n²)'
        >>> sorter.space_complexity
        'O(1)'
        >>> sorter.sort([3, 1, 2])
        [1, 2, 3]
        >>> sorter.sort([5, 3, 8, 4, 2])
        [2, 3, 4, 5, 8]
        >>> sorter.sort([])
        []
        >>> sorter.sort([42])
        [42]
    """

    @property
    def algorithm_name(self) -> str:
        return "bubble_sort"

    @property
    def time_complexity(self) -> str:
        return "O(n²)"

    @property
    def space_complexity(self) -> str:
        return "O(1)"

    def sort(self, items: list[T]) -> list[T]:
        n = len(items)
        for i in range(n):
            for j in range(0, n - i - 1):
                if items[j] > items[j + 1]:  # type: ignore[operator]
                    items[j], items[j + 1] = items[j + 1], items[j]
        return items