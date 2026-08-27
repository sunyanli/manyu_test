from fastapi.testclient import TestClient
import sqlite3

from app import DB_PATH, app

client = TestClient(app)


def test_create_todo_success():
    response = client.post(
        "/api/todos",
        json={"name": "完成周报", "description": "整理本周工作产出"},
    )
    assert response.status_code == 201
    data = response.json()
    assert data["success"] is True
    assert data["data"]["name"] == "完成周报"
    assert data["data"]["description"] == "整理本周工作产出"
    assert "id" in data["data"]
    assert "created_at" in data["data"]

    with sqlite3.connect(DB_PATH) as conn:
        row = conn.execute(
            "SELECT id, name, description, created_at FROM todos WHERE id = ?",
            (data["data"]["id"],),
        ).fetchone()
    assert row == (
        data["data"]["id"],
        "完成周报",
        "整理本周工作产出",
        data["data"]["created_at"],
    )


def test_create_todo_without_description():
    response = client.post("/api/todos", json={"name": "无描述待办"})
    assert response.status_code == 201
    data = response.json()
    assert data["success"] is True
    assert data["data"]["description"] is None


def test_create_todo_name_boundary_lengths():
    response = client.post("/api/todos", json={"name": "x" * 100})
    assert response.status_code == 201
    assert response.json()["data"]["name"] == "x" * 100

    response = client.post("/api/todos", json={"name": "x" * 101})
    assert response.status_code == 400
    assert response.json()["error_code"] == "ERR_TODO_001"


def test_create_todo_description_boundary_lengths():
    response = client.post(
        "/api/todos", json={"name": "边界描述", "description": "x" * 500}
    )
    assert response.status_code == 201
    assert response.json()["data"]["description"] == "x" * 500

    response = client.post(
        "/api/todos", json={"name": "边界描述", "description": "x" * 501}
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == "ERR_TODO_002"


def test_create_todo_missing_name_uses_unified_validation_error():
    response = client.post("/api/todos", json={"description": "缺少名称"})
    assert response.status_code == 422
    data = response.json()
    assert data["success"] is False
    assert data["error_code"] == "ERR_TODO_004"
    assert data["message"]
    assert isinstance(data["detail"], list)


def test_create_todo_name_empty():
    response = client.post("/api/todos", json={"name": "", "description": "描述"})
    assert response.status_code == 400
    data = response.json()
    assert data["success"] is False
    assert data["error_code"] == "ERR_TODO_001"


def test_create_todo_name_too_long():
    response = client.post("/api/todos", json={"name": "x" * 101})
    assert response.status_code == 400
    assert response.json()["error_code"] == "ERR_TODO_001"


def test_create_todo_description_too_long():
    response = client.post(
        "/api/todos", json={"name": "有效名称", "description": "x" * 501}
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == "ERR_TODO_002"
