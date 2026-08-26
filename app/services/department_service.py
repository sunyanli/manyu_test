"""部门业务逻辑 — 树查询、移动、CRUD。"""

from sqlalchemy import select, func, update, delete
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.models.department import Department
from app.utils.exceptions import BadRequestException, NotFoundException


async def get_tree(
    db: AsyncSession, parent_id: int | None = None
) -> list[dict]:
    """懒加载获取子部门列表，含 hasChildren 标记。"""
    if parent_id is None or parent_id == 0:
        stmt = select(Department).where(Department.parent_id.is_(None))
    else:
        stmt = select(Department).where(Department.parent_id == parent_id)

    stmt = stmt.where(Department.status == 1).order_by(Department.sort_order)
    result = await db.execute(stmt)
    depts = result.scalars().all()

    # 批量查询子节点计数，避免 N+1
    dept_ids = [d.id for d in depts]
    has_children_map: dict[int, bool] = {}
    if dept_ids:
        child_counts_stmt = (
            select(Department.parent_id, func.count(Department.id))
            .where(
                Department.parent_id.in_(dept_ids),
                Department.status == 1,
            )
            .group_by(Department.parent_id)
        )
        child_counts_result = await db.execute(child_counts_stmt)
        has_children_map = {row[0]: row[1] > 0 for row in child_counts_result.all()}

    nodes = []
    for d in depts:
        has_children = has_children_map.get(d.id, False)

        nodes.append(
            {
                "id": d.id,
                "name": d.name,
                "parentId": d.parent_id,
                "level": d.level,
                "sortOrder": d.sort_order,
                "hasChildren": has_children,
                "children": [],
            }
        )
    return nodes


async def create_department(
    db: AsyncSession, name: str, parent_id: int | None, sort_order: int
) -> Department:
    """新增部门，自动计算 level 和 path。"""
    level = 1
    parent_path = ""

    if parent_id is not None:
        parent = await _get_active_dept(db, parent_id)
        if parent is None:
            raise NotFoundException("父部门不存在或已禁用")
        level = parent.level + 1
        if level > settings.max_dept_level:
            raise BadRequestException(f"部门层级超过最大限制({settings.max_dept_level}层)")
        parent_path = parent.path

    dept = Department(
        name=name,
        parent_id=parent_id,
        level=level,
        path="",  # 先占位，flush 获取 id 后再设置正确路径
        sort_order=sort_order,
    )
    db.add(dept)
    await db.flush()
    # 使用自身 id 构建物化路径
    dept.path = f"{parent_path}/{dept.id}" if parent_id else f"/{dept.id}"
    await db.flush()
    return dept


async def update_department(
    db: AsyncSession, dept_id: int, name: str | None, sort_order: int | None
) -> Department:
    """编辑部门名称和排序。"""
    dept = await _get_active_dept(db, dept_id)
    if dept is None:
        raise NotFoundException("部门不存在或已禁用")

    if name is not None:
        dept.name = name
    if sort_order is not None:
        dept.sort_order = sort_order
    await db.flush()
    return dept


async def move_department(
    db: AsyncSession, dept_id: int, new_parent_id: int
) -> None:
    """拖拽调整父部门，含循环引用 + 深度校验。"""
    dept = await _get_active_dept(db, dept_id)
    if dept is None:
        raise NotFoundException("部门不存在或已禁用")

    new_parent = await _get_active_dept(db, new_parent_id)
    if new_parent is None:
        raise NotFoundException("目标父部门不存在或已禁用")

    # 循环引用检测：new_parent_id 不能是 dept_id 自身或其子孙
    if new_parent_id == dept_id:
        raise BadRequestException("不能将部门移动到自身下")
    if new_parent.path.startswith(dept.path + "/") or new_parent.path == dept.path:
        raise BadRequestException("不能将部门移动到其子部门下")

    # 深度校验：移动后层级 = new_parent.level + 1 + (子树深度)
    subtree_depth = await _calc_subtree_depth(db, dept_id)
    new_level = new_parent.level + 1
    if new_level + subtree_depth - 1 > settings.max_dept_level:
        raise BadRequestException(f"移动后层级将超过最大限制({settings.max_dept_level}层)")

    old_path = dept.path
    old_level = dept.level

    # 更新自身
    new_path = f"{new_parent.path}/{dept_id}"
    dept.parent_id = new_parent_id
    dept.level = new_level
    dept.path = new_path
    await db.flush()

    # 级联更新所有子孙节点的 path 和 level
    await _cascade_update_children(db, old_path, new_path, new_level - old_level)


async def delete_department(db: AsyncSession, dept_id: int) -> None:
    """软删除部门（status=0），校验无子部门、无在职员工。"""
    dept = await _get_active_dept(db, dept_id)
    if dept is None:
        raise NotFoundException("部门不存在或已禁用")

    # 校验无子部门
    child_count = await db.scalar(
        select(func.count(Department.id)).where(
            Department.parent_id == dept_id, Department.status == 1
        )
    )
    if child_count and child_count > 0:
        raise BadRequestException("该部门下存在子部门，无法删除")

    # 校验无在职员工
    from app.models.employee import Employee
    emp_count = await db.scalar(
        select(func.count(Employee.id)).where(
            Employee.dept_id == dept_id, Employee.status == 1
        )
    )
    if emp_count and emp_count > 0:
        raise BadRequestException("该部门下存在在职员工，无法删除")

    dept.status = 0
    await db.flush()


async def get_department_by_id(db: AsyncSession, dept_id: int) -> Department | None:
    return await db.get(Department, dept_id)


# ── 内部辅助 ──

async def _get_active_dept(db: AsyncSession, dept_id: int) -> Department | None:
    result = await db.execute(
        select(Department).where(Department.id == dept_id, Department.status == 1)
    )
    return result.scalar_one_or_none()


async def _calc_subtree_depth(db: AsyncSession, dept_id: int) -> int:
    """计算某个部门子树的最大深度（包含自身）。"""
    dept = await db.get(Department, dept_id)
    if dept is None:
        return 0

    # 查找所有以 dept.path 为前缀的子孙，取最大 level
    result = await db.execute(
        select(func.max(Department.level)).where(
            Department.path.like(f"{dept.path}/%"), Department.status == 1
        )
    )
    max_level = result.scalar()
    if max_level is None:
        return 1
    return max_level - dept.level + 1


async def _cascade_update_children(
    db: AsyncSession,
    old_path: str,
    new_path: str,
    level_delta: int,
) -> None:
    """更新所有子孙节点的 path 和 level。"""
    # 使用 SQL 原生更新：将 path 中 old_path 前缀替换为 new_path
    from app.models.department import Department

    # 查找所有 path 以 old_path/ 开头的子孙
    result = await db.execute(
        select(Department).where(
            Department.path.like(f"{old_path}/%"), Department.status == 1
        )
    )
    children = result.scalars().all()
    for child in children:
        child.path = new_path + child.path[len(old_path):]
        child.level = child.level + level_delta
    await db.flush()