"""Repository layer for TodoItem CRUD operations."""

from typing import List, Optional

from sqlalchemy.orm import Session

from src.models import TodoItem


class TodoRepository:
    """数据访问层 — 待办事项。"""

    def __init__(self, db: Session):
        self._db = db

    def create(self, title: str, description: Optional[str], user_id: int) -> TodoItem:
        """创建一条新的待办事项并持久化。"""
        item = TodoItem(title=title, description=description, user_id=user_id)
        self._db.add(item)
        self._db.flush()
        self._db.refresh(item)
        return item

    def list_by_user(self, user_id: int, page: int = 1, page_size: int = 20) -> List[TodoItem]:
        """分页查询某用户的所有待办事项，按创建时间倒序。"""
        offset = (page - 1) * page_size
        return (
            self._db.query(TodoItem)
            .filter(TodoItem.user_id == user_id)
            .order_by(TodoItem.gmt_create.desc())
            .offset(offset)
            .limit(page_size)
            .all()
        )

    def get_by_id(self, todo_id: int, user_id: int) -> Optional[TodoItem]:
        """根据 ID 和 user_id 查询单条待办事项。"""
        return (
            self._db.query(TodoItem)
            .filter(TodoItem.id == todo_id, TodoItem.user_id == user_id)
            .first()
        )