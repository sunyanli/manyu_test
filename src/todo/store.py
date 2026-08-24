# src/todo/store.py
import json
import os
from src.todo.model import TodoItem


class TodoStore:
    def __init__(self, filepath: str = "todos.json"):
        self.filepath = filepath

    def save(self, items: list) -> None:
        data = [item.to_dict() for item in items]
        with open(self.filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def load(self) -> list:
        if not os.path.exists(self.filepath):
            return []
        with open(self.filepath, "r", encoding="utf-8") as f:
            content = f.read().strip()
        if not content:
            return []
        data = json.loads(content)
        return [TodoItem.from_dict(item) for item in data]
