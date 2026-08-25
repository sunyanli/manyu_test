"""角色鉴权中间件 — MVP 阶段通过请求头 X-User-Id / X-User-Role 注入。"""

from enum import IntEnum

from fastapi import Header
from typing import Optional

from app.utils.exceptions import ForbiddenException


class Role(IntEnum):
    SUPER_ADMIN = 1  # 超管
    HR = 2  # HR
    DEPT_MANAGER = 3  # 部门主管


async def get_current_user(
    x_user_id: Optional[int] = Header(default=None, alias="X-User-Id"),
    x_user_role: Optional[int] = Header(default=None, alias="X-User-Role"),
) -> dict:
    """解析当前用户身份，返回 {id, role}。无身份时默认超管（MVP 简化）。"""
    if x_user_id is None:
        x_user_id = 0
    if x_user_role is None:
        x_user_role = Role.SUPER_ADMIN
    return {"id": x_user_id, "role": Role(x_user_role)}


def require_admin_or_hr(user: dict) -> dict:
    """要求超管或 HR 角色。"""
    if user["role"] not in (Role.SUPER_ADMIN, Role.HR):
        raise ForbiddenException("仅超管/HR可执行此操作")
    return user


def require_dept_manager_or_above(user: dict) -> dict:
    """要求至少部门主管权限。"""
    if user["role"] not in (Role.SUPER_ADMIN, Role.HR, Role.DEPT_MANAGER):
        raise ForbiddenException("无权访问")
    return user