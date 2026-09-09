"""TodoItem entity — domain model for a todo entry."""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class TodoItem:
    """待办事项实体。"""

    id: Optional[int] = None
    title: str = ""
    description: Optional[str] = None
    user_id: int = 0
    gmt_create: datetime = field(default_factory=datetime.utcnow)
    gmt_modified: datetime = field(default_factory=datetime.utcnow)

    def update_modified(self) -> None:
        """Update the gmt_modified timestamp to current time."""
        self.gmt_modified = datetime.utcnow()