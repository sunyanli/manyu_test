"""Department ORM 模型 — 邻接表 + 物化路径。"""

from sqlalchemy import (
    BigInteger,
    Column,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    SmallInteger,
    String,
    func,
)
from sqlalchemy.orm import relationship
from app.models.base import Base


class Department(Base):
    __tablename__ = "departments"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False, comment="部门名称")
    parent_id = Column(
        BigInteger, ForeignKey("departments.id", ondelete="RESTRICT", onupdate="CASCADE"),
        default=None, comment="父部门ID，NULL 表示根节点"
    )
    level = Column(SmallInteger, nullable=False, default=1, comment="层级深度，根=1")
    path = Column(String(500), default="", comment="物化路径，如 /1/3/7")
    sort_order = Column(Integer, nullable=False, default=0, comment="同级排序")
    status = Column(SmallInteger, nullable=False, default=1, comment="1=启用 0=禁用")
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime, nullable=False, server_default=func.now(), onupdate=func.now()
    )

    # 自引用关系
    parent = relationship("Department", remote_side=[id], backref="children")

    __table_args__ = (
        Index("idx_parent_id", "parent_id"),
        Index("idx_path", "path"),
    )