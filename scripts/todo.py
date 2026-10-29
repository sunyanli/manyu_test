"""TODO 待办事项管理模块 — 仅支持新增操作（最小闭环）."""
import json
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional


@dataclass
class TodoItem:
    """一条待办事项."""

    name: str
    description: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat()
    )

    def to_dict(self) -> dict:
        """序列化为字典."""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TodoItem":
        """从字典反序列化."""
        return cls(
            id=data["id"],
            name=data["name"],
            description=data["description"],
            created_at=data["created_at"],
        )

DEFAULT_STORAGE_PATH = Path("data/todos.json")


class TodoManager:
    """管理待办事项的持久化与操作."""

    def __init__(self, file_path: Optional[Path] = None) -> None:
        self._file_path = file_path or DEFAULT_STORAGE_PATH

    def _load(self) -> list[dict]:
        """从 JSON 文件加载所有事项."""
        if not self._file_path.exists():
            return []
        with open(self._file_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _save(self, items: list[dict]) -> None:
        """将所有事项写入 JSON 文件."""
        self._file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self._file_path, "w", encoding="utf-8") as f:
            json.dump(items, f, ensure_ascii=False, indent=2)

    def add(self, name: str, description: str) -> TodoItem:
        """新增一条待办事项并持久化.

        Args:
            name: 事项名称（必填）.
            description: 事项描述（必填）.

        Returns:
            创建的 TodoItem 实例.
        """
        item = TodoItem(name=name, description=description)
        current = self._load()
        current.append(item.to_dict())
        self._save(current)
        return item
