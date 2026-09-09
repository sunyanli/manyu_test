"""Request DTO for creating a new todo item."""

from dataclasses import dataclass
from typing import Optional


@dataclass
class TodoRequest:
    """新增待办事项请求参数。"""

    title: str
    description: Optional[str] = None