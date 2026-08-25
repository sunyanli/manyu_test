"""Employee ORM 模型 — 员工表，含唯一索引与逻辑删除。"""

from sqlalchemy import (
    BigInteger,
    Column,
    Date,
    DateTime,
    ForeignKey,
    Index,
    SmallInteger,
    String,
    func,
)
from app.models.base import Base


class Employee(Base):
    __tablename__ = "employees"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(50), nullable=False, comment="姓名")
    employee_no = Column(String(30), nullable=False, comment="工号")
    phone = Column(String(20), nullable=False, comment="手机号")
    dept_id = Column(
        BigInteger,
        ForeignKey("departments.id", ondelete="RESTRICT", onupdate="CASCADE"),
        nullable=False,
        comment="所属部门ID",
    )
    position = Column(String(100), default="", comment="职位")
    status = Column(SmallInteger, nullable=False, default=1, comment="1=在职 2=离职")
    entry_date = Column(Date, default=None, comment="入职日期")
    resign_date = Column(Date, default=None, comment="离职日期")
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime, nullable=False, server_default=func.now(), onupdate=func.now()
    )

    __table_args__ = (
        Index("uk_employee_no", "employee_no", unique=True),
        Index("uk_phone", "phone", unique=True),
        Index("idx_dept_id", "dept_id"),
        Index("idx_status", "status"),
    )