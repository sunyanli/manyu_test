# Todo Create Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal "create to-do" capability for internal users: a Python CLI (`python3 todo.py add <名称> [描述]`) that appends a to-do record (name + description) to a JSON file.

**Architecture:** Single flat Python module `todo.py` with a pure domain layer (JSON load/save + record creation) and a thin argparse CLI on top. Storage is one UTF-8 JSON array file (`todos.json` by default, overridable via `--file` for tests and multiple stores). Tests use stdlib `unittest` against temp directories; no third-party dependencies.

**Tech Stack:** Python 3.12 (stdlib only: `json`, `argparse`, `pathlib`, `datetime`, `uuid`), `unittest` (pytest is NOT installed in this environment — verified), UTF-8 JSON storage.

---

## Global Constraints

- Python 3.12+, **stdlib only** — no third-party dependencies (repo has none; pytest absent, confirmed via `python3 -m pytest --version` → "No module named pytest")
- Test framework: `unittest`; run all commands from the repository root
- Storage: UTF-8 JSON array file; default `todos.json` in cwd; override with `--file <path>`
- Record fields (exact keys): `id` (uuid4 hex string), `name` (stripped, non-empty), `description` (stripped, default `""`), `created_at` (UTC ISO-8601, e.g. `2026-09-22T07:30:00.123456+00:00`)
- Minimal closed loop = **create only** — no list/edit/delete/search, no auth (target users are internal), no config file
- CLI exit codes: 0 on success; 2 on usage/validation errors (argparse `parser.error`); errors print Chinese user-facing messages to stderr
- Follow existing flat repo layout (like `hello.py`): `todo.py` at root, tests under `tests/`
- Code comments and user-facing copy in Chinese, matching repo convention (`hello.py` docstring style)
- Run-time data file `todos.json` must never be committed (added to `.gitignore` in Task 3)

---

## File Structure

```
todo.py                    # Domain layer (load/save/add) + CLI (build_parser/main) — one responsibility: the todo recorder
tests/__init__.py          # Empty package marker for unittest discovery
tests/test_todo.py         # Unit tests: storage, add_todo domain, CLI add command
README.md                  # Usage doc for internal users (Task 3)
.gitignore                 # Ignore todos.json (Task 3)
todos.json                 # Runtime data file — created on first add, never committed
```

Existing files `hello.py`, `cred-helper-test.txt`, and `.agents/plans/login-feature.md` are unrelated to this feature and must not be modified.

---

### Task 1: Domain layer — JSON storage and record creation

**Files:**
- Create: `todo.py`
- Create: `tests/__init__.py`
- Test: `tests/test_todo.py`

**Interfaces:**
- Consumes: nothing (first task, stdlib only)
- Produces (later tasks rely on these exact signatures):
  - `DEFAULT_FILE: str = "todos.json"`
  - `load_todos(path: Path) -> list[dict]`
  - `save_todos(path: Path, todos: list[dict]) -> None`
  - `add_todo(todos: list[dict], name: str, description: str = "") -> dict` — appends one record to `todos` and returns it; record has keys `id`, `name`, `description`, `created_at`

- [ ] **Step 1: Write the failing test**

Create `tests/__init__.py` (empty file):

```bash
mkdir -p tests
touch tests/__init__.py
```

Create `tests/test_todo.py` with exactly this content:

```python
"""todo 领域层与存储层单元测试。"""

import tempfile
import unittest
from pathlib import Path

from todo import add_todo, load_todos, save_todos


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


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 -m unittest discover -v`
Expected: collection ERROR — `ModuleNotFoundError: No module named 'todo'`

- [ ] **Step 3: Write minimal implementation**

Create `todo.py` with exactly this content:

```python
#!/usr/bin/env python3
"""todo — 内部用户日常待办记录工具（本期最小闭环：新增待办）。"""

import json
import uuid
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_FILE = "todos.json"


def load_todos(path: Path) -> list[dict]:
    """读取待办列表；文件不存在时返回空列表。"""
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def save_todos(path: Path, todos: list[dict]) -> None:
    """将待办列表以 UTF-8 JSON 写入文件。"""
    path.write_text(
        json.dumps(todos, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def add_todo(todos: list[dict], name: str, description: str = "") -> dict:
    """在待办列表末尾追加一条新待办，返回新记录。"""
    record = {
        "id": uuid.uuid4().hex,
        "name": name,
        "description": description,
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    todos.append(record)
    return record
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 -m unittest discover -v`
Expected: `Ran 5 tests ... OK`

- [ ] **Step 5: Commit**

```bash
git add todo.py tests/__init__.py tests/test_todo.py
git commit -m "feat: add todo domain layer and JSON storage"
```

---

### Task 2: CLI — `add` subcommand

**Files:**
- Modify: `todo.py` (append CLI section after `add_todo`)
- Test: `tests/test_todo.py` (add `CliTests`)

**Interfaces:**
- Consumes (from Task 1, exact signatures): `load_todos(path: Path) -> list[dict]`, `save_todos(path: Path, todos: list[dict]) -> None`, `add_todo(todos: list[dict], name: str, description: str = "") -> dict`, `DEFAULT_FILE`
- Produces (Task 3 relies on):
  - `build_parser() -> argparse.ArgumentParser` — global flag `--file` (default `DEFAULT_FILE`); required subcommand `add` with positional `name` and optional positional `description` (default `""`)
  - `main(argv: list[str] | None = None) -> int` — returns `0` on success; raises `SystemExit(2)` via `parser.error` on blank name or bad usage

- [ ] **Step 1: Write the failing test**

