"""认证服务：登录校验、失败计数、账号锁定、会话签发与注销。"""

from __future__ import annotations

import threading
import time
from typing import Any, Optional

from src import db
from src.config import Config
from src.security import hash_password, sign_session, verify_password, verify_session

AUTH_FAILED = "AUTH_FAILED"
SUCCESS = "SUCCESS"

# 已注销令牌（进程内黑名单，够用且无需额外存储）
_REVOKED: set[str] = set()
_REVOKED_LOCK = threading.Lock()


class AuthService:
    def __init__(self, config: Config) -> None:
        self._config = config
        # 用户不存在时也做一次哈希校验，避免响应时间泄露账号存在性
        self._dummy_hash = hash_password("dummy-password-for-timing")

    def authenticate(
        self, username: str, password: str, client_ip: str = ""
    ) -> tuple[Optional[dict[str, Any]], str]:
        """返回 (user | None, error_code)，成功时 error_code 为空串。"""
        row = db.find_by_username(self._config.db_path, username)
        if row is None:
            verify_password(password, self._dummy_hash)
            return None, AUTH_FAILED

        user = db.row_to_dict(row)
        if not user["is_active"]:
            verify_password(password, self._dummy_hash)
            return None, AUTH_FAILED

        locked_until = user["locked_until"]
        if locked_until is not None and float(locked_until) > time.time():
            verify_password(password, self._dummy_hash)
            return None, AUTH_FAILED

        if not verify_password(password, user["password_hash"]):
            failures = int(user["failed_attempts"]) + 1
            locked_until = None
            if failures >= self._config.lockout_threshold:
                locked_until = time.time() + self._config.lockout_seconds
            db.update_login_failure(self._config.db_path, user["id"], failures, locked_until)
            return None, AUTH_FAILED

        if int(user["failed_attempts"]) or user["locked_until"] is not None:
            db.reset_failed_attempts(self._config.db_path, user["id"])
        user["failed_attempts"] = 0
        user["locked_until"] = None
        return user, ""

    def sign_session(self, user: dict[str, Any], remember_me: bool = False) -> str:
        ttl = (
            self._config.remember_me_ttl_seconds
            if remember_me
            else self._config.session_ttl_seconds
        )
        payload = {"user_id": user["id"], "username": user["username"]}
        return sign_session(payload, ttl, self._config.secret_key)

    def verify_session(self, token: str) -> Optional[dict[str, Any]]:
        with _REVOKED_LOCK:
            if token in _REVOKED:
                return None
        return verify_session(token, self._config.secret_key)

    def logout(self, token: str) -> None:
        if not token:
            return
        with _REVOKED_LOCK:
            _REVOKED.add(token)

    def reset_revoked(self) -> None:  # pragma: no cover - 测试辅助
        with _REVOKED_LOCK:
            _REVOKED.clear()

