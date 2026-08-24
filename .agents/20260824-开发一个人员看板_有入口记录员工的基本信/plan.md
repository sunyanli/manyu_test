# 人员看板系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 开发一个人员看板系统，支持员工基本信息管理（CRUD）、数据导入（单条/批量）、成本预算管理及白名单机制。

**Architecture:** 前后端分离架构。后端（manyu_test）使用 FastAPI + SQLAlchemy + SQLite 提供 RESTful API；前端（manyu_test1）使用 Vue 3 + Vite + Element Plus 构建独立 SPA 页面。前后端通过 HTTP REST API 通信，接口契约在 dima.md 中定义。

**Tech Stack:**
- 后端: Python 3.10+ / FastAPI / SQLAlchemy / SQLite / pandas (CSV/Excel 解析)
- 前端: Vue 3 (Composition API) / Vite / Element Plus / Axios / XLSX (Excel 解析)
- 工具: pytest / uvicorn / Vitest

---

## Global Constraints

- 前后端仓库分离：manyu_test → 后端代码，manyu_test1 → 前端代码
- 接口契约严格对齐 dima.md 中的 API 设计，不得随意修改路径/请求/响应格式
- 数据库使用 SQLite，文件位于后端项目根目录
- 所有新增字段必须向后兼容（仅新增，不删除/重命名已有字段）
- 禁止使用任何需要额外付费许可的第三方库
- CSV 导入编码统一为 UTF-8，支持 BOM 头
- 白名单校验在 API 层统一拦截，不分散到各业务逻辑中
- 每个任务完成后必须运行对应测试，测试通过方可进入下一任务

---

## File Structure

### 后端 (manyu_test)

```
/workspace/manyu_test/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI 应用入口，CORS 配置
│   ├── database.py              # SQLAlchemy 引擎和会话
│   ├── models/
│   │   ├── __init__.py
│   │   ├── employee.py          # Employee ORM 模型
│   │   ├── budget.py            # Budget ORM 模型
│   │   └── whitelist.py         # Whitelist ORM 模型
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── employee.py          # Pydantic 员工请求/响应 schema
│   │   ├── budget.py            # Pydantic 预算请求/响应 schema
│   │   └── whitelist.py         # Pydantic 白名单请求/响应 schema
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── employees.py         # /api/employees 路由
│   │   ├── import_api.py        # /api/import 路由
│   │   ├── budgets.py           # /api/budgets 路由
│   │   └── whitelist.py         # /api/whitelist 路由
│   └── services/
│       ├── __init__.py
│       ├── employee_service.py  # 员工业务逻辑
│       ├── import_service.py    # 导入解析逻辑
│       ├── budget_service.py    # 预算业务逻辑
│       └── whitelist_service.py # 白名单校验逻辑
├── requirements.txt
└── tests/
    ├── __init__.py
    ├── conftest.py              # 测试用数据库会话 fixture
    ├── test_employees.py
    ├── test_import.py
    ├── test_budgets.py
    └── test_whitelist.py
```

### 前端 (manyu_test1)

```
/workspace/manyu_test1/
├── src/
│   ├── main.js                  # Vue 应用入口
│   ├── App.vue                  # 根组件（布局/路由）
│   ├── api/
│   │   ├── request.js           # Axios 实例封装
│   │   ├── employees.js         # 员工 API 调用
│   │   ├── budgets.js           # 预算 API 调用
│   │   └── whitelist.js         # 白名单 API 调用
│   ├── views/
│   │   ├── EmployeeList.vue     # 员工列表页（含搜索/分页/CRUD 弹窗）
│   │   ├── EmployeeImport.vue   # 导入页面（单条录入 + 批量导入）
│   │   ├── BudgetList.vue       # 成本预算管理页
│   │   └── WhitelistManager.vue # 白名单管理页
│   ├── components/
│   │   ├── EmployeeForm.vue     # 员工表单弹窗组件（新增/编辑复用）
│   │   ├── ImportDialog.vue     # 批量导入弹窗组件
│   │   └── BudgetForm.vue       # 预算表单弹窗组件
│   ├── router/
│   │   └── index.js             # Vue Router 路由配置
│   └── styles/
│       └── global.css           # 全局样式
├── index.html
├── package.json
├── vite.config.js
└── vitest.config.js
```

---

## Task 1: 后端项目脚手架搭建 (manyu_test)

**Files:**
- Create: `app/__init__.py`
- Create: `app/main.py`
- Create: `app/database.py`
- Create: `requirements.txt`

**Interfaces:**
- Consumes: 无
- Produces: FastAPI 应用实例，可 `uvicorn app.main:app` 启动

- [ ] **Step 1: 创建项目目录结构**

```bash
mkdir -p app/models app/schemas app/routers app/services tests
touch app/__init__.py app/models/__init__.py app/schemas/__init__.py app/routers/__init__.py app/services/__init__.py tests/__init__.py
```

- [ ] **Step 2: 创建 requirements.txt**

```txt
fastapi==0.104.1
uvicorn[standard]==0.24.0
sqlalchemy==2.0.23
pandas==2.1.4
openpyxl==3.1.2
pytest==7.4.3
httpx==0.25.2
```

- [ ] **Step 3: 创建 app/database.py**

```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase

SQLALCHEMY_DATABASE_URL = "sqlite:///./employee_dashboard.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

- [ ] **Step 4: 创建 app/main.py**

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.database import engine, Base

app = FastAPI(
    title="人员看板系统 API",
    description="员工信息管理、导入、成本预算、白名单管理",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)


@app.get("/api/health")
def health_check():
    return {"status": "ok", "version": "1.0.0"}
```

- [ ] **Step 5: 验证项目可启动**

```bash
cd /workspace/manyu_test
pip install -r requirements.txt -q
uvicorn app.main:app --host 0.0.0.0 --port 8000 &
sleep 2
curl -s http://localhost:8000/api/health
# 期望输出: {"status":"ok","version":"1.0.0"}
kill %1 2>/dev/null
```

---

## Task 2: 后端数据模型定义 (manyu_test)

**Files:**
- Create: `app/models/employee.py`
- Create: `app/models/budget.py`
- Create: `app/models/whitelist.py`
- Modify: `app/database.py` (已就绪，无需修改)

**Interfaces:**
- Consumes: `app.database.Base`
- Produces: `Employee`, `Budget`, `Whitelist` ORM 模型类

- [ ] **Step 1: 创建 app/models/employee.py**

```python
from datetime import date, datetime
from sqlalchemy import String, Integer, Date, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base


class Employee(Base):
    __tablename__ = "employees"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    employee_id: Mapped[str] = mapped_column(String(32), unique=True, nullable=False, comment="员工工号")
    name: Mapped[str] = mapped_column(String(64), nullable=False, comment="姓名")
    department: Mapped[str] = mapped_column(String(128), nullable=True, comment="部门")
    position: Mapped[str] = mapped_column(String(128), nullable=True, comment="职位")
    phone: Mapped[str] = mapped_column(String(20), nullable=True, comment="手机号")
    email: Mapped[str] = mapped_column(String(128), nullable=True, comment="邮箱")
    hire_date: Mapped[date] = mapped_column(Date, nullable=True, comment="入职日期")
    status: Mapped[str] = mapped_column(String(16), default="在职", comment="在职/离职")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, comment="创建时间")
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now, comment="更新时间")
```

