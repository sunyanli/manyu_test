"""Pydantic schemas for TodoItem."""

from datetime import datetime

from pydantic import BaseModel, Field


class TodoCreate(BaseModel):
    """Request body for creating a todo item."""

    name: str = Field(..., min_length=1, max_length=200, description="事项名称")
    description: str = Field(default="", max_length=5000, description="事项描述")


class TodoResponse(BaseModel):
    """Response body for a created todo item."""

    id: int
    name: str
    description: str
    created_at: datetime

    model_config = {"from_attributes": True}