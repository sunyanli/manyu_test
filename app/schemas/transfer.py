"""调动记录请求/响应 Schema。"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class TransferRecordInfo(BaseModel):
    id: int
    employee_id: int
    from_dept_id: int
    to_dept_id: int
    from_position: str
    to_position: str
    reason: str
    operator_id: Optional[int] = None
    created_at: datetime

    model_config = {"from_attributes": True}


class TransferRecordListResult(BaseModel):
    items: list
    total: int
    page: int
    page_size: int