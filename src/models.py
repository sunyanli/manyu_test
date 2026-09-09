"""SQLAlchemy ORM models for User and TodoItem entities."""

from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime

from src.database import Base


class User(Base):
    """用户实体，存储内部用户基本信息。"""

    __tablename__ = "user"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), nullable=False, unique=True)
    password_hash = Column(String(255), nullable=False)
    gmt_create = Column(DateTime, nullable=False, default=datetime.utcnow)
    gmt_modified = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    todos = relationship("TodoItem", back_populates="user", cascade="all, delete-orphan")


class TodoItem(Base):
    """待办事项实体，记录用户的待办事项信息。"""

    __tablename__ = "todo_item"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(200), nullable=False)
    description = Column(Text, nullable=True)
    user_id = Column(Integer, nullable=False)
    gmt_create = Column(DateTime, nullable=False, default=datetime.utcnow)
    gmt_modified = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    user = relationship("User", back_populates="todos")