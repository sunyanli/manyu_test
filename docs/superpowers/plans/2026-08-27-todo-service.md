# 待办事项服务实现计划

> **For agentic workers:** 本计划完成后，使用 `superpowers:executing-plans` 或 `superpowers:subagent-driven-development` 按任务逐步执行。步骤使用 `- [ ]` 复选框语法跟踪进度。

**Goal:** 实现一个仅支持新增待办事项的 FastAPI + SQLite Web 服务。

**Architecture:** 采用 FastAPI 提供 `POST /todos` 接口；使用 SQLAlchemy ORM 定义 `TodoItem` 模型并写入 SQLite 文件；Pydantic 负责请求/响应校验；uvicorn 运行应用。

**Tech Stack:** FastAPI、SQLite、SQLAlchemy 2.x、Pydantic 2.x、uvicorn、pytest、httpx

---

## 全局约束

- 最小闭环：仅支持创建待办事项，不实现查询/编辑/删除。
- 不引入认证、权限、多用户。
- 使用 SQLite 文件数据库，便于本地运行。
- 测试使用内存/独立 SQLite 文件，避免污染开发数据库。
- 代码风格与文件职责清晰，避免单文件臃肿。

---

## 文件结构

```
todo_service/
  __init__.py    # 包入口（空或含版本）
  database.py    # 引擎、会话、依赖注入
  models.py      # SQLAlchemy TodoItem 模型
  schemas.py     # Pydantic 请求/响应模型
tests/
  test_main.py   # 创建接口测试
pyproject.toml   # 项目依赖与元数据
```

---

## Task 1: 项目依赖与数据库基础设施

**Files:**
- Create: `pyproject.toml`
- Create: `todo_service/__init__.py`
- Create: `todo_service/database.py`
- Create: `todo_service/models.py`

**Interfaces:**
- Produces: `database.engine`、`database.SessionLocal`、`database.Base`、`database.get_db()`
- Produces: `models.TodoItem`

- [ ] **Step 1: 创建 pyproject.toml**

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

- [ ] **Step 2: 安装依赖**

```bash
pip install -e ".[dev]"
```

- [ ] **Step 3: 创建 `todo_service/__init__.py`**

```python
__version__ = "0.1.0"
```

- [ ] **Step 4: 创建 `todo_service/database.py`**

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

- [ ] **Step 5: 创建 `todo_service/models.py`**

```python
from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime
from .database import Base


class TodoItem(Base):
    __tablename__ = "todos"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
```

- [ ] **Step 6: 验证表可创建**

```bash
python -c "from todo_service.database import engine; from todo_service.models import Base; Base.metadata.create_all(bind=engine); print('tables created')"
```

Expected: 输出 `tables created`，目录下生成 `todos.db`。

---

## Task 2: API 接口与数据校验

**Files:**
- Create: `todo_service/schemas.py`
- Create: `todo_service/main.py`

**Interfaces:**
- Consumes: `database.engine`、`database.get_db`、`models.TodoItem`
- Produces: `schemas.TodoCreate`、`schemas.TodoRead`
- Produces: `main.app`、`main.create_todo`

- [ ] **Step 1: 创建 `todo_service/schemas.py`**

```python
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class TodoCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=200, description="事项名称")
    description: Optional[str] = Field(None, max_length=2000, description="事项描述")


class TodoRead(TodoCreate):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True
```

- [ ] **Step 2: 创建 `todo_service/main.py`**

```python
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from . import models, schemas
from .database import engine, get_db

app = FastAPI(title="Todo Service")


@app.on_event("startup")
def startup():
    models.Base.metadata.create_all(bind=engine)


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

- [ ] **Step 3: 启动服务并手动验证接口**

```bash
uvicorn todo_service.main:app --reload
```

在另一个终端执行：

```bash
curl -X POST http://localhost:8000/todos \
  -H "Content-Type: application/json" \
  -d '{"name":"整理周报","description":"汇总本周进展"}'
```

Expected: HTTP 201，返回包含 `id`、`name`、`description`、`created_at` 的 JSON。

---

## Task 3: 接口测试

**Files:**
- Create: `tests/test_main.py`

**Interfaces:**
- Consumes: `main.app`、`database.Base`、`database.get_db`
- Produces: `test_create_todo`、`test_create_todo_missing_name`

- [ ] **Step 1: 创建 `tests/test_main.py`**

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

- [ ] **Step 2: 运行测试**

```bash
pytest tests/test_main.py -v
```

Expected: 3 个测试全部通过。

---

## 自检清单

- [ ] `POST /todos` 能正常创建记录并返回 201。
- [ ] 缺少 `name` 时返回 422。
- [ ] `description` 可选，为空时返回正常。
- [ ] 测试通过，不污染 `todos.db`。
- [ ] 无 TODO、TBD、占位符。
