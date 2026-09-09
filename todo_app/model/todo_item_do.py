"""数据对象层：TodoItemDO

内存存储中的待办事项记录结构，对应系分方案 §5.1.1.1 todo_item 表。
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional

DEFAULT_TENANT_ID: str = "default"


@dataclass
class TodoItemDO:
    """待办事项数据对象。

    Attributes:
        id: 系统自增主键。
        name: 事项名称，最大 100 字符。
        description: 事项描述，最大 500 字符，可为 None。
        tenant_id: 租户 ID，默认 "default"。
        gmt_create: 创建时间。
        gmt_modified: 修改时间。
    """

    id: int
    name: str
    description: Optional[str] = None
    tenant_id: str = DEFAULT_TENANT_ID
    gmt_create: datetime = field(default_factory=datetime.now)
    gmt_modified: datetime = field(default_factory=datetime.now)

    def to_dict(self) -> dict:
        """序列化为字典，供 Controller 返回 JSON 使用。"""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "tenant_id": self.tenant_id,
            "gmt_create": self.gmt_create.isoformat(),
            "gmt_modified": self.gmt_modified.isoformat(),
        }
