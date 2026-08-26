"""FastAPI 应用入口 — 挂载路由 + 异常处理器 + 生命周期。"""

from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.database import engine
from app.models.base import Base
from app.routers import departments, employees
from app.utils.exceptions import (
    AppException,
    app_exception_handler,
    validation_exception_handler,
)
from fastapi.exceptions import RequestValidationError


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：开发环境自动建表，关闭时释放连接。"""
    if settings.auto_create_tables:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()


app = FastAPI(
    title="组织架构管理模块",
    description="部门树管理 + 员工生命周期管理 API",
    version="0.1.0",
    lifespan=lifespan,
)

# 注册异常处理器
app.add_exception_handler(AppException, app_exception_handler)
app.add_exception_handler(RequestValidationError, validation_exception_handler)

# 挂载路由
app.include_router(departments.router)
app.include_router(employees.router)


@app.get("/health")
async def health():
    return {"status": "ok"}