"""HTTP 接口：POST /api/auth/login、POST /api/auth/logout。"""

from __future__ import annotations

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Optional
from urllib.parse import urlsplit

from src.auth_service import AUTH_FAILED, SUCCESS, AuthService
from src.config import Config
from src.security import RateLimiter

COOKIE_NAME = "session"

MESSAGES = {
    "SUCCESS_LOGIN": "登录成功",
    "SUCCESS_LOGOUT": "已退出登录",
    "AUTH_FAILED": "账号或密码错误",
    "RATE_LIMITED": "请求过于频繁，请稍后再试",
    "VALIDATION_ERROR": "用户名和密码不能为空",
    "NOT_FOUND": "资源不存在",
    "METHOD_NOT_ALLOWED": "请求方法不允许",
    "INTERNAL_ERROR": "服务器内部错误",
}


def _json_response(
    handler: BaseHTTPRequestHandler,
    status: int,
    payload: dict[str, Any],
    cookies: Optional[list[str]] = None,
) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    for cookie in cookies or []:
        handler.send_header("Set-Cookie", cookie)
    handler.end_headers()
    handler.wfile.write(body)


def _session_cookie(value: str, max_age: int) -> str:
    return (
        f"{COOKIE_NAME}={value}; Path=/; Max-Age={max_age}; HttpOnly; SameSite=Lax"
    )


def _client_ip(handler: BaseHTTPRequestHandler) -> str:
    forwarded = handler.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return handler.client_address[0] if handler.client_address else "unknown"


def _cookie_token(handler: BaseHTTPRequestHandler) -> str:
    raw = handler.headers.get("Cookie") or ""
    for part in raw.split(";"):
        name, _, value = part.strip().partition("=")
        if name == COOKIE_NAME:
            return value
    return ""


class AuthHandler(BaseHTTPRequestHandler):
    server_version = "AuthAPI/1.0"
    config: Config
    limiter: RateLimiter

    def log_message(self, fmt: str, *args: Any) -> None:  # 静默访问日志
        return

    # ------------------------------------------------------------------ routes
    def do_POST(self) -> None:  # noqa: N802 - BaseHTTPRequestHandler 约定
        path = urlsplit(self.path).path.rstrip("/") or "/"
        try:
            if path == "/api/auth/login":
                self._handle_login()
            elif path == "/api/auth/logout":
                self._handle_logout()
            else:
                _json_response(self, 404, {"code": "NOT_FOUND", "message": MESSAGES["NOT_FOUND"]})
        except Exception:  # pragma: no cover - 兜底
            _json_response(
                self, 500, {"code": "INTERNAL_ERROR", "message": MESSAGES["INTERNAL_ERROR"]}
            )

    def do_GET(self) -> None:  # noqa: N802
        path = urlsplit(self.path).path.rstrip("/") or "/"
        if path == "/healthz":
            _json_response(self, 200, {"code": "SUCCESS", "message": "ok"})
        elif path in ("/api/auth/login", "/api/auth/logout"):
            _json_response(
                self,
                405,
                {"code": "METHOD_NOT_ALLOWED", "message": MESSAGES["METHOD_NOT_ALLOWED"]},
            )
        else:
            _json_response(self, 404, {"code": "NOT_FOUND", "message": MESSAGES["NOT_FOUND"]})

    # ---------------------------------------------------------------- handlers
    def _read_json(self) -> Optional[dict[str, Any]]:
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        try:
            data = json.loads(raw or b"{}")
        except (ValueError, UnicodeDecodeError):
            return None
        return data if isinstance(data, dict) else None

    def _handle_login(self) -> None:
        client_ip = _client_ip(self)
        if self.limiter.is_rate_limited(client_ip):
            _json_response(
                self,
                429,
                {"code": "RATE_LIMITED", "message": MESSAGES["RATE_LIMITED"]},
            )
            return

        body = self._read_json()
        username = (body or {}).get("username")
        password = (body or {}).get("password")
        remember_me = bool((body or {}).get("remember_me", False))
        if (
            body is None
            or not isinstance(username, str)
            or not isinstance(password, str)
            or not username.strip()
            or not password
            or len(username) > 128
            or len(password) > 256
        ):
            _json_response(
                self,
                422,
                {"code": "VALIDATION_ERROR", "message": MESSAGES["VALIDATION_ERROR"]},
            )
            return

        service = AuthService(self.config)
        user, error = service.authenticate(username, password, client_ip)
        if error == AUTH_FAILED or user is None:
            self.limiter.record_attempt(client_ip)
            _json_response(
                self, 401, {"code": AUTH_FAILED, "message": MESSAGES["AUTH_FAILED"]}
            )
            return

        token = service.sign_session(user, remember_me)
        ttl = (
            self.config.remember_me_ttl_seconds
            if remember_me
            else self.config.session_ttl_seconds
        )
        _json_response(
            self,
            200,
            {"code": SUCCESS, "message": MESSAGES["SUCCESS_LOGIN"]},
            cookies=[_session_cookie(token, ttl)],
        )

    def _handle_logout(self) -> None:
        token = _cookie_token(self)
        if token:
            AuthService(self.config).logout(token)
        _json_response(
            self,
            200,
            {"code": SUCCESS, "message": MESSAGES["SUCCESS_LOGOUT"]},
            cookies=[_session_cookie("", 0)],
        )


def create_server(config: Config, host: str = "127.0.0.1", port: int = 8000) -> ThreadingHTTPServer:
    handler_class = type(
        "BoundAuthHandler",
        (AuthHandler,),
        {
            "config": config,
            "limiter": RateLimiter(
                max_attempts=config.rate_limit_max_attempts,
                window_seconds=config.rate_limit_window_seconds,
            ),
        },
    )
    return ThreadingHTTPServer((host, port), handler_class)


def run() -> None:  # pragma: no cover - 手工启动入口
    from src import db as database

    config = Config.from_env()
    database.init_db(config.db_path)
    server = create_server(config, host="0.0.0.0", port=8000)
    print(f"listening on http://0.0.0.0:8000 (db={config.db_path})")
    server.serve_forever()

