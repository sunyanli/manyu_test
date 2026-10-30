"""口令哈希、会话令牌签名、IP 滑动窗口限流。"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import secrets
import threading
import time
from collections import defaultdict, deque
from typing import Any, Optional

PBKDF2_ITERATIONS = 120_000
_PREFIX = "pbkdf2_sha256"


def hash_password(plain: str) -> str:
    """返回 `pbkdf2_sha256$迭代次数$盐$摘要`。每次调用盐不同。"""
    salt = secrets.token_hex(16)
    digest = hashlib.pbkdf2_hmac(
        "sha256", plain.encode("utf-8"), salt.encode("utf-8"), PBKDF2_ITERATIONS
    ).hex()
    return f"{_PREFIX}${PBKDF2_ITERATIONS}${salt}${digest}"


def verify_password(plain: str, hashed: str) -> bool:
    try:
        prefix, iterations, salt, digest = hashed.split("$")
        if prefix != _PREFIX:
            return False
        candidate = hashlib.pbkdf2_hmac(
            "sha256", plain.encode("utf-8"), salt.encode("utf-8"), int(iterations)
        ).hex()
    except (ValueError, AttributeError):
        return False
    return hmac.compare_digest(candidate, digest)


def _b64encode(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def _b64decode(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def _sign(raw: str, secret: str) -> str:
    return _b64encode(hmac.new(secret.encode("utf-8"), raw.encode("ascii"), hashlib.sha256).digest())


def sign_session(payload: dict[str, Any], ttl_seconds: int, secret: str) -> str:
    """签名会话令牌，payload 中写入 exp（绝对过期时间戳）。"""
    body = dict(payload)
    body["exp"] = int(time.time()) + int(ttl_seconds)
    raw = _b64encode(json.dumps(body, separators=(",", ":"), sort_keys=True).encode("utf-8"))
    return f"{raw}.{_sign(raw, secret)}"


def verify_session(token: str, secret: str) -> Optional[dict[str, Any]]:
    """校验签名与有效期；失败返回 None。"""
    if not isinstance(token, str) or token.count(".") != 1:
        return None
    raw, signature = token.split(".")
    if not hmac.compare_digest(signature, _sign(raw, secret)):
        return None
    try:
        payload = json.loads(_b64decode(raw))
    except (ValueError, TypeError):
        return None
    if not isinstance(payload, dict):
        return None
    exp = payload.get("exp")
    if not isinstance(exp, (int, float)) or exp <= time.time():
        return None
    return payload


class RateLimiter:
    """按 key（客户端 IP）计数的滑动窗口限流器。"""

    def __init__(self, max_attempts: int = 5, window_seconds: float = 60.0) -> None:
        self._max = max_attempts
        self._window = float(window_seconds)
        self._hits: dict[str, deque[float]] = defaultdict(deque)
        self._lock = threading.Lock()

    def _prune(self, key: str, now: float) -> deque[float]:
        hits = self._hits[key]
        cutoff = now - self._window
        while hits and hits[0] <= cutoff:
            hits.popleft()
        return hits

    def is_rate_limited(self, key: str) -> bool:
        with self._lock:
            return len(self._prune(key, time.monotonic())) >= self._max

    def record_attempt(self, key: str) -> None:
        with self._lock:
            now = time.monotonic()
            self._prune(key, now).append(now)

    def reset(self) -> None:
        with self._lock:
            self._hits.clear()

