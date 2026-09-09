"""Controller layer — FastAPI routes for authentication operations."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from src.database import get_db
from src.model.dto.TodoRequest import TodoRequest
from src.model.dto.TodoResponse import TodoResponse
from src.service.auth_service import AuthService
from src.service.todo_service import TodoService
from src.repository.todo_repository import TodoRepository

router = APIRouter(prefix="/api/auth", tags=["auth"])


def get_auth_service(db: Session = Depends(get_db)) -> AuthService:
    """FastAPI dependency that provides an AuthService."""
    return AuthService()


@router.post("/login")
def login(
    username: str,
    password: str,
    service: AuthService = Depends(get_auth_service),
) -> dict:
    """用户登录。"""
    try:
        result = service.login(username=username, password=password)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc))
    return result


@router.get("/me")
def get_current_user(
    service: AuthService = Depends(get_auth_service),
    current_user_id: int = 1,
) -> dict:
    """获取当前用户信息。"""
    for user in service._users.values():
        if user.id == current_user_id:
            return {
                "code": 0,
                "msg": "success",
                "data": {
                    "id": user.id,
                    "username": user.username,
                },
            }
    raise HTTPException(status_code=404, detail="用户不存在")