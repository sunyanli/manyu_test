import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from todo_service.main import app
from todo_service.database import Base, get_db

SQLALCHEMY_DATABASE_URL = "sqlite:///./test_todos.db"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db():
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)


def override_get_db(db):
    return db


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture()
def client(db):
    app.dependency_overrides[get_db] = lambda: db
    return TestClient(app)


def test_create_todo(client):
    response = client.post("/todos", json={"name": "整理周报", "description": "汇总本周工作进展"})
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "整理周报"
    assert data["description"] == "汇总本周工作进展"
    assert "id" in data
    assert "created_at" in data


def test_create_todo_without_description(client):
    response = client.post("/todos", json={"name": "整理周报"})
    assert response.status_code == 201
    assert response.json()["description"] is None


def test_create_todo_missing_name(client):
    response = client.post("/todos", json={"description": "汇总本周工作进展"})
    assert response.status_code == 422
