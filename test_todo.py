"""Tests for TODO CLI app — Task 1: TODO 创建功能"""

import os
import sqlite3
import tempfile
import unittest

import todo


class TestInitDB(unittest.TestCase):
    """Tests for init_db() — idempotent table creation."""

    def setUp(self):
        self.tmpfile = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.db_path = self.tmpfile.name
        self.tmpfile.close()
        todo._DB_PATH = self.db_path

    def tearDown(self):
        todo._DB_PATH = "todos.db"
        os.unlink(self.db_path)

    def test_init_db_creates_table(self):
        todo.init_db()
        conn = sqlite3.connect(self.db_path)
        cursor = conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='todos'"
        )
        self.assertIsNotNone(cursor.fetchone())

    def test_init_db_is_idempotent(self):
        todo.init_db()
        todo.init_db()  # Should not raise
        conn = sqlite3.connect(self.db_path)
        cursor = conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='todos'"
        )
        self.assertIsNotNone(cursor.fetchone())


class TestAddTodo(unittest.TestCase):
    """Tests for add_todo()."""

    def setUp(self):
        self.tmpfile = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.db_path = self.tmpfile.name
        self.tmpfile.close()
        todo._DB_PATH = self.db_path
        todo.init_db()

    def tearDown(self):
        todo._DB_PATH = "todos.db"
        os.unlink(self.db_path)

    def _get_row_count(self):
        conn = sqlite3.connect(self.db_path)
        return conn.execute("SELECT COUNT(*) FROM todos").fetchone()[0]

    def test_add_todo_normal(self):
        """正常创建：带标题和描述"""
        row_count_before = self._get_row_count()
        result = todo.add_todo("买菜", "去超市买菜")
        self.assertEqual(row_count_before + 1, self._get_row_count())
        self.assertIsInstance(result, dict)
        self.assertIn("id", result)
        self.assertIsInstance(result["id"], int)
        self.assertEqual(result["title"], "买菜")
        self.assertEqual(result["description"], "去超市买菜")
        self.assertIn("created_at", result)

    def test_add_todo_title_only(self):
        """仅标题创建（无描述）"""
        result = todo.add_todo("跑步")
        self.assertEqual(result["title"], "跑步")
        self.assertEqual(result["description"], "")

    def test_add_todo_with_description(self):
        """带描述创建"""
        result = todo.add_todo("写报告", "完成季度总结报告")
        self.assertEqual(result["title"], "写报告")
        self.assertEqual(result["description"], "完成季度总结报告")

    def test_add_todo_empty_title_raises(self):
        """空标题应被拒绝"""
        with self.assertRaises(ValueError):
            todo.add_todo("")
        with self.assertRaises(ValueError):
            todo.add_todo("   ")

    def test_add_todo_returns_distinct_ids(self):
        """连续创建应返回不同的 id"""
        r1 = todo.add_todo("任务一")
        r2 = todo.add_todo("任务二")
        self.assertNotEqual(r1["id"], r2["id"])


if __name__ == "__main__":
    unittest.main()