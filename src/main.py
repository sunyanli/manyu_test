"""FastAPI application entry point."""

from contextlib import asynccontextmanager

from fastapi import FastAPI

from src.database import Base, engine
from src.routes.todo import router as todo_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Create tables on startup."""
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="Todo API", version="0.1.0", lifespan=lifespan)

app.include_router(todo_router)