"""
埋点模块：SQLite 数据库初始化、中间件、报表查询
"""
import logging
import sqlite3
import os
from datetime import datetime

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

logger = logging.getLogger("tracking")

DB_PATH = os.path.join(os.path.dirname(__file__), "tracking.db")

TRACKED_PATHS = {"/api/helloworld", "/api/hash", "/api/bubble_sort"}

VALID_DIMENSIONS = {"user_type", "user_level", "user_dept", "api_name"}


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_connection()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS tracking_logs (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            api_name   TEXT NOT NULL,
            user_id    TEXT NOT NULL,
            user_type  TEXT DEFAULT 'unknown',
            user_level TEXT DEFAULT 'unknown',
            user_dept  TEXT DEFAULT 'unknown',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_api_name ON tracking_logs(api_name)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_type ON tracking_logs(user_type)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_level ON tracking_logs(user_level)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_dept ON tracking_logs(user_dept)
    """)
    conn.commit()
    conn.close()


class TrackingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)

        path = request.url.path
        if path not in TRACKED_PATHS:
            return response

        api_name = path.split("/")[-1]
        user_id = request.headers.get("X-User-Id", "anonymous")
        user_type = request.headers.get("X-User-Type", "unknown")
        user_level = request.headers.get("X-User-Level", "unknown")
        user_dept = request.headers.get("X-User-Dept", "unknown")

        try:
            conn = get_connection()
            conn.execute(
                "INSERT INTO tracking_logs (api_name, user_id, user_type, user_level, user_dept) "
                "VALUES (?, ?, ?, ?, ?)",
                (api_name, user_id, user_type, user_level, user_dept),
            )
            conn.commit()
            conn.close()
        except sqlite3.Error as e:
            logger.error(f"Failed to write tracking log: {e}")

        return response


def get_report(dimension: str) -> dict:
    """按指定维度聚合调用次数，支持逗号分隔的二维交叉维度。"""
    dims = [d.strip() for d in dimension.split(",")]
    for d in dims:
        if d not in VALID_DIMENSIONS:
            return {"error": f"invalid dimension: {d}"}

    select_cols = ", ".join(dims)
    group_cols = ", ".join(dims)

    try:
        conn = get_connection()
        rows = conn.execute(
            f"SELECT {select_cols}, COUNT(*) as cnt "
            "FROM tracking_logs "
            f"GROUP BY {group_cols} "
            "ORDER BY cnt DESC"
        ).fetchall()
        conn.close()

        labels = []
        values = []
        for row in rows:
            if len(dims) == 1:
                label = str(row[dims[0]] or "unknown")
            else:
                label = " / ".join(str(row[d] or "unknown") for d in dims)
            labels.append(label)
            values.append(row["cnt"])

        return {"labels": labels, "values": values}
    except sqlite3.Error as e:
        return {"error": "tracking service unavailable"}