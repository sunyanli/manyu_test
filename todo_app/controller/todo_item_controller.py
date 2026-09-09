"""Controller 层：TodoItemController

提供 POST /api/todos HTTP 入口，基于 Python 标准库 http.server。
对应系分方案 §4.1 W01 接口。
"""

import json
import logging
from http.server import BaseHTTPRequestHandler, HTTPServer
from typing import Optional

from todo_app.common.errors import (
    TodoBizException,
    TodoErrorCode,
    TodoErrorMessage,
)
from todo_app.service.todo_item_service import TodoItemService

logger = logging.getLogger(__name__)

CONTENT_TYPE_JSON: str = "application/json; charset=utf-8"
DEFAULT_HOST: str = "0.0.0.0"
DEFAULT_PORT: int = 8080


def _build_response(code: str, msg: str, data: Optional[dict]) -> dict:
    """按系分全局约定 {code, msg, data} 构造响应体。"""
    return {"code": code, "msg": msg, "data": data}


def create_handler(service: TodoItemService):
    """工厂函数：生成绑定 Service 的 HTTP Handler 类，便于测试注入。"""

    class TodoItemHandler(BaseHTTPRequestHandler):
        """POST /api/todos 处理器。"""

        def log_message(self, fmt: str, *args) -> None:
            """重定向标准日志到 logging。"""
            logger.info("http %s", fmt % args)

        def _send_json(self, status: int, body: dict) -> None:
            payload: bytes = json.dumps(body, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", CONTENT_TYPE_JSON)
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        def do_POST(self) -> None:  # noqa: N802 - http.server 约定
            if self.path != "/api/todos":
                self._send_json(
                    404,
                    _build_response(
                        TodoErrorCode.SYSTEM_ERROR,
                        "Not Found",
                        None,
                    ),
                )
                return

            try:
                length: int = int(self.headers.get("Content-Length") or 0)
                raw: bytes = self.rfile.read(length) if length > 0 else b"{}"
                payload: dict = json.loads(raw.decode("utf-8"))
            except (ValueError, json.JSONDecodeError):
                logger.warning("invalid json body")
                self._send_json(
                    400,
                    _build_response(
                        TodoErrorCode.NAME_EMPTY,
                        "请求体必须为合法 JSON",
                        None,
                    ),
                )
                return

            try:
                item = service.create_todo(
                    name=payload.get("name"),
                    description=payload.get("description"),
                    tenant_id=payload.get("tenant_id"),
                )
            except TodoBizException as e:
                logger.warning("biz error: code=%s msg=%s", e.code, e.msg)
                self._send_json(400, _build_response(e.code, e.msg, None))
                return
            except Exception:
                logger.exception("unexpected error")
                self._send_json(
                    500,
                    _build_response(
                        TodoErrorCode.SYSTEM_ERROR,
                        TodoErrorMessage.SYSTEM_ERROR,
                        None,
                    ),
                )
                return

            self._send_json(
                200,
                _build_response(
                    TodoErrorCode.SUCCESS,
                    TodoErrorMessage.SUCCESS,
                    item.to_dict(),
                ),
            )

    return TodoItemHandler


def create_server(
    service: TodoItemService,
    host: str = DEFAULT_HOST,
    port: int = DEFAULT_PORT,
) -> HTTPServer:
    """创建 HTTP Server 实例。"""
    handler = create_handler(service)
    return HTTPServer((host, port), handler)
