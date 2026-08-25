"""部门路由 — /api/departments/* 全部端点。"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user, require_admin_or_hr, require_dept_manager_or_above
from app.schemas.department import (
    DepartmentCreate,
    DepartmentInfo,
    DepartmentMove,
    DepartmentUpdate,
)
from app.services import department_service

router = APIRouter(prefix="/api/departments", tags=["departments"])


@router.get("/tree")
async def get_tree(
    parent_id: int | None = Query(default=None, alias="parentId", description="父部门ID，0或空=根节点"),
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """懒加载获取部门树。"""
    require_dept_manager_or_above(user)
    nodes = await department_service.get_tree(db, parent_id)
    return {"code": 200, "data": nodes, "msg": "ok"}


@router.post("")
async def create_department(
    body: DepartmentCreate,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """新增部门（超管/HR）。"""
    require_admin_or_hr(user)
    dept = await department_service.create_department(
        db, name=body.name, parent_id=body.parent_id, sort_order=body.sort_order
    )
    return {"code": 200, "data": {"id": dept.id}, "msg": "新增成功"}


@router.put("/{dept_id}")
async def update_department(
    dept_id: int,
    body: DepartmentUpdate,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """编辑部门（超管/HR）。"""
    require_admin_or_hr(user)
    await department_service.update_department(
        db, dept_id=dept_id, name=body.name, sort_order=body.sort_order
    )
    return {"code": 200, "data": None, "msg": "编辑成功"}


@router.put("/{dept_id}/move")
async def move_department(
    dept_id: int,
    body: DepartmentMove,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """拖拽调整父部门（超管/HR）。"""
    require_admin_or_hr(user)
    await department_service.move_department(db, dept_id=dept_id, new_parent_id=body.new_parent_id)
    return {"code": 200, "data": None, "msg": "调整成功"}


@router.delete("/{dept_id}")
async def delete_department(
    dept_id: int,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """软删除部门（超管/HR）。"""
    require_admin_or_hr(user)
    await department_service.delete_department(db, dept_id=dept_id)
    return {"code": 200, "data": None, "msg": "删除成功"}