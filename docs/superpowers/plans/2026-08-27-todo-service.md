# Todo Service Implementation Plan

> **For agentic workers:** After this plan is complete, use `superpowers:executing-plans` or `superpowers:subagent-driven-development` to implement it task by task. Steps use `- [ ]` checkbox syntax for tracking.

**Goal:** Build a minimal FastAPI + SQLite web service that only supports creating todo items.

**Architecture:** FastAPI exposes `POST /todos`; SQLAlchemy ORM defines the `TodoItem` model and writes to a local SQLite file; Pydantic validates request/response bodies; uvicorn runs the application and a `lifespan` handler creates tables on startup.

**Tech Stack:** FastAPI, SQLite, SQLAlchemy 2.x, Pydantic 2.x, uvicorn, pytest, httpx

---

## Global Constraints

- Minimum viable scope: only create todos; no read/update/delete/list endpoints.
- No authentication, authorization, or multi-user support.
- Use a local SQLite file database so the service runs without external dependencies.
- Tests must use an isolated SQLite database and not pollute the development `todos.db`.
- Keep files focused and responsibilities clear.

---

## File Structure

```
todo_service/
  __init__.py    # package entry (version only)
  database.py    # engine, session factory, and get_db dependency
  models.py      # SQLAlchemy TodoItem model
  schemas.py     # Pydantic request/response models
tests/
  test_main.py   # POST /todos endpoint tests
pyproject.toml   # project metadata and dependencies
```

---

## Task 1: Project Dependencies and Database Infrastructure

**Files:**
- Create: `pyproject.toml`
- Create: `todo_service/__init__.py`
- Create: `todo_service/database.py`
- Create: `todo_service/models.py`

**Interfaces:**
- Produces: `database.engine`, `database.SessionLocal`, `database.Base`, `database.get_db()`
- Produces: `models.TodoItem`

- [ ] **Step 1: Create `pyproject.toml`**

```toml
[project]
name = "todo-service"
version = "0.1.0"
description = "极简待办事项 Web 服务"
requires-python = ">=3.10"
dependencies = [
    "fastapi>=0.100.0",
    "uvicorn[standard]>=0.23.0",
    "sqlalchemy>=2.0.0",
    "pydantic>=2.0.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=7.0.0",
    "httpx>=0.24.0",
]
```

- [ ] **Step 2: Install dependencies**

```bash
pip install -e ".[dev]"
```

Expected: `fastapi`, `uvicorn`, `sqlalchemy`, `pydantic`, `pytest`, and `httpx` are installed.

- [ ] **Step 3: Create `todo_service/__init__.py`**

```python
__version__ = "0.1.0"
```

- [ ] **Step 4: Create `todo_service/database.py`**

```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

SQLALCHEMY_DATABASE_URL = "sqlite:///./todos.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

- [ ] **Step 5: Create `todo_service/models.py`**

```python
from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Text, DateTime
from .database import Base


class TodoItem(Base):
    __tablename__ = "todos"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
```

- [ ] **Step 6: Verify tables can be created**

```bash
python -c "from todo_service.database import engine; from todo_service.models import Base; Base.metadata.create_all(bind=engine); print('tables created')"
```

Expected: prints `tables created` and a `todos.db` SQLite file appears in the project root.

---

## Task 2: API Endpoint and Request/Response Validation

**Files:**
- Create: `todo_service/schemas.py`
- Create: `todo_service/main.py`

**Interfaces:**
- Consumes: `database.engine`, `database.get_db`, `models.TodoItem`
- Produces: `schemas.TodoCreate`, `schemas.TodoRead`
- Produces: `main.app`, `main.create_todo`

- [ ] **Step 1: Create `todo_service/schemas.py`**

```python
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict


class TodoCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=200, description="事项名称")
    description: Optional[str] = Field(None, max_length=2000, description="事项描述")


class TodoRead(TodoCreate):
    id: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
```

- [ ] **Step 2: Create `todo_service/main.py`**

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from . import models, schemas
from .database import engine, get_db


@asynccontextmanager
async def lifespan(app: FastAPI):
    models.Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="Todo Service", lifespan=lifespan)


@app.post("/todos", response_model=schemas.TodoRead, status_code=201)
def create_todo(todo: schemas.TodoCreate, db: Session = Depends(get_db)):
    db_todo = models.TodoItem(name=todo.name, description=todo.description)
    db.add(db_todo)
    try:
        db.commit()
        db.refresh(db_todo)
    except Exception as exc:
        db.rollback()
        raise HTTPException(status_code=500, detail="数据库写入失败") from exc
    return db_todo
```

- [ ] **Step 3: Start the service and manually verify the endpoint**

```bash
uvicorn todo_service.main:app --reload
```

In another terminal:

```bash
curl -X POST http://localhost:8000/todos \
  -H "Content-Type: application/json" \
  -d '{"name":"整理周报","description":"汇总本周进展"}'
```

Expected: HTTP 201 with a JSON body containing `id`, `name`, `description`, and `created_at`.

---

## Task 3: Endpoint Tests

**Files:**
- Create: `tests/test_main.py`

**Interfaces:**
- Consumes: `main.app`, `database.Base`, `database.get_db`
- Produces: `test_create_todo`, `test_create_todo_without_description`, `test_create_todo_missing_name`

- [ ] **Step 1: Create `tests/test_main.py`**

```python
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
```

- [ ] **Step 2: Run tests**

```bash
pytest tests/test_main.py -v
```

Expected: all 3 tests pass.

---

## Self-Review Checklist

- [ ] `POST /todos` creates a record and returns 201 with `id`, `name`, `description`, and `created_at`.
- [ ] Missing or invalid `name` returns 422.
- [ ] `description` is optional and accepted as `null`.
- [ ] Tests run against an isolated `test_todos.db` and do not touch `todos.db`.
- [ ] Tables are created automatically when the application starts.
- [ ] No TODO, TBD, or placeholder text remains.