- [ ] **Step 2: 创建 app/models/budget.py**

```python
from datetime import datetime
from decimal import Decimal
from sqlalchemy import String, Integer, Decimal, Text, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base


class Budget(Base):
    __tablename__ = "budgets"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    employee_id: Mapped[str] = mapped_column(String(32), ForeignKey("employees.employee_id"), nullable=False, comment="关联员工工号")
    budget_year: Mapped[int] = mapped_column(Integer, nullable=False, comment="预算年份")
    budget_amount: Mapped[Decimal] = mapped_column(Decimal(12, 2), nullable=False, comment="预算金额")
    actual_amount: Mapped[Decimal] = mapped_column(Decimal(12, 2), default=0.00, comment="实际支出")
    description: Mapped[str] = mapped_column(Text, nullable=True, comment="预算说明")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, comment="创建时间")
```

- [ ] **Step 3: 创建 app/models/whitelist.py**

```python
from datetime import datetime
from sqlalchemy import String, Integer, Boolean, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base


class Whitelist(Base):
    __tablename__ = "whitelist"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    employee_id: Mapped[str] = mapped_column(String(32), unique=True, nullable=True, comment="员工工号（NULL 表示部门级白名单）")
    department: Mapped[str] = mapped_column(String(128), nullable=True, comment="部门名")
    whitelist_type: Mapped[str] = mapped_column(String(16), nullable=False, default="all", comment="import / budget / all")
    enabled: Mapped[bool] = mapped_column(Boolean, default=True, comment="是否启用")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, comment="创建时间")
```

- [ ] **Step 4: 验证模型可创建表**

```python
# 运行: python -c "from app.database import engine, Base; from app.models.employee import Employee; from app.models.budget import Budget; from app.models.whitelist import Whitelist; Base.metadata.create_all(bind=engine); print('Tables created OK')"
# 期望输出: Tables created OK
```

---

## Task 3: 后端 Pydantic Schema 定义 (manyu_test)

**Files:**
- Create: `app/schemas/employee.py`
- Create: `app/schemas/budget.py`
- Create: `app/schemas/whitelist.py`

**Interfaces:**
- Consumes: 无直接依赖
- Produces: `EmployeeCreate`, `EmployeeUpdate`, `EmployeeResponse`, `BudgetCreate`, `BudgetUpdate`, `BudgetResponse`, `WhitelistCreate`, `WhitelistResponse` schema 类

- [ ] **Step 1: 创建 app/schemas/employee.py**

```python
from datetime import date, datetime
from typing import Optional, List
from pydantic import BaseModel, Field


class EmployeeBase(BaseModel):
    employee_id: str = Field(..., max_length=32, description="员工工号")
    name: str = Field(..., max_length=64, description="姓名")
    department: Optional[str] = Field(None, max_length=128, description="部门")
    position: Optional[str] = Field(None, max_length=128, description="职位")
    phone: Optional[str] = Field(None, max_length=20, description="手机号")
    email: Optional[str] = Field(None, max_length=128, description="邮箱")
    hire_date: Optional[date] = Field(None, description="入职日期")
    status: str = Field("在职", max_length=16, description="在职/离职")


class EmployeeCreate(EmployeeBase):
    pass


class EmployeeUpdate(BaseModel):
    name: Optional[str] = Field(None, max_length=64)
    department: Optional[str] = Field(None, max_length=128)
    position: Optional[str] = Field(None, max_length=128)
    phone: Optional[str] = Field(None, max_length=20)
    email: Optional[str] = Field(None, max_length=128)
    hire_date: Optional[date] = None
    status: Optional[str] = Field(None, max_length=16)


class EmployeeResponse(EmployeeBase):
    id: int
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class EmployeeListResponse(BaseModel):
    total: int
    items: List[EmployeeResponse]
    page: int
    page_size: int
```

- [ ] **Step 2: 创建 app/schemas/budget.py**

```python
from datetime import datetime
from decimal import Decimal
from typing import Optional, List
from pydantic import BaseModel, Field


class BudgetBase(BaseModel):
    employee_id: str = Field(..., max_length=32, description="关联员工工号")
    budget_year: int = Field(..., ge=2000, le=2099, description="预算年份")
    budget_amount: Decimal = Field(..., decimal_places=2, description="预算金额")
    actual_amount: Optional[Decimal] = Field(0.00, decimal_places=2, description="实际支出")
    description: Optional[str] = Field(None, description="预算说明")


class BudgetCreate(BudgetBase):
    pass


class BudgetUpdate(BaseModel):
    budget_amount: Optional[Decimal] = Field(None, decimal_places=2)
    actual_amount: Optional[Decimal] = Field(None, decimal_places=2)
    description: Optional[str] = None


class BudgetResponse(BudgetBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True


class BudgetListResponse(BaseModel):
    total: int
    items: List[BudgetResponse]
    page: int
    page_size: int
```

- [ ] **Step 3: 创建 app/schemas/whitelist.py**

```python
from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field


class WhitelistBase(BaseModel):
    employee_id: Optional[str] = Field(None, max_length=32, description="员工工号（NULL 表示部门级白名单）")
    department: Optional[str] = Field(None, max_length=128, description="部门名")
    whitelist_type: str = Field("all", max_length=16, description="import / budget / all")
    enabled: bool = Field(True, description="是否启用")


class WhitelistCreate(WhitelistBase):
    pass


class WhitelistResponse(WhitelistBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True


class WhitelistListResponse(BaseModel):
    total: int
    items: List[WhitelistResponse]
```

---

## Task 4: 后端员工 CRUD API (manyu_test)

**Files:**
- Create: `app/services/employee_service.py`
- Create: `app/routers/employees.py`
- Modify: `app/main.py` (注册路由)
- Create: `tests/conftest.py`
- Create: `tests/test_employees.py`

**Interfaces:**
- Consumes: `Employee`, `EmployeeCreate`, `EmployeeUpdate`, `EmployeeResponse`, `EmployeeListResponse`
- Produces: `GET/POST/PUT/DELETE /api/employees` 端点

- [ ] **Step 1: 创建 tests/conftest.py**

```python
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.database import Base, get_db
from app.main import app

SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


@pytest.fixture(autouse=True)
def setup_db():
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture
def client():
    return TestClient(app)
```

- [ ] **Step 2: 创建 app/services/employee_service.py**

