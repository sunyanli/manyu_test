"""员工业务逻辑 — 唯一性校验、新增、列表、详情、编辑、调动、离职。"""

from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.department import Department
from app.models.employee import Employee
from app.models.transfer_record import TransferRecord
from app.utils.exceptions import ConflictException, NotFoundException, BadRequestException
from app.services.department_service import _escape_like


# ── 唯一性校验 ──

async def check_unique(
    db: AsyncSession, field: str, value: str, exclude_id: int | None = None
) -> bool:
    """检查工号或手机号是否已存在。返回 True 表示存在。"""
    if field == "employeeNo":
        col = Employee.employee_no
    elif field == "phone":
        col = Employee.phone
    else:
        raise BadRequestException("field 仅支持 employeeNo 或 phone")

    stmt = select(func.count(Employee.id)).where(col == value)
    if exclude_id is not None:
        stmt = stmt.where(Employee.id != exclude_id)
    count = await db.scalar(stmt)
    return count > 0


# ── 新增员工 ──

async def create_employee(
    db: AsyncSession,
    name: str,
    employee_no: str,
    dept_id: int,
    phone: str,
    position: str = "",
    entry_date=None,
) -> Employee:
    """新增员工，含唯一性校验 + 部门有效性校验。"""
    # 校验部门
    dept = await db.scalar(
        select(Department).where(Department.id == dept_id, Department.status == 1)
    )
    if dept is None:
        raise NotFoundException("部门不存在或已禁用")

    # 校验工号唯一
    if await check_unique(db, "employeeNo", employee_no):
        raise ConflictException("工号已存在")

    # 校验手机号唯一
    if await check_unique(db, "phone", phone):
        raise ConflictException("手机号已存在")

    emp = Employee(
        name=name,
        employee_no=employee_no,
        dept_id=dept_id,
        phone=phone,
        position=position,
        entry_date=entry_date,
    )
    db.add(emp)
    await db.flush()
    return emp


# ── 员工列表 ──

async def list_employees(
    db: AsyncSession,
    dept_id: int | None = None,
    status: int | None = None,
    keyword: str | None = None,
    page: int = 1,
    page_size: int = 20,
) -> tuple[list[Employee], int]:
    """员工列表（分页 + 筛选）。"""
    stmt = select(Employee)

    if dept_id is not None:
        # 含子部门：通过物化路径匹配
        dept = await db.get(Department, dept_id)
        if dept is None:
            return [], 0
        # 查询该部门及其所有子部门
        sub_dept_stmt = select(Department.id).where(
            (Department.id == dept_id) | (Department.path.like(_escape_like(dept.path) + "/%", escape="\\")),
            Department.status == 1,
        )
        sub_result = await db.execute(sub_dept_stmt)
        dept_ids = [row[0] for row in sub_result.all()]
        stmt = stmt.where(Employee.dept_id.in_(dept_ids))

    if status is not None:
        stmt = stmt.where(Employee.status == status)

    if keyword:
        stmt = stmt.where(
            (Employee.name.like(f"%{keyword}%"))
            | (Employee.employee_no.like(f"%{keyword}%"))
        )

    # 计数
    count_stmt = select(func.count()).select_from(stmt.subquery())
    total = await db.scalar(count_stmt) or 0

    # 分页
    stmt = stmt.offset((page - 1) * page_size).limit(page_size).order_by(Employee.id)
    result = await db.execute(stmt)
    items = result.scalars().all()

    return items, total


# ── 员工详情 ──

async def get_employee(db: AsyncSession, emp_id: int) -> Employee | None:
    return await db.get(Employee, emp_id)


# ── 编辑员工 ──

async def update_employee(
    db: AsyncSession,
    emp_id: int,
    name: str | None = None,
    phone: str | None = None,
    position: str | None = None,
) -> Employee:
    """编辑员工（部分字段）。"""
    emp = await db.get(Employee, emp_id)
    if emp is None:
        raise NotFoundException("员工不存在")
    if emp.status != 1:
        raise BadRequestException("已离职员工不可编辑")

    if name is not None:
        emp.name = name
    if phone is not None:
        if await check_unique(db, "phone", phone, exclude_id=emp_id):
            raise ConflictException("手机号已存在")
        emp.phone = phone
    if position is not None:
        emp.position = position

    await db.flush()
    return emp


# ── 人员调动 ──

async def transfer_employee(
    db: AsyncSession,
    emp_id: int,
    new_dept_id: int,
    new_position: str,
    reason: str,
    operator_id: int | None = None,
) -> Employee:
    """人员调动：事务更新 dept_id + position + 写入留痕记录。"""
    emp = await db.get(Employee, emp_id)
    if emp is None:
        raise NotFoundException("员工不存在")
    if emp.status != 1:
        raise BadRequestException("仅在职员工可调动")

    # 校验目标部门
    new_dept = await db.scalar(
        select(Department).where(Department.id == new_dept_id, Department.status == 1)
    )
    if new_dept is None:
        raise NotFoundException("目标部门不存在或已禁用")

    # 快照当前值
    old_dept_id = emp.dept_id
    old_position = emp.position

    # 更新员工
    emp.dept_id = new_dept_id
    emp.position = new_position or old_position
    await db.flush()

    # 写入留痕
    record = TransferRecord(
        employee_id=emp_id,
        from_dept_id=old_dept_id,
        to_dept_id=new_dept_id,
        from_position=old_position,
        to_position=new_position or old_position,
        reason=reason,
        operator_id=operator_id,
    )
    db.add(record)
    await db.flush()

    return emp


# ── 办理离职 ──

async def resign_employee(
    db: AsyncSession, emp_id: int, resign_date
) -> Employee:
    """办理离职：逻辑删除（status=2）。"""
    emp = await db.get(Employee, emp_id)
    if emp is None:
        raise NotFoundException("员工不存在")
    if emp.status != 1:
        raise BadRequestException("该员工已离职，无需重复操作")

    emp.status = 2
    emp.resign_date = resign_date
    await db.flush()
    return emp


# ── 调动记录查询 ──

async def list_transfer_records(
    db: AsyncSession,
    employee_id: int,
    page: int = 1,
    page_size: int = 20,
) -> tuple[list[TransferRecord], int]:
    """查询某员工的调动记录。"""
    stmt = select(TransferRecord).where(TransferRecord.employee_id == employee_id)

    count_stmt = select(func.count()).select_from(stmt.subquery())
    total = await db.scalar(count_stmt) or 0

    stmt = stmt.offset((page - 1) * page_size).limit(page_size).order_by(
        TransferRecord.created_at.desc()
    )
    result = await db.execute(stmt)
    items = result.scalars().all()

    return items, total