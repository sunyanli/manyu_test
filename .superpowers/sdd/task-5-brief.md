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
