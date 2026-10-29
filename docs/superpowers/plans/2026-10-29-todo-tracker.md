# TODO 待办事项记录工具 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为内部用户构建一个最小化的 TODO 待办事项记录工具，支持新增待办事项（名称 + 描述），JSON 文件持久化。

**Architecture:** 纯 Python 模块，无外部框架依赖。`TodoManager` 类封装 JSON 文件读写与事项管理，`TodoItem` 数据类承载字段。持久化文件位于 `data/todos.json`，首次写入时自动创建。

**Tech Stack:** Python 3, pytest, JSON (stdlib), uuid (stdlib), dataclasses (stdlib)

## Global Constraints

- 零外部 pip 依赖——仅使用 Python 标准库
- 最小闭环：仅实现"新增"操作，不包含编辑/删除/查询
- JSON 文件持久化，路径 `data/todos.json`
- 每条事项字段：id (UUID4)、name (str)、description (str)、created_at (ISO 8601 str)
- 遵循仓库现有 pytest 测试风格（`tmp_path` fixture、`assert` 断言、中文注释）
- 业务代码放在 `scripts/` 目录，测试放在 `tests/` 目录

---

### Task 1: 定义 TodoItem 数据类

**Files:**
- Create: `scripts/todo.py`

**Interfaces:**
- Consumes: 无（第一个任务）
- Produces: `TodoItem` dataclass，字段 `id: str`, `name: str`, `description: str`, `created_at: str`

- [ ] **Step 1: 写入 TodoItem 数据类定义**

```python
"""TODO 待办事项管理模块 — 仅支持新增操作（最小闭环）."""
import json
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional


@dataclass
class TodoItem:
    """一条待办事项."""

    name: str
    description: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat()
    )

    def to_dict(self) -> dict:
        """序列化为字典."""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TodoItem":
        """从字典反序列化."""
        return cls(
            id=data["id"],
            name=data["name"],
            description=data["description"],
            created_at=data["created_at"],
        )
```

- [ ] **Step 2: 验证模块可导入**

Run: `python -c "from scripts.todo import TodoItem; t = TodoItem('测试', '描述'); print(t.to_dict())"`

Expected: 输出包含 `name`、`description`、`id`、`created_at` 的字典，退出码 0。

- [ ] **Step 3: Commit**

```bash
git add scripts/todo.py
git commit -m "feat: add TodoItem dataclass to todo module"
```

---

### Task 2: 实现 TodoManager 新增 + 持久化

**Files:**
- Modify: `scripts/todo.py` — 追加 `TodoManager` 类

**Interfaces:**
- Consumes: `TodoItem`（Task 1 定义）
- Produces: `TodoManager(file_path: Path)` with `add(name: str, description: str) -> TodoItem`, `_load() -> list[dict]`, `_save(items: list[dict]) -> None`

- [ ] **Step 1: 追加 TodoManager 类到 scripts/todo.py**

在文件末尾追加以下代码（紧接 `TodoItem` 类之后）：

```python
DEFAULT_STORAGE_PATH = Path("data/todos.json")


class TodoManager:
    """管理待办事项的持久化与操作."""

    def __init__(self, file_path: Optional[Path] = None) -> None:
        self._file_path = file_path or DEFAULT_STORAGE_PATH

    def _load(self) -> list[dict]:
        """从 JSON 文件加载所有事项."""
        if not self._file_path.exists():
            return []
        with open(self._file_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _save(self, items: list[dict]) -> None:
        """将所有事项写入 JSON 文件."""
        self._file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self._file_path, "w", encoding="utf-8") as f:
            json.dump(items, f, ensure_ascii=False, indent=2)

    def add(self, name: str, description: str) -> TodoItem:
        """新增一条待办事项并持久化.

        Args:
            name: 事项名称（必填）.
            description: 事项描述（必填）.

        Returns:
            创建的 TodoItem 实例.
        """
        item = TodoItem(name=name, description=description)
        current = self._load()
        current.append(item.to_dict())
        self._save(current)
        return item
```

- [ ] **Step 2: 手动冒烟验证**

Run: `python -c "
from pathlib import Path
from scripts.todo import TodoManager
import tempfile, os

tmp = Path(tempfile.mkdtemp()) / 'todos.json'
mgr = TodoManager(tmp)
item = mgr.add('买菜', '去超市买蔬菜和水果')
print(f'Created: id={item.id}, name={item.name}, desc={item.description}')
assert tmp.exists()
import json
data = json.loads(tmp.read_text('utf-8'))
assert len(data) == 1
assert data[0]['name'] == '买菜'
print('OK - persistence verified')
"`

