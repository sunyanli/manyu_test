# Task 3: 创建 sorting 包（接口 + BubbleSorter 类适配）

**Files to create:**
- `sorting/__init__.py`
- `sorting/_interface.py`
- `sorting/bubble_sort.py`（新类，非根目录旧文件）
- `tests/test_sorting.py`

## Requirements

### sorting/__init__.py
```python
from sorting._interface import SortInterface
from sorting.bubble_sort import BubbleSorter

__all__ = ["SortInterface", "BubbleSorter"]
```

### sorting/_interface.py
```python
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
```

### sorting/bubble_sort.py
```python
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
                if items[j] > items[j + 1]:
                    items[j], items[j + 1] = items[j + 1], items[j]
        return items
```

### tests/test_sorting.py
```python
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
```

## Global Constraints
- Python 3.10+ 语法（`list[str]` 等泛型写法）
- 使用 `typing.Protocol`，不使用 ABC
- 所有接口方法必须包含完整的文档字符串和 doctest 示例
- 现有 `bubble_sort.py` 根目录文件不得修改（已在根目录存在）
- 每个包必须包含 `__init__.py`
- 类型标注必须完整，通过 `mypy --strict` 检查