"""员工请求/响应 Pydantic Schema。"""

import re
from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, Field, field_validator


# ── 请求体 ──

class EmployeeCheckRequest(BaseModel):
    field: str = Field(..., pattern=r"^(employeeNo|phone)$")
    value: str = Field(..., min_length=1)


class EmployeeCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, description="姓名")
    employee_no: str = Field(
        ..., alias="employeeNo", min_length=1, max_length=30, description="工号"
    )
    dept_id: int = Field(..., alias="deptId", gt=0, description="所属部门ID")
    phone: str = Field(..., min_length=1, max_length=20, description="手机号")
    position: str = Field(default="", max_length=100, description="职位")
    entry_date: Optional[date] = Field(default=None, alias="entryDate", description="入职日期")

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str) -> str:
        if not re.match(r"^1[3-9]\d{9}$", v):
            raise ValueError("手机号格式不正确，须为11位中国大陆手机号")
        return v


class EmployeeUpdate(BaseModel):
    name: Optional[str] = Field(default=None, min_length=1, max_length=50)
    phone: Optional[str] = Field(default=None, min_length=1, max_length=20)
    position: Optional[str] = Field(default=None, max_length=100)

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not re.match(r"^1[3-9]\d{9}$", v):
            raise ValueError("手机号格式不正确，须为11位中国大陆手机号")
        return v


class EmployeeTransfer(BaseModel):
    new_dept_id: int = Field(..., alias="newDeptId", gt=0, description="目标部门ID")
    new_position: str = Field(
        default="", alias="newPosition", max_length=100, description="新职位"
    )
    reason: str = Field(default="", max_length=500, description="调动原因")


class EmployeeResign(BaseModel):
    resign_date: date = Field(..., alias="resignDate", description="离职日期")


# ── 响应体 ──

class EmployeeCheckResult(BaseModel):
    is_exist: bool = False


class EmployeeInfo(BaseModel):
    id: int
    name: str
    employee_no: str
    phone: str
    dept_id: int
    position: str
    status: int
    entry_date: Optional[date] = None
    resign_date: Optional[date] = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class EmployeeListResult(BaseModel):
    items: list
    total: int
    page: int
    page_size: int