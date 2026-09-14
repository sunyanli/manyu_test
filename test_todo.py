#!/usr/bin/env python3
"""Tests for the daily to-do CLI tool."""

import json
import os
import subprocess
import sys
import tempfile
import unittest


class TestTodoCLI(unittest.TestCase):
    """Integration tests for todo.py CLI."""

    def setUp(self):
        """Create a temporary directory and todos.json path for each test."""
        self.tmpdir = tempfile.mkdtemp()
        self.todos_path = os.path.join(self.tmpdir, "todos.json")

    def tearDown(self):
        """Clean up the temporary directory."""
        import shutil
        shutil.rmtree(self.tmpdir, ignore_errors=True)

    def _run(self, *args):
        """Run todo.py with the given arguments and return CompletedProcess."""
        env = os.environ.copy()
        env["TODOS_PATH"] = self.todos_path
        return subprocess.run(
            [sys.executable, "todo.py"] + list(args),
            capture_output=True,
            text=True,
            env=env,
        )

    def test_add_creates_item(self):
        """Adding a todo item should succeed and persist to todos.json."""
        result = self._run("add", "Buy groceries", "--user", "张三")
        self.assertEqual(result.returncode, 0, f"add failed: {result.stderr}")

        # Verify the item was persisted.
        self.assertTrue(os.path.exists(self.todos_path), "todos.json not created")
        with open(self.todos_path, "r", encoding="utf-8") as f:
            todos = json.load(f)
        self.assertEqual(len(todos), 1)
        self.assertEqual(todos[0]["name"], "Buy groceries")
        self.assertEqual(todos[0]["user"], "张三")

    def test_list_displays_items(self):
        """Listing should display all persisted todo items."""
        # First add an item.
        self._run("add", "Buy groceries", "--user", "张三")
        # Then add another.
        self._run("add", "Read book", "--user", "李四")

        result = self._run("list")
        self.assertEqual(result.returncode, 0)
        output = result.stdout
        self.assertIn("Buy groceries", output)
        self.assertIn("张三", output)
        self.assertIn("Read book", output)
        self.assertIn("李四", output)

    def test_duplicate_rejected(self):
        """Adding an item with the same name and user should be rejected."""
        self._run("add", "Buy groceries", "--user", "张三")
        result = self._run("add", "Buy groceries", "--user", "张三")
        self.assertNotEqual(result.returncode, 0, "duplicate should fail")
        self.assertIn("already exists", result.stderr.lower())

    def test_empty_name_rejected(self):
        """An empty item name should be rejected with an error."""
        result = self._run("add", "", "--user", "张三")
        self.assertNotEqual(result.returncode, 0, "empty name should fail")
        self.assertIn("empty", result.stderr.lower())

    def test_add_without_user(self):
        """Adding without --user should still work (user field maybe optional)."""
        # The spec doesn't say --user is optional, but the example shows it.
        # Let's test that --user is required, or default to something.
        result = self._run("add", "Task without user")
        # Spec doesn't define behavior without --user; check it doesn't crash.
        # We'll assume --user is required.
        self.assertNotEqual(result.returncode, 0)

    def test_list_empty(self):
        """Listing with no items should not crash."""
        result = self._run("list")
        self.assertEqual(result.returncode, 0)

    def test_persistence_between_runs(self):
        """Items should persist between runs (second invocation of list)."""
        self._run("add", "Persistent task", "--user", "张三")
        # Second run should see the same item.
        result = self._run("list")
        self.assertIn("Persistent task", result.stdout)


if __name__ == "__main__":
    unittest.main()