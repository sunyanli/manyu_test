# tests/test_service.py
import os
import tempfile
import pytest
from src.todo.store import TodoStore
from src.todo.service import TodoService


@pytest.fixture
def service():
    fd, path = tempfile.mkstemp(suffix=".json")
    os.close(fd)
    store = TodoStore(filepath=path)
    svc = TodoService(store=store)
    yield svc
    if os.path.exists(path):
        os.remove(path)


def test_add_todo(service):
    item = service.add_todo("Buy groceries", "Milk, eggs, bread")
    assert item.name == "Buy groceries"
    assert item.description == "Milk, eggs, bread"
    assert item.id is not None


def test_add_todo_persisted(service):
    service.add_todo("Task 1", "Description 1")
    service.add_todo("Task 2", "Description 2")
    all_items = service.list_todos()
    assert len(all_items) == 2
    assert all_items[0].name == "Task 1"
    assert all_items[1].name == "Task 2"


def test_add_todo_empty_name_raises(service):
    with pytest.raises(ValueError, match="name"):
        service.add_todo("", "some description")
