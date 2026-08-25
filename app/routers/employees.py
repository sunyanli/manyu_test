"""员工路由 — /api/employees/* 全部端点。"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user, require_admin_or_hr, require_dept_manager_or_above
from app.schemas.employee import (
    EmployeeCheckRequest,
    EmployeeCreate,
    EmployeeUpdate,
    EmployeeTransfer,
    EmployeeResign,
    EmployeeCheckResult,
    EmployeeInfo,
    EmployeeListResult,
)
from app.services import employee_service
from app.services.approval_callback import notify_approval_system

router = APIRouter(prefix="/api/employees", tags=["employees"])


@router.get("/check")
async def check_unique(
    field: str = Query(..., description="employeeNo | phone"),
    value: str = Query(..., description="待校验值"),
    db: AsyncSession = Depends(get_db),
):
    """实时唯一性校验。"""
    is_exist = await employee_service.check_unique(db, field, value)
    return {"code": 200, "data": {"isExist": is_exist}, "msg": "ok"}


@router.post("")
async def create_employee(
    body: EmployeeCreate,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """新增员工（超管/HR）。"""
    require_admin_or_hr(user)
    emp = await employee_service.create_employee(
        db,
        name=body.name,
        employee_no=body.employee_no,
        dept_id=body.dept_id,
        phone=body.phone,
        position=body.position,
        entry_date=body.entry_date,
    )
    return {"code": 200, "data": {"id": emp.id}, "msg": "新增成功"}


@router.get("")
async def list_employees(
    dept_id: int | None = Query(default=None, alias="deptId"),
    status: int | None = Query(default=None),
    keyword: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100, alias="pageSize"),
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """员工列表（分页+筛选）。"""
    require_dept_manager_or_above(user)
    items, total = await employee_service.list_employees(
        db, dept_id=dept_id, status=status, keyword=keyword, page=page, page_size=page_size
    )
    return {
        "code": 200,
        "data": {
            "items": [EmployeeInfo.model_validate(e).model_dump() for e in items],
            "total": total,
            "page": page,
            "pageSize": page_size,
        },
        "msg": "ok",
    }


@router.get("/{emp_id}")
async def get_employee(
    emp_id: int,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """员工详情。"""
    require_dept_manager_or_above(user)
    emp = await employee_service.get_employee(db, emp_id)
    if emp is None:
        return {"code": 404, "data": None, "msg": "员工不存在"}
    return {"code": 200, "data": EmployeeInfo.model_validate(emp).model_dump(), "msg": "ok"}


@router.put("/{emp_id}")
async def update_employee(
    emp_id: int,
    body: EmployeeUpdate,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """编辑员工信息。"""
    require_dept_manager_or_above(user)
    emp = await employee_service.update_employee(
        db, emp_id=emp_id, name=body.name, phone=body.phone, position=body.position
    )
    return {"code": 200, "data": EmployeeInfo.model_validate(emp).model_dump(), "msg": "编辑成功"}


@router.post("/{emp_id}/transfer")
async def transfer_employee(
    emp_id: int,
    body: EmployeeTransfer,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """人员调动（超管/HR）。"""
    require_admin_or_hr(user)
    emp = await employee_service.transfer_employee(
        db,
        emp_id=emp_id,
        new_dept_id=body.new_dept_id,
        new_position=body.new_position,
        reason=body.reason,
        operator_id=user["id"],
    )
    # 异步通知审批系统（不阻塞）
    import asyncio
    asyncio.create_task(
        notify_approval_system(
            employee_id=emp_id,
            old_dept_id=emp.dept_id,
            new_dept_id=body.new_dept_id,
            new_position=body.new_position,
        )
    )
    return {"code": 200, "data": None, "msg": "调动成功"}


@router.put("/{emp_id}/resign")
async def resign_employee(
    emp_id: int,
    body: EmployeeResign,
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """办理离职（超管/HR）。"""
    require_admin_or_hr(user)
    await employee_service.resign_employee(db, emp_id=emp_id, resign_date=body.resign_date)
    return {"code": 200, "data": None, "msg": "离职办理成功"}


@router.get("/{emp_id}/transfers")
async def list_transfer_records(
    emp_id: int,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100, alias="pageSize"),
    db: AsyncSession = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    """调动记录查询（超管/HR）。"""
    require_admin_or_hr(user)
    items, total = await employee_service.list_transfer_records(
        db, employee_id=emp_id, page=page, page_size=page_size
    )
    from app.schemas.transfer import TransferRecordInfo
    return {
        "code": 200,
        "data": {
            "items": [TransferRecordInfo.model_validate(r).model_dump() for r in items],
            "total": total,
            "page": page,
            "pageSize": page_size,
        },
        "msg": "ok",
    }