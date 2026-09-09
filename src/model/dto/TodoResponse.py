"""Response DTO for todo item operations."""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class TodoResponse:
    """待办事项响应数据。"""

    id: int
    title: str
    description: Optional[str]
    user_id: int
    gmt_create: str
    gmt_modified: str

    @classmethod
    def from_entity(cls, item) -> "TodoResponse":
        """Convert a TodoItem entity to a TodoResponse DTO."""
        return cls(
            id=item.id,
            title=item.title,
            description=item.description,
            user_id=item.user_id,
            gmt_create=item.gmt_create.strftime("%Y-%m-%d %H:%M:%S"),
            gmt_modified=item.gmt_modified.strftime("%Y-%m-%d %H:%M:%S"),
        )