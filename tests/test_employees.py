"""员工管理 + 调动 + 离职单元测试。"""

import pytest
from httpx import AsyncClient


# ── 辅助函数 ──

async def _setup_departments(client: AsyncClient):
    """创建测试部门树。"""
    await client.post("/api/departments", json={"name": "研发部"})
    await client.post("/api/departments", json={"name": "产品部"})


async def _create_employee(
    client: AsyncClient,
    name="张三",
    employee_no="10086",
    dept_id=1,
    phone="13800138000",
    position="前端开发",
):
    return await client.post(
        "/api/employees",
        json={
            "name": name,
            "employeeNo": employee_no,
            "deptId": dept_id,
            "phone": phone,
            "position": position,
        },
    )


# ── 唯一性校验 ──

@pytest.mark.asyncio
async def test_check_unique_not_exist(client: AsyncClient):
    """工号不存在时返回 isExist=false。"""
    resp = await client.get("/api/employees/check?field=employeeNo&value=99999")
    assert resp.status_code == 200
    assert resp.json()["data"]["isExist"] is False


@pytest.mark.asyncio
async def test_check_unique_exist(client: AsyncClient):
    """工号已存在时返回 isExist=true。"""
    await _setup_departments(client)
    await _create_employee(client, employee_no="10086", phone="13800138000")
    resp = await client.get("/api/employees/check?field=employeeNo&value=10086")
    assert resp.json()["data"]["isExist"] is True


# ── 新增员工 ──

@pytest.mark.asyncio
async def test_create_employee(client: AsyncClient):
    """正常新增员工。"""
    await _setup_departments(client)
    resp = await _create_employee(client)
    assert resp.status_code == 200
    assert resp.json()["data"]["id"] == 1


@pytest.mark.asyncio
async def test_create_employee_duplicate_no(client: AsyncClient):
    """重复工号返回 409。"""
    await _setup_departments(client)
    await _create_employee(client, employee_no="10086", phone="13800138000")
    resp = await _create_employee(client, employee_no="10086", phone="13800138001")
    assert resp.status_code == 409
    assert "工号" in resp.json()["msg"]


@pytest.mark.asyncio
async def test_create_employee_duplicate_phone(client: AsyncClient):
    """重复手机号返回 409。"""
    await _setup_departments(client)
    await _create_employee(client, employee_no="10086", phone="13800138000")
    resp = await _create_employee(client, employee_no="10087", phone="13800138000")
    assert resp.status_code == 409
    assert "手机号" in resp.json()["msg"]


@pytest.mark.asyncio
async def test_create_employee_invalid_dept(client: AsyncClient):
    """无效部门返回 404。"""
    resp = await _create_employee(client, dept_id=999)
    assert resp.status_code == 404


@pytest.mark.asyncio
async def test_create_employee_invalid_phone_format(client: AsyncClient):
    """手机号格式错误返回 422。"""
    await _setup_departments(client)
    resp = await client.post(
        "/api/employees",
        json={
            "name": "张三",
            "employeeNo": "10086",
            "deptId": 1,
            "phone": "12345",
            "position": "前端",
        },
    )
    assert resp.status_code == 422


# ── 员工列表 ──

@pytest.mark.asyncio
async def test_list_employees(client: AsyncClient):
    """员工列表分页+筛选。"""
    await _setup_departments(client)
    await _create_employee(client, name="张三", employee_no="E001", phone="13800000001")
    await _create_employee(client, name="李四", employee_no="E002", phone="13800000002")

    resp = await client.get("/api/employees?page=1&pageSize=10")
    data = resp.json()["data"]
    assert data["total"] == 2
    assert len(data["items"]) == 2


@pytest.mark.asyncio
async def test_list_employees_status_filter(client: AsyncClient):
    """按离职状态筛选。"""
    await _setup_departments(client)
    await _create_employee(client, employee_no="E001", phone="13800000001")
    await _create_employee(client, employee_no="E002", phone="13800000002")

    # 离职一个
    await client.put("/api/employees/1/resign", json={"resignDate": "2023-11-01"})

    resp = await client.get("/api/employees?status=2")
    assert resp.json()["data"]["total"] == 1

    resp = await client.get("/api/employees?status=1")
    assert resp.json()["data"]["total"] == 1


# ── 员工详情 ──

@pytest.mark.asyncio
async def test_get_employee(client: AsyncClient):
    """员工详情。"""
    await _setup_departments(client)
    await _create_employee(client)
    resp = await client.get("/api/employees/1")
    assert resp.status_code == 200
    assert resp.json()["data"]["name"] == "张三"


# ── 编辑员工 ──

@pytest.mark.asyncio
async def test_update_employee(client: AsyncClient):
    """编辑员工信息。"""
    await _setup_departments(client)
    await _create_employee(client)
    resp = await client.put("/api/employees/1", json={"name": "张三丰"})
    assert resp.status_code == 200
    assert resp.json()["data"]["name"] == "张三丰"


# ── 人员调动 ──

@pytest.mark.asyncio
async def test_transfer_employee(client: AsyncClient):
    """正常调动 + 留痕。"""
    await _setup_departments(client)
    await _create_employee(client)

    resp = await client.post(
        "/api/employees/1/transfer",
        json={"newDeptId": 2, "newPosition": "产品经理", "reason": "业务调整"},
    )
    assert resp.status_code == 200
    assert resp.json()["msg"] == "调动成功"

    # 验证员工部门已变更
    resp = await client.get("/api/employees/1")
    assert resp.json()["data"]["dept_id"] == 2
    assert resp.json()["data"]["position"] == "产品经理"

    # 验证调动记录已写入
    resp = await client.get("/api/employees/1/transfers")
    records = resp.json()["data"]
    assert records["total"] == 1
    assert records["items"][0]["from_dept_id"] == 1
    assert records["items"][0]["to_dept_id"] == 2


@pytest.mark.asyncio
async def test_transfer_resigned_employee(client: AsyncClient):
    """离职员工不能调动。"""
    await _setup_departments(client)
    await _create_employee(client)
    await client.put("/api/employees/1/resign", json={"resignDate": "2023-11-01"})

    resp = await client.post(
        "/api/employees/1/transfer",
        json={"newDeptId": 2, "newPosition": "产品", "reason": "test"},
    )
    assert resp.status_code == 400
    assert "在职" in resp.json()["msg"]


# ── 办理离职 ──

@pytest.mark.asyncio
async def test_resign_employee(client: AsyncClient):
    """正常办理离职。"""
    await _setup_departments(client)
    await _create_employee(client)

    resp = await client.put(
        "/api/employees/1/resign", json={"resignDate": "2023-11-01"}
    )
    assert resp.status_code == 200
    assert resp.json()["msg"] == "离职办理成功"

    # 验证状态
    resp = await client.get("/api/employees/1")
    assert resp.json()["data"]["status"] == 2
    assert resp.json()["data"]["resign_date"] == "2023-11-01"


@pytest.mark.asyncio
async def test_resign_already_resigned(client: AsyncClient):
    """重复离职应拒绝。"""
    await _setup_departments(client)
    await _create_employee(client)
    await client.put("/api/employees/1/resign", json={"resignDate": "2023-11-01"})

    resp = await client.put(
        "/api/employees/1/resign", json={"resignDate": "2023-11-02"}
    )
    assert resp.status_code == 400
    assert "已离职" in resp.json()["msg"]