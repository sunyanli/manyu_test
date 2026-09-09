"""Business service layer for authentication operations."""

import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Dict, Optional

from src.model.entity.User import User


TOKEN_EXPIRY_SECONDS = 86400  # 24 hours


class AuthService:
    """认证服务 — 用户登录与 Token 校验。"""

    def __init__(self):
        """Initialize with an in-memory user store (for testing)."""
        self._users: Dict[str, User] = {}
        self._tokens: Dict[str, Dict] = {}

    def _hash_password(self, password: str) -> str:
        """Hash a password using SHA-256 with a random salt."""
        salt = secrets.token_hex(16)
        hashed = hashlib.sha256((password + salt).encode()).hexdigest()
        return f"{salt}${hashed}"

    def _verify_password(self, password: str, stored_hash: str) -> bool:
        """Verify a password against the stored hash."""
        try:
            salt, hashed = stored_hash.split("$", 1)
            return hashlib.sha256((password + salt).encode()).hexdigest() == hashed
        except (ValueError, AttributeError):
            return False

    def _generate_token(self) -> str:
        """Generate a secure random token."""
        return secrets.token_hex(32)

    def register_user(self, username: str, password: str) -> User:
        """Register a new user with hashed password."""
        if username in self._users:
            raise ValueError("用户名已存在")
        user = User(
            username=username,
            password_hash=self._hash_password(password),
        )
        self._users[username] = user
        return user

    def login(self, username: str, password: str) -> dict:
        """用户登录，验证凭据并返回 Token。

        业务规则：
        - 用户名和密码校验通过后生成 JWT Token，有效期 24 小时
        - 凭据错误时返回 AUTH_001 错误码
        """
        user = self._users.get(username)
        if user is None or not self._verify_password(password, user.password_hash):
            raise ValueError("AUTH_001: 用户名或密码错误")

        token = self._generate_token()
        expires_at = datetime.utcnow() + timedelta(seconds=TOKEN_EXPIRY_SECONDS)
        self._tokens[token] = {"user_id": user.id, "expires_at": expires_at}

        return {
            "code": 0,
            "msg": "success",
            "data": {
                "token": token,
                "username": user.username,
                "expires_in": TOKEN_EXPIRY_SECONDS,
            },
        }

    def verify_token(self, token: str, expired: bool = False) -> Optional[User]:
        """校验 Token 有效性，返回关联的用户信息。

        业务规则：
        - Token 有效且未过期时返回用户
        - Token 无效或过期时抛出 ValueError
        """
        if not token:
            raise ValueError("AUTH_002: Token 无效")

        token_data = self._tokens.get(token)
        if token_data is None:
            raise ValueError("AUTH_002: Token 无效")

        if expired or datetime.utcnow() > token_data["expires_at"]:
            raise ValueError("AUTH_002: Token 已过期")

        # Find user by id
        for user in self._users.values():
            if user.id == token_data["user_id"]:
                return user

        raise ValueError("AUTH_002: Token 无效")