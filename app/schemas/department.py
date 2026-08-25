"""部门请求/响应 Pydantic Schema。"""

from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


# ── 请求体 ──

class DepartmentCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="部门名称")
    parent_id: Optional[int] = Field(default=None, description="父部门ID，NULL 表示根节点")
    sort_order: int = Field(default=0, ge=0, description="同级排序")


class DepartmentUpdate(BaseModel):
    name: Optional[str] = Field(default=None, min_length=1, max_length=100)
    sort_order: Optional[int] = Field(default=None, ge=0)


class DepartmentMove(BaseModel):
    new_parent_id: int = Field(..., gt=0, description="新父部门ID")


# ── 响应体 ──

class DepartmentTreeNode(BaseModel):
    id: int
    name: str
    parent_id: Optional[int] = None
    level: int
    sort_order: int
    has_children: bool = False
    children: List["DepartmentTreeNode"] = []

    model_config = {"from_attributes": True}


class DepartmentInfo(BaseModel):
    id: int
    name: str
    parent_id: Optional[int] = None
    level: int
    sort_order: int
    status: int
    path: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}