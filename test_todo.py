#!/usr/bin/env python3
"""Tests for the daily to-do CLI tool."""

import json
import os
import subprocess
import sys
import tempfile


def _make_runner(tmpdir):
    """Return a (_run, todos_path) pair backed by an isolated temp directory.

    _run(*args) invokes ``todo.py`` with the given CLI arguments and
    returns a ``subprocess.CompletedProcess``.
    """
    todos_path = os.path.join(tmpdir, "todos.json")

    def _run(*args):
        env = os.environ.copy()
        env["TODOS_PATH"] = todos_path
        return subprocess.run(
            [sys.executable, "todo.py"] + list(args),
            capture_output=True,
            text=True,
            env=env,
        )

    return _run, todos_path


def test_add_creates_item():
    """Adding a todo item should succeed and persist to todos.json."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        result = _run("add", "Buy groceries", "--user", "张三")
        assert result.returncode == 0, f"add failed: {result.stderr}"

        assert os.path.exists(todos_path), "todos.json not created"
        with open(todos_path, "r", encoding="utf-8") as f:
            todos = json.load(f)
        assert len(todos) == 1
        assert todos[0]["name"] == "Buy groceries"
        assert todos[0]["user"] == "张三"


def test_list_displays_items():
    """Listing should display all persisted todo items."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        _run("add", "Buy groceries", "--user", "张三")
        _run("add", "Read book", "--user", "李四")

        result = _run("list")
        assert result.returncode == 0
        output = result.stdout
        assert "Buy groceries" in output
        assert "张三" in output
        assert "Read book" in output
        assert "李四" in output


def test_duplicate_rejected():
    """Adding an item with the same name and user should be rejected."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        _run("add", "Buy groceries", "--user", "张三")
        result = _run("add", "Buy groceries", "--user", "张三")
        assert result.returncode != 0, "duplicate should fail"
        assert "already exists" in result.stderr.lower()


def test_empty_name_rejected():
    """An empty item name should be rejected with an error."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        result = _run("add", "", "--user", "张三")
        assert result.returncode != 0, "empty name should fail"
        assert "empty" in result.stderr.lower()


def test_add_without_user():
    """--user is required; omitting it should fail with a non-zero exit code."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        result = _run("add", "Task without user")
        assert result.returncode != 0


def test_list_empty():
    """Listing with no items should not crash."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        result = _run("list")
        assert result.returncode == 0


def test_persistence_between_runs():
    """Items should persist between independent invocations (two runners)."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run1, todos_path = _make_runner(tmpdir)
        _run1("add", "Persistent task", "--user", "张三")

        _run2, _ = _make_runner(tmpdir)
        result = _run2("list")
        assert "Persistent task" in result.stdout


# ---------------------------------------------------------------------------
# Unit tests — direct function calls (import todo, no subprocess)
# ---------------------------------------------------------------------------

import unittest
import todo


class TestTodoFunctions(unittest.TestCase):
    """Unit tests that directly import and call todo module functions."""

    def setUp(self):
        self.tmpdir = tempfile.TemporaryDirectory()
        self.todos_path = os.path.join(self.tmpdir.name, "todos.json")
        self._orig_env = os.environ.get("TODOS_PATH")
        os.environ["TODOS_PATH"] = self.todos_path

    def tearDown(self):
        self.tmpdir.cleanup()
        if self._orig_env is None:
            os.environ.pop("TODOS_PATH", None)
        else:
            os.environ["TODOS_PATH"] = self._orig_env

    def test_load_todos_valid_json(self):
        """load_todos should parse valid JSON."""
        with open(self.todos_path, "w", encoding="utf-8") as f:
            json.dump([{"name": "Task", "user": "Alice"}], f)
        result = todo.load_todos()
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]["name"], "Task")
        self.assertEqual(result[0]["user"], "Alice")

    def test_load_todos_empty_file(self):
        """load_todos should return [] when no file exists."""
        result = todo.load_todos()
        self.assertEqual(result, [])

    def test_load_todos_corrupted_json(self):
        """load_todos should exit gracefully on corrupted JSON."""
        with open(self.todos_path, "w", encoding="utf-8") as f:
            f.write("not valid json {{{")
        with self.assertRaises(SystemExit) as cm:
            todo.load_todos()
        self.assertEqual(cm.exception.code, 1)

    def test_add_todo_valid(self):
        """add_todo should add and persist a valid item."""
        msg = todo.add_todo("Buy milk", "张三")
        self.assertIn("Buy milk", msg)
        self.assertIn("张三", msg)
        todos = todo.load_todos()
        self.assertEqual(len(todos), 1)
        self.assertEqual(todos[0]["name"], "Buy milk")
        self.assertEqual(todos[0]["user"], "张三")

    def test_add_todo_empty_name(self):
        """add_todo should raise ValueError for empty name."""
        with self.assertRaises(ValueError) as cm:
            todo.add_todo("", "张三")
        self.assertIn("empty", str(cm.exception))

    def test_add_todo_duplicate(self):
        """add_todo should raise ValueError for duplicate."""
        todo.add_todo("Task", "张三")
        with self.assertRaises(ValueError) as cm:
            todo.add_todo("Task", "张三")
        self.assertIn("already exists", str(cm.exception))

    def test_save_and_load_roundtrip(self):
        """save_todos + load_todos round-trip should preserve data."""
        items = [
            {"name": "Task 1", "user": "Alice"},
            {"name": "Task 2", "user": "Bob"},
        ]
        todo.save_todos(items)
        result = todo.load_todos()
        self.assertEqual(result, items)
