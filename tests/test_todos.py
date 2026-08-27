from fastapi.testclient import TestClient
from app import app

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
