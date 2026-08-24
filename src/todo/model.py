# src/todo/model.py
from dataclasses import dataclass, field
from datetime import datetime, timezone
import uuid


@dataclass
class TodoItem:
    name: str
    description: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TodoItem":
        return cls(
            name=data["name"],
            description=data["description"],
            id=data["id"],
            created_at=data["created_at"],
        )
