"""TransferRecord ORM 模型 — 调动留痕表。"""

from sqlalchemy import (
    BigInteger,
    Column,
    DateTime,
    ForeignKey,
    Index,
    String,
    func,
)
from app.models.base import Base


class TransferRecord(Base):
    __tablename__ = "transfer_records"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    employee_id = Column(
        BigInteger,
        ForeignKey("employees.id", ondelete="RESTRICT", onupdate="CASCADE"),
        nullable=False,
        comment="员工ID",
    )
    from_dept_id = Column(BigInteger, nullable=False, comment="原部门ID")
    to_dept_id = Column(BigInteger, nullable=False, comment="目标部门ID")
    from_position = Column(String(100), default="", comment="原职位")
    to_position = Column(String(100), default="", comment="新职位")
    reason = Column(String(500), default="", comment="调动原因")
    operator_id = Column(BigInteger, default=None, comment="操作人ID")
    created_at = Column(DateTime, nullable=False, server_default=func.now())

    __table_args__ = (
        Index("idx_employee_id", "employee_id"),
        Index("idx_created_at", "created_at"),
    )