```python
from typing import Optional, Tuple, List
from sqlalchemy.orm import Session
from sqlalchemy import or_
from app.models.employee import Employee
from app.schemas.employee import EmployeeCreate, EmployeeUpdate


def list_employees(
    db: Session,
    page: int = 1,
    page_size: int = 20,
    search: Optional[str] = None,
    department: Optional[str] = None,
    status: Optional[str] = None,
) -> Tuple[List[Employee], int]:
    query = db.query(Employee)
    if search:
        query = query.filter(
            or_(
                Employee.name.ilike(f"%{search}%"),
                Employee.employee_id.ilike(f"%{search}%"),
                Employee.department.ilike(f"%{search}%"),
            )
        )
    if department:
        query = query.filter(Employee.department == department)
    if status:
        query = query.filter(Employee.status == status)
    total = query.count()
    employees = query.order_by(Employee.id.desc()).offset((page - 1) * page_size).limit(page_size).all()
    return employees, total


def get_employee(db: Session, employee_id: int) -> Optional[Employee]:
    return db.query(Employee).filter(Employee.id == employee_id).first()


def get_employee_by_emp_no(db: Session, emp_no: str) -> Optional[Employee]:
    return db.query(Employee).filter(Employee.employee_id == emp_no).first()


def create_employee(db: Session, data: EmployeeCreate) -> Employee:
    employee = Employee(**data.model_dump())
    db.add(employee)
    db.commit()
    db.refresh(employee)
    return employee


def update_employee(db: Session, employee_id: int, data: EmployeeUpdate) -> Optional[Employee]:
    employee = get_employee(db, employee_id)
    if not employee:
        return None
    update_data = data.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(employee, key, value)
    db.commit()
    db.refresh(employee)
    return employee


def delete_employee(db: Session, employee_id: int) -> bool:
    employee = get_employee(db, employee_id)
    if not employee:
        return False
    db.delete(employee)
    db.commit()
    return True
```

- [ ] **Step 3: 创建 app/routers/employees.py**

```python
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.employee import (
    EmployeeCreate, EmployeeUpdate, EmployeeResponse, EmployeeListResponse,
)
from app.services import employee_service

router = APIRouter(prefix="/api/employees", tags=["员工管理"])


@router.get("", response_model=EmployeeListResponse)
def list_employees(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    search: Optional[str] = Query(None),
    department: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    db: Session = Depends(get_db),
):
    employees, total = employee_service.list_employees(db, page, page_size, search, department, status)
    return EmployeeListResponse(
        total=total,
        items=[EmployeeResponse.model_validate(emp) for emp in employees],
        page=page,
        page_size=page_size,
    )


@router.get("/{employee_id}", response_model=EmployeeResponse)
def get_employee(employee_id: int, db: Session = Depends(get_db)):
    employee = employee_service.get_employee(db, employee_id)
    if not employee:
        raise HTTPException(status_code=404, detail="员工不存在")
    return EmployeeResponse.model_validate(employee)


@router.post("", response_model=EmployeeResponse, status_code=201)
def create_employee(data: EmployeeCreate, db: Session = Depends(get_db)):
    existing = employee_service.get_employee_by_emp_no(db, data.employee_id)
    if existing:
        raise HTTPException(status_code=409, detail="员工工号已存在")
    employee = employee_service.create_employee(db, data)
    return EmployeeResponse.model_validate(employee)


@router.put("/{employee_id}", response_model=EmployeeResponse)
def update_employee(employee_id: int, data: EmployeeUpdate, db: Session = Depends(get_db)):
    employee = employee_service.update_employee(db, employee_id, data)
    if not employee:
        raise HTTPException(status_code=404, detail="员工不存在")
    return EmployeeResponse.model_validate(employee)


@router.delete("/{employee_id}", status_code=204)
def delete_employee(employee_id: int, db: Session = Depends(get_db)):
    success = employee_service.delete_employee(db, employee_id)
    if not success:
        raise HTTPException(status_code=404, detail="员工不存在")
    return None
```

- [ ] **Step 4: 修改 app/main.py 注册路由**

在 `app = FastAPI(...)` 之后、`app.add_middleware(...)` 之前或之后，添加：

```python
from app.routers import employees, budgets, whitelist, import_api

app.include_router(employees.router)
# budgets / whitelist / import_api 路由在后续任务中注册
```

- [ ] **Step 5: 创建 tests/test_employees.py**

```python
import pytest
from app.schemas.employee import EmployeeCreate, EmployeeUpdate


def test_create_employee(client):
    response = client.post("/api/employees", json={
        "employee_id": "EMP001",
        "name": "张三",
        "department": "技术部",
        "position": "工程师",
        "phone": "13800138001",
        "email": "zhangsan@example.com",
    })
    assert response.status_code == 201
    data = response.json()
    assert data["employee_id"] == "EMP001"
    assert data["name"] == "张三"
    assert data["id"] > 0


def test_create_duplicate_employee_id(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    response = client.post("/api/employees", json={"employee_id": "EMP001", "name": "李四"})
    assert response.status_code == 409


def test_list_employees(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    client.post("/api/employees", json={"employee_id": "EMP002", "name": "李四"})
    response = client.get("/api/employees?page=1&page_size=10")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 2
    assert len(data["items"]) == 2


def test_get_employee(client):
    resp = client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    emp_id = resp.json()["id"]
    response = client.get(f"/api/employees/{emp_id}")
    assert response.status_code == 200
    assert response.json()["name"] == "张三"


def test_get_employee_not_found(client):
    response = client.get("/api/employees/999")
    assert response.status_code == 404


def test_update_employee(client):
    resp = client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    emp_id = resp.json()["id"]
    response = client.put(f"/api/employees/{emp_id}", json={"name": "张三（更新）", "position": "高级工程师"})
    assert response.status_code == 200
    assert response.json()["name"] == "张三（更新）"


def test_delete_employee(client):
    resp = client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    emp_id = resp.json()["id"]
    response = client.delete(f"/api/employees/{emp_id}")
    assert response.status_code == 204
    response = client.get(f"/api/employees/{emp_id}")
    assert response.status_code == 404


def test_search_employees(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三", "department": "技术部"})
    client.post("/api/employees", json={"employee_id": "EMP002", "name": "李四", "department": "市场部"})
    response = client.get("/api/employees?search=张三")
    assert response.status_code == 200
    assert response.json()["total"] == 1
```

- [ ] **Step 6: 运行测试**

```bash
cd /workspace/manyu_test
pytest tests/test_employees.py -v
# 期望: 所有测试 PASS
```

---

## Task 5: 后端导入 API (manyu_test)

**Files:**
- Create: `app/services/import_service.py`
- Create: `app/routers/import_api.py`
- Modify: `app/main.py` (注册 import 路由)
- Create: `tests/test_import.py`

**Interfaces:**
- Consumes: `Employee`, `EmployeeCreate`, `employee_service`
- Produces: `POST /api/import/employees`, `GET /api/import/template`

- [ ] **Step 1: 创建 app/services/import_service.py**

