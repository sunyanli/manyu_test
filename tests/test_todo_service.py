"""Tests for TodoService — AAA pattern (Arrange, Act, Assert)."""

import pytest
from datetime import datetime

from src.service.todo_service import TodoService
from src.model.entity.TodoItem import TodoItem
from src.model.dto.TodoRequest import TodoRequest


class TestTodoServiceCreate:
    """Test TodoService.create_todo method."""

    def test_should_create_todo_when_valid_request(self):
        """正常路径：有效的 title 和 description，应成功创建待办事项。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        request = TodoRequest(title="完成项目设计文档", description="编写系统分析设计文档并提交评审")

        # Act
        result = service.create_todo(user_id=1, request=request)

        # Assert
        assert result is not None
        assert result.id == 1
        assert result.title == "完成项目设计文档"
        assert result.description == "编写系统分析设计文档并提交评审"
        assert result.user_id == 1
        assert isinstance(result.gmt_create, datetime)
        assert isinstance(result.gmt_modified, datetime)
        assert len(todo_list) == 1

    def test_should_create_todo_without_description(self):
        """正常路径：description 为空（选填），应成功创建待办事项。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        request = TodoRequest(title="简单任务")

        # Act
        result = service.create_todo(user_id=2, request=request)

        # Assert
        assert result is not None
        assert result.title == "简单任务"
        assert result.description is None
        assert result.user_id == 2
        assert len(todo_list) == 1

    def test_should_raise_when_title_is_empty(self):
        """异常路径：title 为空字符串，应抛出 ValueError。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        request = TodoRequest(title="", description="有空标题")

        # Act & Assert
        with pytest.raises(ValueError, match="事项名称不能为空"):
            service.create_todo(user_id=1, request=request)

    def test_should_raise_when_title_is_whitespace(self):
        """异常路径：title 为纯空格，应抛出 ValueError。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        request = TodoRequest(title="   ", description="纯空格标题")

        # Act & Assert
        with pytest.raises(ValueError, match="事项名称不能为空"):
            service.create_todo(user_id=1, request=request)

    def test_should_raise_when_title_exceeds_200_chars(self):
        """异常路径：title 超过 200 字符，应抛出 ValueError。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        long_title = "x" * 201
        request = TodoRequest(title=long_title)

        # Act & Assert
        with pytest.raises(ValueError, match="事项名称长度不能超过 200 字符"):
            service.create_todo(user_id=1, request=request)

    def test_should_raise_when_description_exceeds_2000_chars(self):
        """异常路径：description 超过 2000 字符，应抛出 ValueError。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        long_desc = "x" * 2001
        request = TodoRequest(title="正常标题", description=long_desc)

        # Act & Assert
        with pytest.raises(ValueError, match="事项描述长度不能超过 2000 字符"):
            service.create_todo(user_id=1, request=request)

    def test_should_assign_incremental_id(self):
        """正常路径：连续创建多条待办，ID 应自增。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        req1 = TodoRequest(title="任务1")
        req2 = TodoRequest(title="任务2")

        # Act
        r1 = service.create_todo(user_id=1, request=req1)
        r2 = service.create_todo(user_id=1, request=req2)

        # Assert
        assert r1.id == 1
        assert r2.id == 2

    def test_should_set_user_id_on_todo(self):
        """正常路径：创建的待办事项应绑定正确的 user_id。"""
        # Arrange
        todo_list: list[TodoItem] = []
        service = TodoService(todo_list=todo_list)
        request = TodoRequest(title="用户专属任务")

        # Act
        result = service.create_todo(user_id=42, request=request)

        # Assert
        assert result.user_id == 42