Expected: 输出 `OK - persistence verified`，退出码 0。

- [ ] **Step 3: Commit**

```bash
git add scripts/todo.py
git commit -m "feat: add TodoManager with JSON file persistence"
```

---

### Task 3: 编写 TodoItem 单元测试

**Files:**
- Create: `tests/test_todo.py`

**Interfaces:**
- Consumes: `TodoItem`（Task 1）
- Produces: 无（测试文件，不被其他模块消费）

- [ ] **Step 1: 写入 TodoItem 测试**

```python
"""Unit tests for todo.py."""
import uuid
from datetime import datetime

from scripts.todo import TodoItem


class TestTodoItem:
    """TodoItem 数据类单元测试."""

    def test_create_with_name_and_description(self):
        """使用名称和描述创建 TodoItem，字段正确填充."""
        item = TodoItem(name="写周报", description="汇总本周工作内容")

        assert item.name == "写周报"
        assert item.description == "汇总本周工作内容"
        assert item.id  # 非空
        assert item.created_at  # 非空

    def test_id_is_valid_uuid4_string(self):
        """id 为合法的 UUID4 字符串."""
        item = TodoItem(name="测试", description="测试 UUID")

        uuid.UUID(item.id, version=4)  # 不抛异常即为合法 UUID4

    def test_id_is_unique_across_instances(self):
        """不同实例的 id 不重复."""
        item1 = TodoItem(name="A", description="desc A")
        item2 = TodoItem(name="B", description="desc B")

        assert item1.id != item2.id

    def test_created_at_is_iso8601_utc(self):
        """created_at 为 ISO 8601 UTC 格式字符串."""
        item = TodoItem(name="测试", description="时间格式")

        parsed = datetime.fromisoformat(item.created_at)
        assert parsed.utcoffset() is not None or item.created_at.endswith("+00:00") or "Z" in item.created_at

    def test_to_dict_returns_all_fields(self):
        """to_dict 返回包含全部字段的字典."""
        item = TodoItem(name="测试", description="描述")

        result = item.to_dict()

        assert isinstance(result, dict)
        assert result["name"] == "测试"
        assert result["description"] == "描述"
        assert result["id"] == item.id
        assert result["created_at"] == item.created_at

    def test_from_dict_restores_identical_item(self):
        """from_dict 反序列化后与原始对象字段一致."""
        original = TodoItem(name="原始", description="原始描述")
        data = original.to_dict()
        restored = TodoItem.from_dict(data)

        assert restored.id == original.id
        assert restored.name == original.name
        assert restored.description == original.description
        assert restored.created_at == original.created_at
```

- [ ] **Step 2: 运行测试验证全部通过**

Run: `python -m pytest tests/test_todo.py -v`

Expected: 6 passed, 0 failed.

- [ ] **Step 3: Commit**

```bash
git add tests/test_todo.py
git commit -m "test: add TodoItem unit tests"
```

---

### Task 4: 编写 TodoManager 单元测试

**Files:**
- Modify: `tests/test_todo.py` — 追加 `TestTodoManager` 类

**Interfaces:**
- Consumes: `TodoManager`、`TodoItem`（Task 1、Task 2）
- Produces: 无

- [ ] **Step 1: 追加 TodoManager 测试类到 tests/test_todo.py**

在文件末尾追加以下代码：

