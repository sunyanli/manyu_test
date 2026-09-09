"""Service 层：TodoItemService

实现系分方案 §5.1.3.1 创建待办事项（F01）业务规则：
- R01: name 非空
- R02: name 长度 ≤ 100
- R03: description 长度 ≤ 500
"""

import logging
from typing import Optional

from todo_app.common.errors import (
    TodoBizException,
    TodoErrorCode,
    TodoErrorMessage,
)
from todo_app.model.todo_item_do import DEFAULT_TENANT_ID, TodoItemDO
from todo_app.repository.todo_item_repository import TodoItemRepository

logger = logging.getLogger(__name__)

NAME_MAX_LENGTH: int = 100
DESCRIPTION_MAX_LENGTH: int = 500


class TodoItemService:
    """待办事项业务服务。"""

    def __init__(self, repository: TodoItemRepository) -> None:
        self._repository: TodoItemRepository = repository

    def create_todo(
        self,
        name: Optional[str],
        description: Optional[str] = None,
        tenant_id: Optional[str] = None,
    ) -> TodoItemDO:
        """创建待办事项。

        Args:
            name: 事项名称，必填，最大 100 字符。
            description: 事项描述，可选，最大 500 字符。
            tenant_id: 租户 ID，默认 "default"。

        Returns:
            已持久化的 TodoItemDO。

        Raises:
            TodoBizException: 业务规则不满足时抛出对应错误码。
        """
        self._validate_name(name)
        self._validate_description(description)

        effective_tenant: str = tenant_id if tenant_id else DEFAULT_TENANT_ID
        item: TodoItemDO = TodoItemDO(
            id=0,
            name=name.strip(),
            description=description,
            tenant_id=effective_tenant,
        )
        saved: TodoItemDO = self._repository.save(item)
        logger.info("todo created: id=%s tenant_id=%s", saved.id, saved.tenant_id)
        return saved

    @staticmethod
    def _validate_name(name: Optional[str]) -> None:
        """R01/R02: name 非空且长度 ≤ 100。"""
        if name is None or not name.strip():
            raise TodoBizException(
                TodoErrorCode.NAME_EMPTY, TodoErrorMessage.NAME_EMPTY
            )
        if len(name) > NAME_MAX_LENGTH:
            raise TodoBizException(
                TodoErrorCode.NAME_TOO_LONG, TodoErrorMessage.NAME_TOO_LONG
            )

    @staticmethod
    def _validate_description(description: Optional[str]) -> None:
        """R03: description 长度 ≤ 500。"""
        if description is not None and len(description) > DESCRIPTION_MAX_LENGTH:
            raise TodoBizException(
                TodoErrorCode.DESCRIPTION_TOO_LONG,
                TodoErrorMessage.DESCRIPTION_TOO_LONG,
            )
