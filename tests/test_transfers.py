"""调动与离职集成测试。"""

import pytest
from httpx import AsyncClient


async def _setup(client: AsyncClient):
    """创建测试数据：2个部门 + 1个员工。"""
    await client.post("/api/departments", json={"name": "研发部"})
    await client.post("/api/departments", json={"name": "产品部"})
    await client.post(
        "/api/employees",
        json={
            "name": "张三",
            "employeeNo": "E001",
            "deptId": 1,
            "phone": "13800000001",
            "position": "前端开发",
        },
    )


@pytest.mark.asyncio
async def test_transfer_writes_record(client: AsyncClient):
    """调动后调动记录正确写入。"""
    await _setup(client)

    await client.post(
        "/api/employees/1/transfer",
        json={"newDeptId": 2, "newPosition": "产品经理", "reason": "业务需要"},
    )

    resp = await client.get("/api/employees/1/transfers")
    data = resp.json()["data"]
    assert data["total"] == 1
    record = data["items"][0]
    assert record["from_dept_id"] == 1
    assert record["to_dept_id"] == 2
    assert record["from_position"] == "前端开发"
    assert record["to_position"] == "产品经理"
    assert record["reason"] == "业务需要"


@pytest.mark.asyncio
async def test_transfer_multiple_times(client: AsyncClient):
    """多次调动，记录按时间倒序。"""
    await _setup(client)
    await client.post("/api/departments", json={"name": "测试部"})

    # 第一次：研发部 → 产品部
    await client.post(
        "/api/employees/1/transfer",
        json={"newDeptId": 2, "newPosition": "产品", "reason": "第一次"},
    )
    # 第二次：产品部 → 测试部
    await client.post(
        "/api/employees/1/transfer",
        json={"newDeptId": 3, "newPosition": "测试", "reason": "第二次"},
    )

    resp = await client.get("/api/employees/1/transfers")
    records = resp.json()["data"]["items"]
    assert len(records) == 2
    # 最新记录在前
    assert records[0]["to_dept_id"] == 3
    assert records[1]["to_dept_id"] == 2


@pytest.mark.asyncio
async def test_resign_then_filter(client: AsyncClient):
    """离职后列表筛选正确。"""
    await _setup(client)
    await client.post(
        "/api/employees",
        json={
            "name": "李四",
            "employeeNo": "E002",
            "deptId": 1,
            "phone": "13800000002",
            "position": "后端",
        },
    )

    # 离职张三
    await client.put("/api/employees/1/resign", json={"resignDate": "2023-11-01"})

    resp = await client.get("/api/employees?status=1")
    assert resp.json()["data"]["total"] == 1
    assert resp.json()["data"]["items"][0]["name"] == "李四"

    resp = await client.get("/api/employees?status=2")
    assert resp.json()["data"]["total"] == 1
    assert resp.json()["data"]["items"][0]["name"] == "张三"


@pytest.mark.asyncio
async def test_end_to_end(client: AsyncClient):
    """端到端：创建部门树 → 新增员工 → 调动 → 离职。"""
    # 创建部门树
    await client.post("/api/departments", json={"name": "公司", "sortOrder": 0})
    await client.post("/api/departments", json={"name": "研发部", "parentId": 1})
    await client.post("/api/departments", json={"name": "前端组", "parentId": 2})

    # 验证树结构
    resp = await client.get("/api/departments/tree")
    assert len(resp.json()["data"]) == 1
    assert resp.json()["data"][0]["hasChildren"] is True

    resp = await client.get("/api/departments/tree?parentId=1")
    assert len(resp.json()["data"]) == 1
    assert resp.json()["data"][0]["name"] == "研发部"

    resp = await client.get("/api/departments/tree?parentId=2")
    assert len(resp.json()["data"]) == 1
    assert resp.json()["data"][0]["name"] == "前端组"

    # 新增员工
    resp = await client.post(
        "/api/employees",
        json={
            "name": "张三",
            "employeeNo": "EMP001",
            "deptId": 3,
            "phone": "13800000001",
            "position": "前端开发",
        },
    )
    assert resp.status_code == 200
    emp_id = resp.json()["data"]["id"]

    # 唯一性校验
    resp = await client.get("/api/employees/check?field=employeeNo&value=EMP001")
    assert resp.json()["data"]["isExist"] is True

    # 调动
    await client.post("/api/departments", json={"name": "后端组", "parentId": 2})
    resp = await client.post(
        f"/api/employees/{emp_id}/transfer",
        json={"newDeptId": 4, "newPosition": "后端开发", "reason": "业务调整"},
    )
    assert resp.status_code == 200

    # 验证调动记录
    resp = await client.get(f"/api/employees/{emp_id}/transfers")
    assert resp.json()["data"]["total"] == 1

    # 离职
    resp = await client.put(
        f"/api/employees/{emp_id}/resign", json={"resignDate": "2023-12-31"}
    )
    assert resp.status_code == 200

    # 验证离职状态
    resp = await client.get(f"/api/employees/{emp_id}")
    assert resp.json()["data"]["status"] == 2
    assert resp.json()["data"]["resign_date"] == "2023-12-31"