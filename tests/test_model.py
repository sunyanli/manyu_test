# tests/test_model.py
import pytest
from src.todo.model import TodoItem


def test_todo_item_creation():
    item = TodoItem(name="Buy groceries", description="Milk, eggs, bread")
    assert item.name == "Buy groceries"
    assert item.description == "Milk, eggs, bread"
    assert item.id is not None
    assert len(item.id) == 36  # UUID format
    assert item.created_at is not None


def test_todo_item_unique_ids():
    item1 = TodoItem(name="Task 1", description="Desc 1")
    item2 = TodoItem(name="Task 2", description="Desc 2")
    assert item1.id != item2.id


def test_todo_item_to_dict():
    item = TodoItem(name="Test", description="Test desc")
    d = item.to_dict()
    assert d["name"] == "Test"
    assert d["description"] == "Test desc"
    assert "id" in d
    assert "created_at" in d