```python
import io
import csv
from typing import List, Tuple, Dict
from sqlalchemy.orm import Session
from app.models.employee import Employee
from app.schemas.employee import EmployeeCreate
from app.services import employee_service


def parse_csv(content: bytes) -> List[Dict[str, str]]:
    text = content.decode("utf-8-sig")
    reader = csv.DictReader(io.StringIO(text))
    records = []
    for row in reader:
        # 标准化字段名
        record = {}
        for k, v in row.items():
            key = k.strip().lower().replace(" ", "_").replace("（", "(").replace("）", ")")
            record[key] = v.strip() if v else ""
        records.append(record)
    return records


def import_employees(db: Session, records: List[Dict[str, str]]) -> Tuple[int, int, List[str]]:
    success_count = 0
    error_count = 0
    errors = []

    for i, record in enumerate(records):
        try:
            employee_id = record.get("employee_id") or record.get("工号") or ""
            name = record.get("name") or record.get("姓名") or ""
            department = record.get("department") or record.get("部门") or ""
            position = record.get("position") or record.get("职位") or ""

            if not employee_id or not name:
                error_count += 1
                errors.append(f"第{i+2}行: 缺少必填字段（工号/姓名）")
                continue

            existing = employee_service.get_employee_by_emp_no(db, employee_id)
            if existing:
                error_count += 1
                errors.append(f"第{i+2}行: 工号 {employee_id} 已存在")
                continue

            data = EmployeeCreate(
                employee_id=employee_id,
                name=name,
                department=department,
                position=position,
                phone=record.get("phone") or record.get("手机号") or "",
                email=record.get("email") or record.get("邮箱") or "",
                status=record.get("status") or record.get("状态") or "在职",
            )
            employee_service.create_employee(db, data)
            success_count += 1
        except Exception as e:
            error_count += 1
            errors.append(f"第{i+2}行: {str(e)}")

    return success_count, error_count, errors


def generate_template() -> str:
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["employee_id", "name", "department", "position", "phone", "email", "status"])
    writer.writerow(["EMP001", "张三", "技术部", "工程师", "13800138001", "zhangsan@example.com", "在职"])
    return output.getvalue()
```

- [ ] **Step 2: 创建 app/routers/import_api.py**

```python
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from fastapi.responses import PlainTextResponse
from sqlalchemy.orm import Session
from app.database import get_db
from app.services import import_service

router = APIRouter(prefix="/api/import", tags=["数据导入"])


@router.post("/employees")
async def import_employees(file: UploadFile = File(...), db: Session = Depends(get_db)):
    if not file.filename or not (file.filename.endswith(".csv") or file.filename.endswith(".xlsx")):
        raise HTTPException(status_code=400, detail="仅支持 CSV 或 Excel 文件")
    content = await file.read()
    records = import_service.parse_csv(content)
    if not records:
        raise HTTPException(status_code=400, detail="文件为空或格式不正确")
    success_count, error_count, errors = import_service.import_employees(db, records)
    return {
        "success_count": success_count,
        "error_count": error_count,
        "total": len(records),
        "errors": errors,
    }


@router.get("/template", response_class=PlainTextResponse)
def download_template():
    return PlainTextResponse(
        content=import_service.generate_template(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=employee_import_template.csv"},
    )
```

- [ ] **Step 3: 修改 app/main.py 注册路由**

```python
from app.routers import employees, budgets, whitelist, import_api

app.include_router(employees.router)
app.include_router(import_api.router)
# budgets / whitelist 路由在后续任务中注册
```

- [ ] **Step 4: 创建 tests/test_import.py**

```python
import io
import csv


def test_import_employees_csv(client):
    # 生成 CSV 内容
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["employee_id", "name", "department", "position"])
    writer.writerow(["IMP001", "导入测试1", "技术部", "工程师"])
    writer.writerow(["IMP002", "导入测试2", "市场部", "经理"])
    csv_content = output.getvalue()

    response = client.post(
        "/api/import/employees",
        files={"file": ("test.csv", csv_content, "text/csv")},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success_count"] == 2
    assert data["error_count"] == 0


def test_import_employees_missing_required(client):
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["employee_id", "name"])
    writer.writerow(["", ""])  # 缺少必填字段
    csv_content = output.getvalue()

    response = client.post(
        "/api/import/employees",
        files={"file": ("test.csv", csv_content, "text/csv")},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["error_count"] == 1


def test_download_template(client):
    response = client.get("/api/import/template")
    assert response.status_code == 200
    assert "employee_id" in response.text
    assert "text/csv" in response.headers["content-type"]
```

- [ ] **Step 5: 运行测试**

```bash
cd /workspace/manyu_test
pytest tests/test_import.py -v
# 期望: 所有测试 PASS
```

---

## Task 6: 后端成本预算 API (manyu_test)

**Files:**
- Create: `app/services/budget_service.py`
- Create: `app/routers/budgets.py`
- Modify: `app/main.py` (注册路由)
- Create: `tests/test_budgets.py`

**Interfaces:**
- Consumes: `Budget`, `BudgetCreate`, `BudgetUpdate`, `BudgetResponse`, `BudgetListResponse`, `employee_service`
- Produces: `GET/POST/PUT/DELETE /api/budgets` 端点

- [ ] **Step 1: 创建 app/services/budget_service.py**

```python
from typing import Optional, Tuple, List
from sqlalchemy.orm import Session
from app.models.budget import Budget
from app.schemas.budget import BudgetCreate, BudgetUpdate


def list_budgets(
    db: Session,
    page: int = 1,
    page_size: int = 20,
    employee_id: Optional[str] = None,
    budget_year: Optional[int] = None,
) -> Tuple[List[Budget], int]:
    query = db.query(Budget)
    if employee_id:
        query = query.filter(Budget.employee_id == employee_id)
    if budget_year:
        query = query.filter(Budget.budget_year == budget_year)
    total = query.count()
    budgets = query.order_by(Budget.id.desc()).offset((page - 1) * page_size).limit(page_size).all()
    return budgets, total


def get_budget(db: Session, budget_id: int) -> Optional[Budget]:
    return db.query(Budget).filter(Budget.id == budget_id).first()


def create_budget(db: Session, data: BudgetCreate) -> Budget:
    budget = Budget(**data.model_dump())
    db.add(budget)
    db.commit()
    db.refresh(budget)
    return budget


def update_budget(db: Session, budget_id: int, data: BudgetUpdate) -> Optional[Budget]:
    budget = get_budget(db, budget_id)
    if not budget:
        return None
    update_data = data.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(budget, key, value)
    db.commit()
    db.refresh(budget)
    return budget


def delete_budget(db: Session, budget_id: int) -> bool:
    budget = get_budget(db, budget_id)
    if not budget:
        return False
    db.delete(budget)
    db.commit()
    return True
```

- [ ] **Step 2: 创建 app/routers/budgets.py**

