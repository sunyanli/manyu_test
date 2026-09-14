"""Todo REST routes — create only (minimum closed loop)."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from src.database import get_db
from src.models.todo import TodoItem
from src.schemas.todo import TodoCreate, TodoResponse

router = APIRouter(prefix="/api/todos", tags=["todos"])


@router.post("/", response_model=TodoResponse, status_code=201)
def create_todo(body: TodoCreate, db: Session = Depends(get_db)) -> TodoItem:
    """Create a new todo item. Returns the created item with id and timestamp."""
    item = TodoItem(name=body.name, description=body.description)
    db.add(item)
    db.commit()
    db.refresh(item)
    return item