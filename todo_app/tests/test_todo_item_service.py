"""TodoItemService 单元测试

遵循 AAA 模式（Arrange-Act-Assert），使用真实 Repository（内存实现）替代 Mock，
因 Repository 为无副作用内存实现，符合 FIRST 原则中的 Fast/Independent。
"""

import unittest

from todo_app.common.errors import (
    TodoBizException,
    TodoErrorCode,
)
from todo_app.model.todo_item_do import DEFAULT_TENANT_ID
from todo_app.repository.todo_item_repository import TodoItemRepository
from todo_app.service.todo_item_service import TodoItemService


class TestCreateTodo(unittest.TestCase):
    """create_todo 方法测试集。"""

    def setUp(self):
        """每个用例独立的 Service 实例，保证用例隔离。"""
        self.service = TodoItemService(TodoItemRepository())

    def test_should_return_todo_when_valid_request(self):
        # Arrange
        name = "完成日报"
        description = "编写并提交每日工作日报"

        # Act
        result = self.service.create_todo(name=name, description=description)

        # Assert
        self.assertEqual(result.id, 1)
        self.assertEqual(result.name, name)
        self.assertEqual(result.description, description)
        self.assertEqual(result.tenant_id, DEFAULT_TENANT_ID)

    def test_should_raise_todo_001_when_name_none(self):
        # Act & Assert
        with self.assertRaises(TodoBizException) as ctx:
            self.service.create_todo(name=None)
        self.assertEqual(ctx.exception.code, TodoErrorCode.NAME_EMPTY)

    def test_should_raise_todo_001_when_name_blank(self):
        with self.assertRaises(TodoBizException) as ctx:
            self.service.create_todo(name="   ")
        self.assertEqual(ctx.exception.code, TodoErrorCode.NAME_EMPTY)

    def test_should_raise_todo_002_when_name_too_long(self):
        # Arrange
        long_name = "a" * 101

        # Act & Assert
        with self.assertRaises(TodoBizException) as ctx:
            self.service.create_todo(name=long_name)
        self.assertEqual(ctx.exception.code, TodoErrorCode.NAME_TOO_LONG)

    def test_should_raise_todo_003_when_description_too_long(self):
        long_desc = "b" * 501
        with self.assertRaises(TodoBizException) as ctx:
            self.service.create_todo(name="ok", description=long_desc)
        self.assertEqual(ctx.exception.code, TodoErrorCode.DESCRIPTION_TOO_LONG)

    def test_should_use_default_tenant_when_omitted(self):
        result = self.service.create_todo(name="x")
        self.assertEqual(result.tenant_id, DEFAULT_TENANT_ID)

    def test_should_use_given_tenant_when_provided(self):
        result = self.service.create_todo(name="x", tenant_id="t-100")
        self.assertEqual(result.tenant_id, "t-100")

    def test_should_generate_incremental_ids(self):
        first = self.service.create_todo(name="one")
        second = self.service.create_todo(name="two")
        self.assertEqual(second.id, first.id + 1)

    def test_should_accept_boundary_name_100_chars(self):
        result = self.service.create_todo(name="a" * 100)
        self.assertEqual(len(result.name), 100)

    def test_should_accept_boundary_desc_500_chars(self):
        result = self.service.create_todo(name="ok", description="b" * 500)
        self.assertEqual(len(result.description), 500)


if __name__ == "__main__":
    unittest.main()