```python
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.budget import (
    BudgetCreate, BudgetUpdate, BudgetResponse, BudgetListResponse,
)
from app.services import budget_service, employee_service

router = APIRouter(prefix="/api/budgets", tags=["成本预算"])


@router.get("", response_model=BudgetListResponse)
def list_budgets(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    employee_id: Optional[str] = Query(None),
    budget_year: Optional[int] = Query(None),
    db: Session = Depends(get_db),
):
    budgets, total = budget_service.list_budgets(db, page, page_size, employee_id, budget_year)
    return BudgetListResponse(
        total=total,
        items=[BudgetResponse.model_validate(b) for b in budgets],
        page=page,
        page_size=page_size,
    )


@router.get("/{budget_id}", response_model=BudgetResponse)
def get_budget(budget_id: int, db: Session = Depends(get_db)):
    budget = budget_service.get_budget(db, budget_id)
    if not budget:
        raise HTTPException(status_code=404, detail="预算记录不存在")
    return BudgetResponse.model_validate(budget)


@router.post("", response_model=BudgetResponse, status_code=201)
def create_budget(data: BudgetCreate, db: Session = Depends(get_db)):
    employee = employee_service.get_employee_by_emp_no(db, data.employee_id)
    if not employee:
        raise HTTPException(status_code=404, detail="关联员工不存在")
    budget = budget_service.create_budget(db, data)
    return BudgetResponse.model_validate(budget)


@router.put("/{budget_id}", response_model=BudgetResponse)
def update_budget(budget_id: int, data: BudgetUpdate, db: Session = Depends(get_db)):
    budget = budget_service.update_budget(db, budget_id, data)
    if not budget:
        raise HTTPException(status_code=404, detail="预算记录不存在")
    return BudgetResponse.model_validate(budget)


@router.delete("/{budget_id}", status_code=204)
def delete_budget(budget_id: int, db: Session = Depends(get_db)):
    success = budget_service.delete_budget(db, budget_id)
    if not success:
        raise HTTPException(status_code=404, detail="预算记录不存在")
    return None
```

- [ ] **Step 3: 修改 app/main.py 注册路由**

```python
from app.routers import employees, budgets, whitelist, import_api

app.include_router(employees.router)
app.include_router(import_api.router)
app.include_router(budgets.router)
# whitelist 路由在后续任务中注册
```

- [ ] **Step 4: 创建 tests/test_budgets.py**

```python
def test_create_budget(client):
    # 先创建员工
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    response = client.post("/api/budgets", json={
        "employee_id": "EMP001",
        "budget_year": 2026,
        "budget_amount": 100000.00,
        "description": "年度预算",
    })
    assert response.status_code == 201
    data = response.json()
    assert data["employee_id"] == "EMP001"
    assert data["budget_amount"] == 100000.00


def test_create_budget_employee_not_found(client):
    response = client.post("/api/budgets", json={
        "employee_id": "NOT_EXIST",
        "budget_year": 2026,
        "budget_amount": 50000.00,
    })
    assert response.status_code == 404


def test_list_budgets(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    client.post("/api/budgets", json={"employee_id": "EMP001", "budget_year": 2026, "budget_amount": 100000.00})
    client.post("/api/budgets", json={"employee_id": "EMP001", "budget_year": 2026, "budget_amount": 200000.00})
    response = client.get("/api/budgets?page=1&page_size=10")
    assert response.status_code == 200
    assert response.json()["total"] == 2


def test_update_budget(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    resp = client.post("/api/budgets", json={"employee_id": "EMP001", "budget_year": 2026, "budget_amount": 100000.00})
    budget_id = resp.json()["id"]
    response = client.put(f"/api/budgets/{budget_id}", json={"budget_amount": 150000.00})
    assert response.status_code == 200
    assert response.json()["budget_amount"] == 150000.00


def test_delete_budget(client):
    client.post("/api/employees", json={"employee_id": "EMP001", "name": "张三"})
    resp = client.post("/api/budgets", json={"employee_id": "EMP001", "budget_year": 2026, "budget_amount": 100000.00})
    budget_id = resp.json()["id"]
    response = client.delete(f"/api/budgets/{budget_id}")
    assert response.status_code == 204
```

- [ ] **Step 5: 运行测试**

```bash
cd /workspace/manyu_test
pytest tests/test_budgets.py -v
# 期望: 所有测试 PASS
```

---

## Task 7: 后端白名单 API (manyu_test)

**Files:**
- Create: `app/services/whitelist_service.py`
- Create: `app/routers/whitelist.py`
- Modify: `app/main.py` (注册路由)
- Create: `tests/test_whitelist.py`

**Interfaces:**
- Consumes: `Whitelist`, `WhitelistCreate`, `WhitelistResponse`, `WhitelistListResponse`
- Produces: `GET/POST/DELETE /api/whitelist` 端点

- [ ] **Step 1: 创建 app/services/whitelist_service.py**

```python
from typing import Optional, List
from sqlalchemy.orm import Session
from app.models.whitelist import Whitelist
from app.schemas.whitelist import WhitelistCreate


def list_whitelist(db: Session) -> List[Whitelist]:
    return db.query(Whitelist).order_by(Whitelist.id.desc()).all()


def get_whitelist(db: Session, whitelist_id: int) -> Optional[Whitelist]:
    return db.query(Whitelist).filter(Whitelist.id == whitelist_id).first()


def create_whitelist(db: Session, data: WhitelistCreate) -> Whitelist:
    entry = Whitelist(**data.model_dump())
    db.add(entry)
    db.commit()
    db.refresh(entry)
    return entry


def delete_whitelist(db: Session, whitelist_id: int) -> bool:
    entry = get_whitelist(db, whitelist_id)
    if not entry:
        return False
    db.delete(entry)
    db.commit()
    return True


def check_whitelist(db: Session, employee_id: str, department: str, whitelist_type: str) -> bool:
    """检查员工是否在白名单中"""
    entry = db.query(Whitelist).filter(
        Whitelist.enabled == True,
        Whitelist.whitelist_type.in_([whitelist_type, "all"]),
    ).filter(
        (Whitelist.employee_id == employee_id) | (Whitelist.department == department)
    ).first()
    return entry is not None
```

- [ ] **Step 2: 创建 app/routers/whitelist.py**

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.whitelist import (
    WhitelistCreate, WhitelistResponse, WhitelistListResponse,
)
from app.services import whitelist_service

router = APIRouter(prefix="/api/whitelist", tags=["白名单管理"])


@router.get("", response_model=WhitelistListResponse)
def list_whitelist(db: Session = Depends(get_db)):
    entries = whitelist_service.list_whitelist(db)
    return WhitelistListResponse(
        total=len(entries),
        items=[WhitelistResponse.model_validate(e) for e in entries],
    )


@router.post("", response_model=WhitelistResponse, status_code=201)
def create_whitelist(data: WhitelistCreate, db: Session = Depends(get_db)):
    entry = whitelist_service.create_whitelist(db, data)
    return WhitelistResponse.model_validate(entry)


@router.delete("/{whitelist_id}", status_code=204)
def delete_whitelist(whitelist_id: int, db: Session = Depends(get_db)):
    success = whitelist_service.delete_whitelist(db, whitelist_id)
    if not success:
        raise HTTPException(status_code=404, detail="白名单条目不存在")
    return None
