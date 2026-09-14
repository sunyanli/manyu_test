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
    """Items should persist between runs (second invocation of list)."""
    with tempfile.TemporaryDirectory() as tmpdir:
        _run, todos_path = _make_runner(tmpdir)

        _run("add", "Persistent task", "--user", "张三")
        result = _run("list")
        assert "Persistent task" in result.stdout
