# tests/test_store.py
import os
import tempfile
import pytest
from src.todo.model import TodoItem
from src.todo.store import TodoStore


@pytest.fixture
def temp_file():
    fd, path = tempfile.mkstemp(suffix=".json")
    os.close(fd)
    yield path
    if os.path.exists(path):
        os.remove(path)


def test_save_and_load(temp_file):
    store = TodoStore(filepath=temp_file)
    items = [
        TodoItem(name="Task 1", description="Desc 1"),
        TodoItem(name="Task 2", description="Desc 2"),
    ]
    store.save(items)
    loaded = store.load()
    assert len(loaded) == 2
    assert loaded[0].name == "Task 1"
    assert loaded[1].name == "Task 2"


def test_load_empty_file(temp_file):
    store = TodoStore(filepath=temp_file)
    loaded = store.load()
    assert loaded == []


def test_load_nonexistent_file():
    store = TodoStore(filepath="/tmp/nonexistent_todo_test.json")
    loaded = store.load()
    assert loaded == []
