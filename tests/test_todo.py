"""todo 领域层与存储层单元测试。"""

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

from todo import add_todo, load_todos, main, save_todos


class StorageTests(unittest.TestCase):
    def test_load_missing_file_returns_empty_list(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(load_todos(Path(tmp) / "todos.json"), [])

    def test_save_then_load_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "todos.json"
            record = {
                "id": "abc123",
                "name": "买牛奶",
                "description": "两盒全脂",
                "created_at": "2026-09-22T00:00:00+00:00",
            }
            save_todos(path, [record])
            self.assertEqual(load_todos(path), [record])


class AddTodoTests(unittest.TestCase):
    def test_add_todo_appends_record_with_task_info(self):
        todos = []
        record = add_todo(todos, "写周报", "整理本周进展")
        self.assertEqual(todos, [record])
        self.assertEqual(record["name"], "写周报")
        self.assertEqual(record["description"], "整理本周进展")

    def test_add_todo_assigns_unique_id_and_utc_timestamp(self):
        todos = []
        first = add_todo(todos, "任务一", "")
        second = add_todo(todos, "任务二", "")
        self.assertTrue(first["id"])
        self.assertNotEqual(first["id"], second["id"])
        self.assertTrue(first["created_at"].endswith("+00:00"))

    def test_add_todo_description_defaults_to_empty(self):
        todos = []
        record = add_todo(todos, "只填名称")
        self.assertEqual(record["description"], "")


class CliTests(unittest.TestCase):
    def test_add_command_writes_record_to_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "todos.json"
            exit_code = main(["--file", str(path), "add", "买牛奶", "两盒全脂"])
            self.assertEqual(exit_code, 0)
            stored = load_todos(path)
            self.assertEqual(len(stored), 1)
            self.assertEqual(stored[0]["name"], "买牛奶")
            self.assertEqual(stored[0]["description"], "两盒全脂")

    def test_add_command_without_description_defaults_to_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "todos.json"
            self.assertEqual(main(["--file", str(path), "add", "只填名称"]), 0)
            self.assertEqual(load_todos(path)[0]["description"], "")

    def test_add_command_with_blank_name_fails_before_write(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "todos.json"
            stderr = io.StringIO()
            with contextlib.redirect_stderr(stderr):
                with self.assertRaises(SystemExit) as ctx:
                    main(["--file", str(path), "add", "   "])
            self.assertEqual(ctx.exception.code, 2)
            self.assertIn("事项名称不能为空", stderr.getvalue())
            self.assertFalse(path.exists())


if __name__ == "__main__":
    unittest.main()
