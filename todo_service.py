"""待办事项服务模块

提供 TodoItem 数据类和 TodoService 业务服务。
支持创建和查询待办事项（名称+描述）。
"""
import uuid
from datetime import datetime
from typing import List, Optional


class TodoItem:
    """待办事项数据对象"""

    def __init__(self, name: str, description: str = ""):
        """
        初始化待办事项

        Args:
            name: 事项名称，不能为空
            description: 事项描述，可选
        """
        self.id: str = str(uuid.uuid4())
        self.name: str = name
        self.description: str = description
        self.completed: bool = False
        self.created_at: datetime = datetime.now()

    def mark_completed(self):
        """标记为已完成"""
        self.completed = True

    def __str__(self) -> str:
        status = "已完成" if self.completed else "未完成"
        return f"[{status}] {self.name}"


class TodoService:
    """待办事项业务服务"""

    def __init__(self):
        self._items: List[TodoItem] = []

    def add_todo(self, name: str, description: str = "") -> TodoItem:
        """
        新增待办事项

        Args:
            name: 事项名称，不能为空或空白
            description: 事项描述，可选

        Returns:
            创建的 TodoItem 实例

        Raises:
            ValueError: 名称为空或仅含空白字符时抛出
        """
        if not name or not name.strip():
            raise ValueError("事项名称不能为空")
        item = TodoItem(name.strip(), description)
        self._items.append(item)
        return item

    def list_all(self) -> List[TodoItem]:
        """获取所有待办事项列表"""
        return list(self._items)

    def get_by_id(self, item_id: str) -> Optional[TodoItem]:
        """
        根据ID查找待办事项

        Args:
            item_id: 事项ID

        Returns:
            匹配的 TodoItem，未找到返回 None
        """
        for item in self._items:
            if item.id == item_id:
                return item
        return None