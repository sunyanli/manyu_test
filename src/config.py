"""运行配置：优先读环境变量，缺省值可直接开发运行。"""

from __future__ import annotations

import os
from dataclasses import dataclass


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name)
    if raw is None or raw.strip() == "":
        return default
    return int(raw)


def _env_float(name: str, default: float) -> float:
    raw = os.environ.get(name)
    if raw is None or raw.strip() == "":
        return default
    return float(raw)


@dataclass(frozen=True)
class Config:
    """认证相关配置。"""

    db_path: str = "app.db"
    secret_key: str = "dev-secret-change-in-production"
    session_ttl_seconds: int = 7200  # 2 小时
    remember_me_ttl_seconds: int = 2_592_000  # 30 天
    rate_limit_max_attempts: int = 5
    rate_limit_window_seconds: float = 60.0
    lockout_threshold: int = 10
    lockout_seconds: int = 1800  # 30 分钟

    @classmethod
    def from_env(cls) -> "Config":
        defaults = cls()
        return cls(
            db_path=os.environ.get("DB_PATH", defaults.db_path),
            secret_key=os.environ.get("SECRET_KEY", defaults.secret_key),
            session_ttl_seconds=_env_int("SESSION_TTL_SECONDS", defaults.session_ttl_seconds),
            remember_me_ttl_seconds=_env_int(
                "REMEMBER_ME_TTL_SECONDS", defaults.remember_me_ttl_seconds
            ),
            rate_limit_max_attempts=_env_int(
                "RATE_LIMIT_MAX_ATTEMPTS", defaults.rate_limit_max_attempts
            ),
            rate_limit_window_seconds=_env_float(
                "RATE_LIMIT_WINDOW_SECONDS", defaults.rate_limit_window_seconds
            ),
            lockout_threshold=_env_int("ACCOUNT_LOCKOUT_THRESHOLD", defaults.lockout_threshold),
            lockout_seconds=_env_int("ACCOUNT_LOCKOUT_SECONDS", defaults.lockout_seconds),
        )

