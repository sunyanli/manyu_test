"""Repository 层：内存存储 TodoItemRepository

使用线程安全字典 + 自增 ID 锁，对应系分方案 §5.1.3.1 并发控制策略。
"""

import threading
from typing import Optional

from todo_app.model.todo_item_do import TodoItemDO


class TodoItemRepository:
    """待办事项内存仓储。

    线程安全：使用单锁保护自增 ID 与字典写入。
    """

    def __init__(self) -> None:
        self._lock: threading.Lock = threading.Lock()
        self._store: dict[int, TodoItemDO] = {}
        self._next_id: int = 1

    def save(self, item: TodoItemDO) -> TodoItemDO:
        """保存待办事项，返回已分配 id 的对象。

        Args:
            item: 待保存对象，其 id 字段会被覆盖为自增值。

        Returns:
            带自增 id 的 TodoItemDO。
        """
        with self._lock:
            item.id = self._next_id
            self._store[item.id] = item
            self._next_id += 1
            return item

    def find_by_id(self, item_id: int) -> Optional[TodoItemDO]:
        """按 id 查询（预留扩展，本期未对外暴露）。"""
        with self._lock:
            return self._store.get(item_id)
