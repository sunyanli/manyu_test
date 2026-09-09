"""FastAPI application entry point for the Todo Management System."""

from fastapi import FastAPI
from src.database import init_db
from src.controller.todo_controller import router as todo_router
from src.controller.auth_controller import router as auth_router

app = FastAPI(title="待办事项管理系统", version="1.0.0")

# Initialize database tables on startup
@app.on_event("startup")
def startup_event():
    init_db()

# Register routers
app.include_router(todo_router)
app.include_router(auth_router)


@app.get("/")
def root():
    """Health check endpoint."""
    return {"code": 0, "msg": "success", "data": {"status": "running"}}