"""统一响应体 Schema。"""

from typing import Any, Generic, Optional, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = 200
    data: Optional[T] = None
    msg: str = "ok"


class PaginatedData(BaseModel):
    items: list
    total: int
    page: int
    page_size: int