"""User entity — domain model for an internal user."""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class User:
    """用户实体，存储内部用户基本信息。"""

    id: Optional[int] = None
    username: str = ""
    password_hash: str = ""
    gmt_create: datetime = field(default_factory=datetime.utcnow)
    gmt_modified: datetime = field(default_factory=datetime.utcnow)