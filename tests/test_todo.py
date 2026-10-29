"""Unit tests for todo.py."""
import json
import uuid
from datetime import datetime
from pathlib import Path

from scripts.todo import TodoItem, TodoManager


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