In `tests/test_todo.py`, replace the import block at the top:

```python
import contextlib
import io
import tempfile
import unittest
from pathlib import Path

from todo import add_todo, load_todos, main, save_todos
```

Append this class before the `if __name__ == "__main__":` block:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 -m unittest discover -v`
Expected: collection ERROR — `ImportError: cannot import name 'main' from 'todo'`

- [ ] **Step 3: Write minimal implementation**

Replace the entire content of `todo.py` with exactly this (imports gain `argparse` and `sys`; CLI section appended; note the `if __name__` block is new):

```python
#!/usr/bin/env python3
"""todo — 内部用户日常待办记录工具（本期最小闭环：新增待办）。"""

import argparse
import json
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_FILE = "todos.json"


def load_todos(path: Path) -> list[dict]:
    """读取待办列表；文件不存在时返回空列表。"""
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def save_todos(path: Path, todos: list[dict]) -> None:
    """将待办列表以 UTF-8 JSON 写入文件。"""
    path.write_text(
        json.dumps(todos, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def add_todo(todos: list[dict], name: str, description: str = "") -> dict:
    """在待办列表末尾追加一条新待办，返回新记录。"""
    record = {
        "id": uuid.uuid4().hex,
        "name": name,
        "description": description,
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    todos.append(record)
    return record


def build_parser() -> argparse.ArgumentParser:
    """构建命令行解析器。"""
    parser = argparse.ArgumentParser(
        prog="todo", description="记录日常待办事项（本期仅支持新增）"
    )
    parser.add_argument(
        "--file", default=DEFAULT_FILE, help="待办存储文件路径（默认 %(default)s）"
    )
    sub = parser.add_subparsers(dest="command", required=True)
    p_add = sub.add_parser("add", help="新增待办事项")
    p_add.add_argument("name", help="事项名称")
    p_add.add_argument("description", nargs="?", default="", help="事项描述（可选）")
    return parser


def main(argv: list[str] | None = None) -> int:
    """CLI 入口：add 子命令写入一条待办并打印确认信息。"""
    parser = build_parser()
    args = parser.parse_args(argv)
    name = args.name.strip()
    if not name:
        parser.error("事项名称不能为空")
    path = Path(args.file)
    todos = load_todos(path)
    record = add_todo(todos, name, args.description.strip())
    save_todos(path, todos)
    print(f"已新增待办：{record['name']}（id={record['id']}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 -m unittest discover -v`
Expected: `Ran 8 tests ... OK`

- [ ] **Step 5: Commit**

```bash
git add todo.py tests/test_todo.py
git commit -m "feat: add todo CLI with add subcommand"
```

---

### Task 3: End-to-end acceptance + usage doc

**Files:**
- Create: `README.md`
- Create: `.gitignore`

**Interfaces:**
- Consumes (from Task 2): `python3 todo.py add <name> [description]` with global `--file`; success prints `已新增待办：<name>（id=<32位hex>）` and exits 0
- Produces: usage documentation for internal users; ignores the runtime data file

- [ ] **Step 1: Run the real CLI end-to-end (no test harness)**

Run:
```bash
python3 todo.py add "购买服务器" "为内部工具准备" --file /tmp/todo-e2e.json
cat /tmp/todo-e2e.json
rm /tmp/todo-e2e.json
```
Expected stdout of the first command: `已新增待办：购买服务器（id=` followed by a 32-char hex id and `）`.
Expected JSON: a one-element array whose record has exactly the keys `id`, `name` (`购买服务器`), `description` (`为内部工具准备`), `created_at` (ends with `+00:00`).

Then verify the default-file path writes to repo root and clean it up (must NOT be committed):
```bash
python3 todo.py add "冒烟" "默认文件路径"
cat todos.json
rm todos.json
```
Expected: same confirmation format; `todos.json` contains the record `冒烟`.

- [ ] **Step 2: Verify blank-name error path end-to-end**

Run: `python3 todo.py add "  "`
Expected: usage text plus `todo: error: 事项名称不能为空` on stderr; exit code 2 (check with `echo $?`)

- [ ] **Step 3: Write README.md**

Create `README.md` with exactly this content:

````markdown
# todo — 内部待办记录工具

帮助内部用户记录日常待办事项。本期最小闭环：仅支持新增待办。

## 使用

```bash
python3 todo.py add <事项名称> [事项描述]
```

示例：

```bash
python3 todo.py add "购买服务器" "为内部工具准备"
```

- 事项名称必填（不能为空白）；事项描述可选。
- 待办默认保存在当前目录的 `todos.json`；用 `--file <路径>` 可指定其他存储文件。
- 运行环境：Python 3.12+，无第三方依赖。

## 开发

```bash
python3 -m unittest discover -v
```
````

- [ ] **Step 4: Write .gitignore**

Create `.gitignore` with exactly this content:

```
todos.json
__pycache__/
*.pyc
```

- [ ] **Step 5: Run the full suite, then commit**

Run: `python3 -m unittest discover -v`
Expected: `Ran 8 tests ... OK`

```bash
git add README.md .gitignore
git commit -m "docs: add todo usage guide and ignore runtime data file"
```

---

## Verification Checklist (acceptance per spec)

- 新增待办：`python3 todo.py add "名称" "描述"` → exit 0, record appended with `name` + `description` + `id` + `created_at`
- 事项名称与描述：name 必填（空白拒绝，exit 2），description 可选（默认空串）
- 最小闭环：仅创建 — 计划中不存在 list/edit/delete 任务
- 内部用户：无登录/鉴权环节
- `python3 -m unittest discover -v` 全绿（8 tests）
