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

