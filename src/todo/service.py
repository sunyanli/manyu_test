# src/todo/service.py
from src.todo.model import TodoItem
from src.todo.store import TodoStore


class TodoService:
    def __init__(self, store: TodoStore):
        self.store = store

    def add_todo(self, name: str, description: str) -> TodoItem:
        if not name or not name.strip():
            raise ValueError("Todo item name cannot be empty")
        item = TodoItem(name=name.strip(), description=description.strip())
        items = self.store.load()
        items.append(item)
        self.store.save(items)
        return item

    def list_todos(self) -> list:
        return self.store.load()
