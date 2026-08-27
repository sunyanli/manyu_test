from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict


class TodoCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=200, description="事项名称")
    description: Optional[str] = Field(None, max_length=2000, description="事项描述")


class TodoRead(TodoCreate):
    id: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
