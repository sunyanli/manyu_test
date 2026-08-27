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