```

- [ ] **Step 3: 修改 app/main.py 注册路由**

```python
from app.routers import employees, budgets, whitelist, import_api

app.include_router(employees.router)
app.include_router(import_api.router)
app.include_router(budgets.router)
app.include_router(whitelist.router)
```

- [ ] **Step 4: 创建 tests/test_whitelist.py**

```python
def test_create_whitelist(client):
    response = client.post("/api/whitelist", json={
        "employee_id": "EMP001",
        "department": "技术部",
        "whitelist_type": "import",
        "enabled": True,
    })
    assert response.status_code == 201
    data = response.json()
    assert data["employee_id"] == "EMP001"
    assert data["whitelist_type"] == "import"


def test_list_whitelist(client):
    client.post("/api/whitelist", json={"employee_id": "EMP001", "whitelist_type": "import"})
    client.post("/api/whitelist", json={"employee_id": "EMP002", "whitelist_type": "budget"})
    response = client.get("/api/whitelist")
    assert response.status_code == 200
    assert response.json()["total"] == 2


def test_delete_whitelist(client):
    resp = client.post("/api/whitelist", json={"employee_id": "EMP001", "whitelist_type": "import"})
    wl_id = resp.json()["id"]
    response = client.delete(f"/api/whitelist/{wl_id}")
    assert response.status_code == 204
    response = client.get("/api/whitelist")
    assert response.json()["total"] == 0
```

- [ ] **Step 5: 运行测试**

```bash
cd /workspace/manyu_test
pytest tests/test_whitelist.py -v
# 期望: 所有测试 PASS
```

---

## Task 8: 前端项目脚手架搭建 (manyu_test1)

**Files:**
- Create: `package.json`
- Create: `vite.config.js`
- Create: `index.html`
- Create: `src/main.js`
- Create: `src/App.vue`
- Create: `src/router/index.js`
- Create: `src/api/request.js`
- Create: `src/styles/global.css`

**Interfaces:**
- Consumes: 后端 API 地址 `http://localhost:8000`
- Produces: 可运行的 Vue 3 前端项目

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "employee-dashboard-frontend",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "dependencies": {
    "vue": "^3.3.8",
    "vue-router": "^4.2.5",
    "element-plus": "^2.4.3",
    "axios": "^1.6.2",
    "xlsx": "^0.18.5",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.5.0",
    "vite": "^5.0.4",
    "vitest": "^1.0.4",
    "@vue/test-utils": "^2.4.1",
    "jsdom": "^23.0.1"
  }
}
```

- [ ] **Step 2: 创建 vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 3: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>人员看板系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 4: 创建 src/main.js**

```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)
app.use(ElementPlus, { locale: undefined })
app.use(router)
app.mount('#app')
```

- [ ] **Step 5: 创建 src/styles/global.css**

```css
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}
.page-container {
  padding: 20px;
}
```

- [ ] **Step 6: 创建 src/api/request.js**

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const msg = error.response?.data?.detail || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 7: 创建 src/router/index.js**

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/employees',
  },
  {
    path: '/employees',
    name: 'EmployeeList',
    component: () => import('../views/EmployeeList.vue'),
    meta: { title: '员工管理' },
  },
  {
    path: '/import',
    name: 'EmployeeImport',
    component: () => import('../views/EmployeeImport.vue'),
    meta: { title: '数据导入' },
  },
  {
    path: '/budgets',
    name: 'BudgetList',
    component: () => import('../views/BudgetList.vue'),
    meta: { title: '成本预算' },
  },
  {
    path: '/whitelist',
    name: 'WhitelistManager',
    component: () => import('../views/WhitelistManager.vue'),
    meta: { title: '白名单管理' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
```

- [ ] **Step 8: 创建 src/App.vue**

```vue
<template>
  <div id="app-layout">
    <el-container>
      <el-aside width="220px">
        <div class="logo">人员看板</div>
        <el-menu
          :default-active="route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/employees">
            <el-icon><User /></el-icon>
            <span>员工管理</span>
          </el-menu-item>
          <el-menu-item index="/import">
            <el-icon><Upload /></el-icon>
            <span>数据导入</span>
          </el-menu-item>
          <el-menu-item index="/budgets">
            <el-icon><Coin /></el-icon>
            <span>成本预算</span>
          </el-menu-item>
          <el-menu-item index="/whitelist">
            <el-icon><Lock /></el-icon>
            <span>白名单管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header height="50px">
          <span class="header-title">{{ route.meta.title }}</span>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { User, Upload, Coin, Lock } from '@element-plus/icons-vue'
const route = useRoute()
</script>

<style scoped>
#app-layout {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
}
.logo {
  height: 50px;
  line-height: 50px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #263445;
}
.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.el-main {
  background-color: #f5f7fa;
}
</style>
```

- [ ] **Step 9: 安装依赖验证**

```bash
cd /workspace/manyu_test1
npm install
npm run build
# 期望: 构建成功，无错误
```

---

## Task 9: 前端员工管理页面 (manyu_test1)

**Files:**
- Create: `src/views/EmployeeList.vue`
- Create: `src/components/EmployeeForm.vue`
- Create: `src/api/employees.js`

**Interfaces:**
- Consumes: `GET/POST/PUT/DELETE /api/employees`
- Produces: 员工列表页（搜索、分页、新增、编辑、删除）

- [ ] **Step 1: 创建 src/api/employees.js**

```javascript
import request from './request'

export function getEmployees(params) {
  return request.get('/employees', { params })
}

export function getEmployee(id) {
  return request.get(`/employees/${id}`)
}

export function createEmployee(data) {
  return request.post('/employees', data)
}

export function updateEmployee(id, data) {
  return request.put(`/employees/${id}`, data)
}

export function deleteEmployee(id) {
  return request.delete(`/employees/${id}`)
}
```

- [ ] **Step 2: 创建 src/components/EmployeeForm.vue**

```vue
<template>
  <el-dialog
    :title="isEdit ? '编辑员工' : '新增员工'"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工号" prop="employee_id">
            <el-input v-model="formData.employee_id" :disabled="isEdit" placeholder="请输入工号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="formData.name" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="部门" prop="department">
            <el-input v-model="formData.department" placeholder="请输入部门" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职位" prop="position">
            <el-input v-model="formData.position" placeholder="请输入职位" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="入职日期" prop="hire_date">
            <el-date-picker v-model="formData.hire_date" type="date" placeholder="选择日期" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" style="width: 100%">
              <el-option label="在职" value="在职" />
              <el-option label="离职" value="离职" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createEmployee, updateEmployee } from '../api/employees'

const props = defineProps({
  modelValue: Boolean,
  employee: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => { emit('update:modelValue', v) })

const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  employee_id: '',
  name: '',
  department: '',
  position: '',
  phone: '',
  email: '',
  hire_date: null,
  status: '在职',
})

const rules = {
  employee_id: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

watch(() => props.employee, (emp) => {
  if (emp) {
    isEdit.value = true
    Object.assign(formData, emp)
  } else {
    isEdit.value = false
    Object.assign(formData, { employee_id: '', name: '', department: '', position: '', phone: '', email: '', hire_date: null, status: '在职' })
  }
}, { immediate: true })

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateEmployee(props.employee.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createEmployee(formData)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>
```

