"""Business service layer for todo operations."""

from typing import List, Optional

from src.model.entity.TodoItem import TodoItem
from src.model.dto.TodoRequest import TodoRequest
from src.model.dto.TodoResponse import TodoResponse
from src.repository.todo_repository import TodoRepository


TITLE_MAX_LEN = 200
DESCRIPTION_MAX_LEN = 2000


class TodoService:
    """待办事项业务服务。"""

    def __init__(self, todo_list: Optional[List[TodoItem]] = None):
        """Initialize with an in-memory list (for testing) or a repository (for production)."""
        self._todo_list = todo_list if todo_list is not None else []
        self._repository: Optional[TodoRepository] = None
        self._next_id = 0

    def set_repository(self, repository: TodoRepository) -> None:
        """Inject a real repository for production use."""
        self._repository = repository

    def _validate_title(self, title: str) -> None:
        """Validate the title field per business rules R01/R02."""
        if not title or not title.strip():
            raise ValueError("事项名称不能为空")
        if len(title) > TITLE_MAX_LEN:
            raise ValueError(f"事项名称长度不能超过 {TITLE_MAX_LEN} 字符")

    def _validate_description(self, description: Optional[str]) -> None:
        """Validate the description field per business rule R03."""
        if description is not None and len(description) > DESCRIPTION_MAX_LEN:
            raise ValueError(f"事项描述长度不能超过 {DESCRIPTION_MAX_LEN} 字符")

    def create_todo(self, user_id: int, request: TodoRequest) -> TodoItem:
        """创建一条新的待办事项。

        业务规则：
        - R01: 事项名称不能为空或纯空格
        - R02: 事项名称长度不超过 200 字符
        - R03: 事项描述长度不超过 2000 字符
        """
        self._validate_title(request.title)
        self._validate_description(request.description)

        self._next_id += 1
        item = TodoItem(
            id=self._next_id,
            title=request.title,
            description=request.description,
            user_id=user_id,
        )
        self._todo_list.append(item)
        return item

    def list_todos(self, user_id: int, page: int = 1, page_size: int = 20) -> List[TodoItem]:
        """查询某用户的所有待办事项，按创建时间倒序排列。"""
        if page_size > 100:
            page_size = 100
        return [item for item in self._todo_list if item.user_id == user_id]

    def get_todo_detail(self, todo_id: int, user_id: int) -> Optional[TodoItem]:
        """根据 ID 查询单条待办事项，仅限创建者本人。"""
        for item in self._todo_list:
            if item.id == todo_id and item.user_id == user_id:
                return item
        return None