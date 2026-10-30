"""SQLite 存储层：建表与 users 表的读写。"""

from __future__ import annotations

import os
import sqlite3
import time
from contextlib import closing
from typing import Any, Optional

SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL UNIQUE,
    password_hash   TEXT    NOT NULL,
    is_active       INTEGER NOT NULL DEFAULT 1,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until    REAL,
    created_at      REAL    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
"""


def connect(db_path: str) -> sqlite3.Connection:
    conn = sqlite3.connect(db_path, timeout=10, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_db(db_path: str) -> None:
    directory = os.path.dirname(os.path.abspath(db_path))
    os.makedirs(directory, exist_ok=True)
    with closing(connect(db_path)) as conn, conn:
        conn.executescript(SCHEMA)


def create_user(
    db_path: str,
    username: str,
    password_hash: str,
    is_active: bool = True,
) -> int:
    with closing(connect(db_path)) as conn, conn:
        cursor = conn.execute(
            "INSERT INTO users (username, password_hash, is_active, created_at)"
            " VALUES (?, ?, ?, ?)",
            (username, password_hash, 1 if is_active else 0, time.time()),
        )
        return int(cursor.lastrowid)


def find_by_username(db_path: str, username: str) -> Optional[sqlite3.Row]:
    with closing(connect(db_path)) as conn:
        cursor = conn.execute("SELECT * FROM users WHERE username = ?", (username,))
        return cursor.fetchone()


def find_by_id(db_path: str, user_id: int) -> Optional[sqlite3.Row]:
    with closing(connect(db_path)) as conn:
        cursor = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,))
        return cursor.fetchone()


def update_login_failure(
    db_path: str,
    user_id: int,
    failed_attempts: int,
    locked_until: Optional[float],
) -> None:
    with closing(connect(db_path)) as conn, conn:
        conn.execute(
            "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?",
            (failed_attempts, locked_until, user_id),
        )


def reset_failed_attempts(db_path: str, user_id: int) -> None:
    with closing(connect(db_path)) as conn, conn:
        conn.execute(
            "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ?",
            (user_id,),
        )


def row_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    return {key: row[key] for key in row.keys()}

