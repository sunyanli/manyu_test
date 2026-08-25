"""部门管理单元测试。"""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_get_tree_empty(client: AsyncClient):
    """空树返回空列表。"""
    resp = await client.get("/api/departments/tree")
    assert resp.status_code == 200
    data = resp.json()
    assert data["code"] == 200
    assert data["data"] == []


@pytest.mark.asyncio
async def test_create_department(client: AsyncClient):
    """新增根部门。"""
    resp = await client.post(
        "/api/departments",
        json={"name": "研发部", "sortOrder": 0},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["code"] == 200
    assert data["data"]["id"] == 1

    # 树中应出现
    resp = await client.get("/api/departments/tree")
    nodes = resp.json()["data"]
    assert len(nodes) == 1
    assert nodes[0]["name"] == "研发部"
    assert nodes[0]["hasChildren"] is False


@pytest.mark.asyncio
async def test_create_sub_department(client: AsyncClient):
    """新增子部门后 hasChildren 应正确。"""
    # 创建根部门
    await client.post("/api/departments", json={"name": "研发部"})
    # 创建子部门
    resp = await client.post(
        "/api/departments",
        json={"name": "前端组", "parentId": 1},
    )
    assert resp.status_code == 200

    # 根部门应标记 hasChildren=true
    resp = await client.get("/api/departments/tree")
    nodes = resp.json()["data"]
    assert nodes[0]["hasChildren"] is True

    # 懒加载子部门
    resp = await client.get("/api/departments/tree?parentId=1")
    nodes = resp.json()["data"]
    assert len(nodes) == 1
    assert nodes[0]["name"] == "前端组"


@pytest.mark.asyncio
async def test_move_department_circular_ref(client: AsyncClient):
    """循环引用检测：不能将部门移动到自身或子部门下。"""
    await client.post("/api/departments", json={"name": "研发部"})
    await client.post("/api/departments", json={"name": "前端组", "parentId": 1})

    # 移动研发部到前端组下 → 循环引用，应拒绝
    resp = await client.put("/api/departments/1/move", json={"newParentId": 2})
    assert resp.status_code == 400
    assert "子部门" in resp.json()["msg"]


@pytest.mark.asyncio
async def test_move_department(client: AsyncClient):
    """正常拖拽调整父部门。"""
    await client.post("/api/departments", json={"name": "研发部"})
    await client.post("/api/departments", json={"name": "产品部"})
    await client.post("/api/departments", json={"name": "前端组", "parentId": 1})

    # 将前端组从研发部移动到产品部
    resp = await client.put("/api/departments/3/move", json={"newParentId": 2})
    assert resp.status_code == 200
    assert resp.json()["msg"] == "调整成功"

    # 验证：研发部下无子部门
    resp = await client.get("/api/departments/tree?parentId=1")
    assert resp.json()["data"] == []

    # 产品部下有前端组
    resp = await client.get("/api/departments/tree?parentId=2")
    assert len(resp.json()["data"]) == 1
    assert resp.json()["data"][0]["name"] == "前端组"


@pytest.mark.asyncio
async def test_delete_department_with_children(client: AsyncClient):
    """有子部门的部门不能删除。"""
    await client.post("/api/departments", json={"name": "研发部"})
    await client.post("/api/departments", json={"name": "前端组", "parentId": 1})

    resp = await client.delete("/api/departments/1")
    assert resp.status_code == 400
    assert "子部门" in resp.json()["msg"]


@pytest.mark.asyncio
async def test_delete_empty_department(client: AsyncClient):
    """无子部门且无员工的部门可以删除。"""
    await client.post("/api/departments", json={"name": "测试部"})

    resp = await client.delete("/api/departments/1")
    assert resp.status_code == 200
    assert resp.json()["msg"] == "删除成功"

    # 树中不再出现
    resp = await client.get("/api/departments/tree")
    assert resp.json()["data"] == []