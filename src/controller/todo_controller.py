"""Controller layer — FastAPI routes for todo operations."""

from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from src.database import get_db
from src.model.dto.TodoRequest import TodoRequest
from src.model.dto.TodoResponse import TodoResponse
from src.service.todo_service import TodoService

router = APIRouter(prefix="/api/todo", tags=["todo"])


def get_todo_service(db: Session = Depends(get_db)) -> TodoService:
    """FastAPI dependency that provides a TodoService backed by the real repository."""
    from src.repository.todo_repository import TodoRepository

    service = TodoService()
    service.set_repository(TodoRepository(db))
    return service


@router.post("")
def create_todo(
    request: TodoRequest,
    service: TodoService = Depends(get_todo_service),
    current_user_id: int = 1,
) -> TodoResponse:
    """新增待办事项。"""
    try:
        item = service.create_todo(user_id=current_user_id, request=request)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    return TodoResponse.from_entity(item)


@router.get("")
def list_todos(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    service: TodoService = Depends(get_todo_service),
    current_user_id: int = 1,
) -> dict:
    """查看待办事项列表。"""
    items = service.list_todos(user_id=current_user_id, page=page, page_size=page_size)
    return {
        "code": 0,
        "msg": "success",
        "data": {
            "total": len(items),
            "items": [TodoResponse.from_entity(i) for i in items],
        },
    }


@router.get("/{todo_id}")
def get_todo_detail(
    todo_id: int,
    service: TodoService = Depends(get_todo_service),
    current_user_id: int = 1,
) -> TodoResponse:
    """查看待办事项详情。"""
    item = service.get_todo_detail(todo_id=todo_id, user_id=current_user_id)
    if item is None:
        raise HTTPException(status_code=404, detail="待办事项不存在")
    return TodoResponse.from_entity(item)