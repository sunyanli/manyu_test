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

