"""排序算法接口定义"""

from typing import Protocol, TypeVar

T = TypeVar("T")


class SortInterface(Protocol):
    """排序算法接口：提供排序功能"""

    def sort(self, items: list[T]) -> list[T]:
        """对列表进行原地排序

        Args:
            items: 待排序列表

        Returns:
            排序后的列表（原地排序，同时返回引用）

        Examples:
            >>> BubbleSorter().sort([3, 1, 2])
            [1, 2, 3]
            >>> BubbleSorter().sort([5, 3, 8, 4, 2])
            [2, 3, 4, 5, 8]
        """
        ...

    @property
    def algorithm_name(self) -> str:
        """返回算法名称，如 'bubble_sort', 'quick_sort' 等"""
        ...

    @property
    def time_complexity(self) -> str:
        """返回时间复杂度描述，如 'O(n²)'"""
        ...

    @property
    def space_complexity(self) -> str:
        """返回空间复杂度描述，如 'O(1)'"""
        ...