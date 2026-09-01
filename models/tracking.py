import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "tracking.db")


def get_db_path() -> str:
    return DB_PATH


def init_db(db_path: str = DB_PATH) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS api_call_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            api_name TEXT NOT NULL,
            caller_id TEXT,
            caller_name TEXT,
            dept TEXT,
            level TEXT,
            user_type TEXT,
            called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
    )
    conn.commit()
    conn.close()


def insert_log(
    db_path: str,
    api_name: str,
    caller_id: str | None,
    caller_name: str | None,
    dept: str | None,
    level: str | None,
    user_type: str | None,
) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        INSERT INTO api_call_logs (api_name, caller_id, caller_name, dept, level, user_type)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (api_name, caller_id, caller_name, dept, level, user_type),
    )
    conn.commit()
    conn.close()