- [ ] **Step 3: 创建 src/views/EmployeeList.vue**

```vue
<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="showAddDialog">新增员工</el-button>
      <div class="search-bar">
        <el-input v-model="search" placeholder="搜索工号/姓名/部门" clearable style="width: 260px" @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-button @click="handleSearch" type="primary">搜索</el-button>
      </div>
    </div>

    <el-table :data="employees" border stripe v-loading="loading">
      <el-table-column prop="employee_id" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="department" label="部门" width="150" />
      <el-table-column prop="position" label="职位" width="150" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column prop="hire_date" label="入职日期" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchEmployees"
      />
    </div>

    <EmployeeForm v-model="showForm" :employee="currentEmployee" @saved="fetchEmployees" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEmployees, deleteEmployee } from '../api/employees'
import EmployeeForm from '../components/EmployeeForm.vue'

const employees = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const search = ref('')
const loading = ref(false)
const showForm = ref(false)
const currentEmployee = ref(null)

onMounted(() => fetchEmployees())

async function fetchEmployees() {
  loading.value = true
  try {
    const res = await getEmployees({ page: page.value, page_size: pageSize.value, search: search.value || undefined })
    employees.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchEmployees()
}

function showAddDialog() {
  currentEmployee.value = null
  showForm.value = true
}

function showEditDialog(row) {
  currentEmployee.value = { ...row }
  showForm.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除员工 "${row.name}" 吗？`, '确认删除', { type: 'warning' })
    await deleteEmployee(row.id)
    ElMessage.success('删除成功')
    fetchEmployees()
  } catch {
    // 取消删除
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.search-bar {
  display: flex;
  gap: 8px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

---

## Task 10: 前端导入页面 (manyu_test1)

**Files:**
- Create: `src/views/EmployeeImport.vue`
- Create: `src/components/ImportDialog.vue`

**Interfaces:**
- Consumes: `POST /api/import/employees`, `GET /api/import/template`
- Produces: 导入页面（单条录入 + 批量导入 + 模板下载）

- [ ] **Step 1: 创建 src/views/EmployeeImport.vue**

```vue
<template>
  <div class="page-container">
    <el-card class="import-card">
      <template #header>
        <span>单条录入</span>
      </template>
      <el-form :model="singleForm" label-width="100px" :rules="singleRules" ref="singleFormRef" size="small">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="工号" prop="employee_id">
              <el-input v-model="singleForm.employee_id" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="singleForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="部门">
              <el-input v-model="singleForm.department" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="职位">
              <el-input v-model="singleForm.position" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="手机号">
              <el-input v-model="singleForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮箱">
              <el-input v-model="singleForm.email" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="handleSingleSubmit" :loading="singleLoading">录入</el-button>
          <el-button @click="resetSingleForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="import-card" style="margin-top: 16px">
      <template #header>
        <span>批量导入</span>
      </template>
      <div class="batch-import">
        <p>支持 CSV 格式文件导入员工数据。可先下载模板参考格式。</p>
        <div class="batch-actions">
          <el-button @click="downloadTemplate">下载导入模板</el-button>
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            accept=".csv,.xlsx"
            :on-change="handleFileChange"
            :limit="1"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
          <el-button type="success" :disabled="!selectedFile" @click="handleBatchImport" :loading="batchLoading">
            开始导入
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="importResult" class="import-card" style="margin-top: 16px">
      <template #header>
        <span>导入结果</span>
      </template>
      <el-alert
        :title="`导入完成：成功 ${importResult.success_count} 条，失败 ${importResult.error_count} 条`"
        :type="importResult.error_count === 0 ? 'success' : 'warning'"
        show-icon
      />
      <div v-if="importResult.errors.length" style="margin-top: 12px">
        <p style="font-weight: 600">错误详情：</p>
        <ul>
          <li v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const singleForm = reactive({
  employee_id: '', name: '', department: '', position: '', phone: '', email: '',
})
const singleRules = {
  employee_id: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}
const singleFormRef = ref(null)
const singleLoading = ref(false)
const selectedFile = ref(null)
const batchLoading = ref(false)
const importResult = ref(null)

async function handleSingleSubmit() {
  const valid = await singleFormRef.value.validate().catch(() => false)
  if (!valid) return
  singleLoading.value = true
  try {
    await request.post('/employees', singleForm)
    ElMessage.success('录入成功')
    resetSingleForm()
  } finally {
    singleLoading.value = false
  }
}

function resetSingleForm() {
  Object.assign(singleForm, { employee_id: '', name: '', department: '', position: '', phone: '', email: '' })
  importResult.value = null
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function downloadTemplate() {
  try {
    const res = await request.get('/import/template', { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = 'employee_import_template.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('模板下载失败')
  }
}

async function handleBatchImport() {
  if (!selectedFile.value) return
  batchLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await request.post('/import/employees', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    importResult.value = res.data
    selectedFile.value = null
  } finally {
    batchLoading.value = false
  }
}
</script>

<style scoped>
.batch-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
}
</style>
```

---

## Task 11: 前端成本预算页面 (manyu_test1)

**Files:**
- Create: `src/views/BudgetList.vue`
- Create: `src/components/BudgetForm.vue`
- Create: `src/api/budgets.js`

**Interfaces:**
- Consumes: `GET/POST/PUT/DELETE /api/budgets`
- Produces: 成本预算列表页

- [ ] **Step 1: 创建 src/api/budgets.js**

```javascript
import request from './request'

export function getBudgets(params) {
  return request.get('/budgets', { params })
}

export function createBudget(data) {
  return request.post('/budgets', data)
}

export function updateBudget(id, data) {
  return request.put(`/budgets/${id}`, data)
}

export function deleteBudget(id) {
  return request.delete(`/budgets/${id}`)
}
```

- [ ] **Step 2: 创建 src/components/BudgetForm.vue**

```vue
<template>
  <el-dialog
    :title="isEdit ? '编辑预算' : '新增预算'"
    v-model="visible"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="员工工号" prop="employee_id">
        <el-input v-model="formData.employee_id" :disabled="isEdit" placeholder="请输入员工工号" />
      </el-form-item>
      <el-form-item label="预算年份" prop="budget_year">
        <el-input-number v-model="formData.budget_year" :min="2020" :max="2099" style="width: 100%" />
      </el-form-item>
      <el-form-item label="预算金额" prop="budget_amount">
        <el-input-number v-model="formData.budget_amount" :min="0" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="实际支出" prop="actual_amount">
        <el-input-number v-model="formData.actual_amount" :min="0" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model="formData.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createBudget, updateBudget } from '../api/budgets'

const props = defineProps({
  modelValue: Boolean,
  budget: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => { emit('update:modelValue', v) })

const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  employee_id: '',
  budget_year: new Date().getFullYear(),
  budget_amount: 0,
  actual_amount: 0,
  description: '',
})

const rules = {
  employee_id: [{ required: true, message: '请输入员工工号', trigger: 'blur' }],
  budget_year: [{ required: true, message: '请选择预算年份', trigger: 'blur' }],
  budget_amount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
}

watch(() => props.budget, (budget) => {
  if (budget) {
    isEdit.value = true
    Object.assign(formData, budget)
  } else {
    isEdit.value = false
    Object.assign(formData, { employee_id: '', budget_year: new Date().getFullYear(), budget_amount: 0, actual_amount: 0, description: '' })
  }
}, { immediate: true })

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBudget(props.budget.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createBudget(formData)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>
```

- [ ] **Step 3: 创建 src/views/BudgetList.vue**

```vue
<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="showAddDialog">新增预算</el-button>
      <div class="search-bar">
        <el-input v-model="filterEmployeeId" placeholder="员工工号" clearable style="width: 160px" />
        <el-input-number v-model="filterYear" :min="2020" :max="2099" placeholder="年份" controls-position="right" style="width: 140px" />
        <el-button @click="fetchBudgets" type="primary">查询</el-button>
      </div>
    </div>

    <el-table :data="budgets" border stripe v-loading="loading">
      <el-table-column prop="employee_id" label="员工工号" width="120" />
      <el-table-column prop="budget_year" label="年份" width="80" />
      <el-table-column prop="budget_amount" label="预算金额" width="150">
        <template #default="{ row }">¥{{ row.budget_amount?.toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="actual_amount" label="实际支出" width="150">
        <template #default="{ row }">¥{{ row.actual_amount?.toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="200" />
      <el-table-column prop="created_at" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchBudgets"
      />
    </div>

    <BudgetForm v-model="showForm" :budget="currentBudget" @saved="fetchBudgets" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgets, deleteBudget } from '../api/budgets'
import BudgetForm from '../components/BudgetForm.vue'

const budgets = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const showForm = ref(false)
const currentBudget = ref(null)
const filterEmployeeId = ref('')
const filterYear = ref(null)

onMounted(() => fetchBudgets())

async function fetchBudgets() {
  loading.value = true
  try {
    const params = { page: page.value, page_size: pageSize.value }
    if (filterEmployeeId.value) params.employee_id = filterEmployeeId.value
    if (filterYear.value) params.budget_year = filterYear.value
    const res = await getBudgets(params)
    budgets.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function showAddDialog() {
  currentBudget.value = null
  showForm.value = true
}

function showEditDialog(row) {
  currentBudget.value = { ...row }
  showForm.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该预算记录吗？', '确认删除', { type: 'warning' })
    await deleteBudget(row.id)
    ElMessage.success('删除成功')
    fetchBudgets()
  } catch { /* 取消 */ }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

---

## Task 12: 前端白名单管理页面 (manyu_test1)

**Files:**
- Create: `src/views/WhitelistManager.vue`
- Create: `src/api/whitelist.js`

**Interfaces:**
- Consumes: `GET/POST/DELETE /api/whitelist`
- Produces: 白名单管理页

- [ ] **Step 1: 创建 src/api/whitelist.js**

```javascript
import request from './request'

export function getWhitelist() {
  return request.get('/whitelist')
}

export function createWhitelist(data) {
  return request.post('/whitelist', data)
}

export function deleteWhitelist(id) {
  return request.delete(`/whitelist/${id}`)
}
```

- [ ] **Step 2: 创建 src/views/WhitelistManager.vue**

```vue
<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="showAddDialog">新增白名单</el-button>
    </div>

    <el-table :data="whitelist" border stripe v-loading="loading">
      <el-table-column prop="employee_id" label="员工工号" width="150" />
      <el-table-column prop="department" label="部门" width="150" />
      <el-table-column prop="whitelist_type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.whitelist_type === 'all' ? 'success' : row.whitelist_type === 'import' ? 'warning' : 'primary'">
            {{ row.whitelist_type === 'all' ? '全部' : row.whitelist_type === 'import' ? '导入' : '预算' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增白名单对话框 -->
    <el-dialog title="新增白名单" v-model="showForm" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="员工工号" prop="employee_id">
          <el-input v-model="formData.employee_id" placeholder="为空则表示部门级白名单" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="formData.department" placeholder="部门名" />
        </el-form-item>
        <el-form-item label="白名单类型" prop="whitelist_type">
          <el-select v-model="formData.whitelist_type" style="width: 100%">
            <el-option label="全部" value="all" />
            <el-option label="导入" value="import" />
            <el-option label="预算" value="budget" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWhitelist, createWhitelist, deleteWhitelist } from '../api/whitelist'

const whitelist = ref([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const formData = reactive({
  employee_id: '',
  department: '',
  whitelist_type: 'all',
  enabled: true,
})
const rules = {}

onMounted(() => fetchWhitelist())

async function fetchWhitelist() {
  loading.value = true
  try {
    const res = await getWhitelist()
    whitelist.value = res.data.items
  } finally {
    loading.value = false
  }
}

function showAddDialog() {
  Object.assign(formData, { employee_id: '', department: '', whitelist_type: 'all', enabled: true })
  showForm.value = true
}

async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createWhitelist(formData)
    ElMessage.success('新增成功')
    showForm.value = false
    fetchWhitelist()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该白名单条目吗？', '确认删除', { type: 'warning' })
    await deleteWhitelist(row.id)
    ElMessage.success('删除成功')
    fetchWhitelist()
  } catch { /* 取消 */ }
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
```

---

## Self-Review Checklist

1. **Spec 覆盖检查**：
   - ✅ 员工基本信息管理（CRUD）→ Task 4（后端）+ Task 9（前端）
   - ✅ 数据导入（单条+批量）→ Task 5（后端）+ Task 10（前端）
   - ✅ 成本预算管理 → Task 6（后端）+ Task 11（前端）
   - ✅ 白名单管理 → Task 7（后端）+ Task 12（前端）
   - ✅ 跨仓架构对齐 → 所有 Task 均明确区分 manyu_test（后端）和 manyu_test1（前端）

2. **占位符检查**：无 TBD/TODO/implement later 等占位符，所有代码块均为完整实现。

3. **类型一致性检查**：
   - API 路径与 dima.md 设计一致（`/api/employees`, `/api/budgets`, `/api/whitelist`, `/api/import/employees`）
   - 前后端接口字段名匹配（`employee_id`, `budget_year`, `budget_amount` 等）
   - 响应格式一致（`total`, `items`, `page`, `page_size`）

4. **跨仓对齐点**：
   - 后端 `GET /api/employees` → 前端 `getEmployees()` 调用
   - 后端 `POST /api/import/employees` → 前端 `ImportDialog.vue` 上传
   - 后端 `GET/POST/PUT/DELETE /api/budgets` → 前端 `BudgetList.vue` 操作
   - 后端 `GET/POST/DELETE /api/whitelist` → 前端 `WhitelistManager.vue` 操作