```python
import json
from pathlib import Path

from scripts.todo import TodoManager


class TestTodoManager:
    """TodoManager 持久化与操作单元测试."""

    def test_add_creates_file_when_missing(self, tmp_path: Path):
        """存储文件不存在时，add 自动创建文件和目录."""
        storage = tmp_path / "subdir" / "todos.json"
        mgr = TodoManager(storage)

        mgr.add("任务", "描述")

        assert storage.exists()
        data = json.loads(storage.read_text("utf-8"))
        assert len(data) == 1

    def test_add_appends_to_existing_file(self, tmp_path: Path):
        """已有数据时，add 追加而非覆盖."""
        storage = tmp_path / "todos.json"
        mgr = TodoManager(storage)

        item1 = mgr.add("第一项", "第一条描述")
        item2 = mgr.add("第二项", "第二条描述")

        data = json.loads(storage.read_text("utf-8"))
        assert len(data) == 2
        ids = [d["id"] for d in data]
        assert item1.id in ids
        assert item2.id in ids

    def test_add_returns_correct_todo_item(self, tmp_path: Path):
        """add 返回的 TodoItem 包含正确的 name 和 description."""
        storage = tmp_path / "todos.json"
        mgr = TodoManager(storage)

        item = mgr.add("晨会", "参加每日站会，汇报进度")

        assert item.name == "晨会"
        assert item.description == "参加每日站会，汇报进度"
        assert item.id  # 非空

    def test_add_persists_all_fields(self, tmp_path: Path):
        """持久化后的 JSON 包含全部字段."""
        storage = tmp_path / "todos.json"
        mgr = TodoManager(storage)

        item = mgr.add("代码审查", "审查 PR #42")

        data = json.loads(storage.read_text("utf-8"))
        record = data[0]
        assert record["id"] == item.id
        assert record["name"] == "代码审查"
        assert record["description"] == "审查 PR #42"
        assert record["created_at"] == item.created_at

    def test_add_preserves_existing_data(self, tmp_path: Path):
        """add 保留文件中已有条目的完整性."""
        storage = tmp_path / "todos.json"
        # 预制一条数据
        storage.parent.mkdir(parents=True, exist_ok=True)
        existing = {
            "id": "abc-123",
            "name": "已有任务",
            "description": "之前添加的",
            "created_at": "2026-01-01T00:00:00+00:00",
        }
        storage.write_text(json.dumps([existing]), encoding="utf-8")

        mgr = TodoManager(storage)
        mgr.add("新任务", "新添加的")

        data = json.loads(storage.read_text("utf-8"))
        assert len(data) == 2
        assert data[0] == existing  # 原数据未被篡改
        assert data[1]["name"] == "新任务"
```

- [ ] **Step 2: 运行全部测试**

Run: `python -m pytest tests/test_todo.py -v`

Expected: 11 passed, 0 failed（Task 3 的 6 条 + 本次 5 条）。

- [ ] **Step 3: Commit**

```bash
git add tests/test_todo.py
git commit -m "test: add TodoManager persistence unit tests"
```

---

### Task 5: 冒烟集成测试（端到端验证）

**Files:**
- Modify: `tests/test_todo.py` — 追加 `TestTodoIntegration` 类

**Interfaces:**
- Consumes: `TodoManager`（Task 2）
- Produces: 无

- [ ] **Step 1: 追加集成测试到 tests/test_todo.py**

在文件末尾追加以下代码：

```python
class TestTodoIntegration:
    """端到端集成测试 — 模拟真实使用场景."""

    def test_full_flow_multiple_items(self, tmp_path: Path):
        """连续添加多条事项，验证完整流程."""
        storage = tmp_path / "todos.json"
        mgr = TodoManager(storage)

        items = [
            mgr.add("需求评审", "评审 V2.0 需求文档"),
            mgr.add("修复 Bug", "修复登录页 500 错误"),
            mgr.add("发布上线", "发布 V2.0 到生产环境"),
        ]

        assert len(items) == 3
        names = [item.name for item in items]
        assert names == ["需求评审", "修复 Bug", "发布上线"]

        # 验证持久化
        data = json.loads(storage.read_text("utf-8"))
        assert len(data) == 3
        for item in items:
            matching = [d for d in data if d["id"] == item.id]
            assert len(matching) == 1
            assert matching[0]["name"] == item.name
```

- [ ] **Step 2: 运行全部测试**

Run: `python -m pytest tests/test_todo.py -v`

Expected: 12 passed, 0 failed（Task 3 的 6 条 + Task 4 的 5 条 + 本次 1 条）。

- [ ] **Step 3: Commit**

```bash
git add tests/test_todo.py
git commit -m "test: add end-to-end integration smoke test"
```

---

## Self-Review

**1. Spec coverage:**
- ✅ "新增待办事项" → Task 2 `add()` 方法，Task 4 全覆盖测试
- ✅ "事项名称和描述" → TodoItem 字段 `name` + `description`，Task 3 验证
- ✅ "最小闭环：仅创建" → 只实现 `add`，无 edit/delete/query
- ✅ "内部用户" → 纯本地工具，零网络依赖
- ✅ JSON 持久化 → Task 2 `_load/_save`，Task 4 覆盖文件读写

**2. Placeholder scan:** 无 TBD/TODO/placeholder/edge-case 占位。每个测试步骤都包含完整可运行的代码。

**3. Type consistency:**
- `TodoItem` 字段签名：`id: str, name: str, description: str, created_at: str` — Task 1 定义，Task 3/4/5 一致引用
- `TodoManager.add(name: str, description: str) -> TodoItem` — Task 2 定义，Task 4/5 一致调用
- 文件路径：`scripts/todo.py`、`tests/test_todo.py`、`data/todos.json` — 全部任务一致

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-10-29-todo-tracker.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**