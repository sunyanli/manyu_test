"""堆排序（Heap Sort）实现。

原地排序算法，基于最大堆，时间复杂度 O(n log n)，空间复杂度 O(1)。
"""

from typing import TypeVar, List

T = TypeVar("T")


def heapify(arr: List[T], n: int, i: int) -> None:
    """维护最大堆性质：确保以 i 为根的子树满足最大堆条件。

    Args:
        arr: 待调整的数组
        n:   堆的有效大小
        i:   当前根节点索引
    """
    largest = i          # 假设当前根最大
    left = 2 * i + 1     # 左子节点
    right = 2 * i + 2    # 右子节点

    # 如果左子节点存在且大于当前最大
    if left < n and arr[left] > arr[largest]:
        largest = left

    # 如果右子节点存在且大于当前最大
    if right < n and arr[right] > arr[largest]:
        largest = right

    # 如果最大值不是根，交换并递归调整
    if largest != i:
        arr[i], arr[largest] = arr[largest], arr[i]
        heapify(arr, n, largest)


def heap_sort(arr: List[T]) -> List[T]:
    """堆排序：原地排序，升序返回。

    Args:
        arr: 待排序的数组（原地修改）

    Returns:
        排序后的数组（与输入同一个引用）
    """
    n = len(arr)
    if n <= 1:
        return arr

    # 阶段一：构建最大堆（从最后一个非叶节点开始）
    for i in range(n // 2 - 1, -1, -1):
        heapify(arr, n, i)

    # 阶段二：逐个提取最大元素
    for i in range(n - 1, 0, -1):
        # 将当前堆顶（最大值）交换到数组末尾
        arr[0], arr[i] = arr[i], arr[0]
        # 对缩小后的堆重新 heapify
        heapify(arr, i, 0)

    return arr