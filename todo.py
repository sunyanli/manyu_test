#!/usr/bin/env python3
"""TODO CLI — 简易待办事项管理工具（SQLite 持久化）"""

import argparse
import sqlite3
import sys

_DB_PATH = "todos.db"


def init_db() -> None:
    """幂等建表，可多次调用不报错。"""
    conn = sqlite3.connect(_DB_PATH)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS todos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT DEFAULT '',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
    )
    conn.commit()
    conn.close()


def add_todo(title: str, description: str = "") -> dict:
    """将待办事项写入 SQLite 并返回完整字典。空标题抛出 ValueError。"""
    if not title or not title.strip():
        raise ValueError("title 不能为空")

    conn = sqlite3.connect(_DB_PATH)
    cursor = conn.execute(
        "INSERT INTO todos (title, description) VALUES (?, ?)",
        (title.strip(), description),
    )
    row_id = cursor.lastrowid
    conn.commit()

    row = conn.execute(
        "SELECT id, title, description, created_at FROM todos WHERE id = ?",
        (row_id,),
    ).fetchone()
    conn.close()

    return {
        "id": row[0],
        "title": row[1],
        "description": row[2],
        "created_at": row[3],
    }


def main() -> None:
    """CLI 入口。"""
    parser = argparse.ArgumentParser(description="TODO CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # add 子命令
    add_parser = subparsers.add_parser("add", help="添加待办事项")
    add_parser.add_argument("title", help="待办事项标题")
    add_parser.add_argument("description", nargs="?", default="", help="待办事项描述（可选）")

    args = parser.parse_args()

    if args.command == "add":
        init_db()
        try:
            result = add_todo(args.title, args.description)
            print(f"已添加待办事项: #{result['id']} {result['title']} (创建时间: {result['created_at']})")
        except ValueError as e:
            print(f"错误: {e}", file=sys.stderr)
            sys.exit(1)


if __name__ == "__main__":
    main()