"""TodoService 单元测试"""
import unittest
from datetime import datetime
from todo_service import TodoItem, TodoService


class TestTodoItem(unittest.TestCase):
    """测试 TodoItem 数据类"""

    def test_create_todo_item_with_name_and_description(self):
        """应能创建含名称和描述的待办事项"""
        item = TodoItem(name="测试事项", description="这是测试描述")
        self.assertEqual(item.name, "测试事项")
        self.assertEqual(item.description, "这是测试描述")
        self.assertIsNotNone(item.id)
        self.assertFalse(item.completed)
        self.assertIsInstance(item.created_at, datetime)

    def test_create_todo_item_with_minimal_fields(self):
        """应能仅用名称创建待办事项"""
        item = TodoItem(name="最小事项")
        self.assertEqual(item.name, "最小事项")
        self.assertEqual(item.description, "")
        self.assertIsNotNone(item.id)
        self.assertFalse(item.completed)

    def test_todo_item_string_representation(self):
        """字符串表示应包含名称和完成状态"""
        item = TodoItem(name="测试", description="desc")
        self.assertIn("测试", str(item))
        self.assertIn("未完成", str(item))

    def test_todo_item_id_unique(self):
        """每个待办事项的ID应唯一"""
        item1 = TodoItem(name="事项1")
        item2 = TodoItem(name="事项2")
        self.assertNotEqual(item1.id, item2.id)

    def test_mark_as_completed(self):
        """应支持标记为已完成"""
        item = TodoItem(name="可完成事项")
        item.mark_completed()
        self.assertTrue(item.completed)


class TestTodoService(unittest.TestCase):
    """测试 TodoService 业务逻辑"""

    def setUp(self):
        self.service = TodoService()

    def test_add_todo_should_return_item_with_id(self):
        """添加待办应返回带ID的事项"""
        item = self.service.add_todo("会议准备", "准备下午的会议材料")
        self.assertIsNotNone(item)
        self.assertIsNotNone(item.id)
        self.assertEqual(item.name, "会议准备")
        self.assertEqual(item.description, "准备下午的会议材料")

    def test_add_todo_should_increase_count(self):
        """添加待办后事项数量应增加"""
        self.service.add_todo("事项A", "描述A")
        self.service.add_todo("事项B", "描述B")
        self.assertEqual(len(self.service.list_all()), 2)

    def test_add_todo_with_empty_name(self):
        """名称为空时应抛出异常"""
        with self.assertRaises(ValueError):
            self.service.add_todo("", "描述")

    def test_add_todo_with_whitespace_name(self):
        """名称为空白时应抛出异常"""
        with self.assertRaises(ValueError):
            self.service.add_todo("   ", "描述")

    def test_add_todo_with_name_only(self):
        """应支持仅添加名称的待办事项"""
        item = self.service.add_todo("仅名称事项")
        self.assertEqual(item.description, "")

    def test_add_todo_with_long_name_and_description(self):
        """应支持长名称和长描述"""
        long_name = "A" * 200
        long_desc = "B" * 1000
        item = self.service.add_todo(long_name, long_desc)
        self.assertEqual(item.name, long_name)
        self.assertEqual(item.description, long_desc)

    def test_list_all_empty(self):
        """初始状态下列表应为空"""
        self.assertEqual(len(self.service.list_all()), 0)

    def test_multiple_todos_have_unique_ids(self):
        """多次添加应生成唯一ID"""
        id_set = set()
        for i in range(10):
            item = self.service.add_todo(f"事项{i}")
            id_set.add(item.id)
        self.assertEqual(len(id_set), 10)

    def test_get_todo_by_id(self):
        """应能通过ID获取待办事项"""
        added = self.service.add_todo("查找测试", "通过ID查找")
        found = self.service.get_by_id(added.id)
        self.assertIsNotNone(found)
        self.assertEqual(found.id, added.id)
        self.assertEqual(found.name, "查找测试")

    def test_get_todo_by_nonexistent_id(self):
        """不存在的ID应返回None"""
        result = self.service.get_by_id("non_existent_id")
        self.assertIsNone(result)

    def test_add_todo_returns_correct_item_in_list(self):
        """添加后的事项应在列表中找到"""
        added = self.service.add_todo("验证事项", "验证描述")
        items = self.service.list_all()
        found = any(item.id == added.id for item in items)
        self.assertTrue(found)


if __name__ == "__main__":
    unittest.main()