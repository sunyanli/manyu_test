#!/usr/bin/env python3
"""todo — 内部用户日常待办记录工具（本期最小闭环：新增待办）。"""

import argparse
import json
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_FILE = "todos.json"


def load_todos(path: Path) -> list[dict]:
    """读取待办列表；文件不存在时返回空列表。"""
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def save_todos(path: Path, todos: list[dict]) -> None:
    """将待办列表以 UTF-8 JSON 写入文件。"""
    path.write_text(
        json.dumps(todos, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def add_todo(todos: list[dict], name: str, description: str = "") -> dict:
    """在待办列表末尾追加一条新待办，返回新记录。"""
    record = {
        "id": uuid.uuid4().hex,
        "name": name,
        "description": description,
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    todos.append(record)
    return record


def build_parser() -> argparse.ArgumentParser:
    """构建命令行解析器。"""
    parser = argparse.ArgumentParser(
        prog="todo", description="记录日常待办事项（本期仅支持新增）"
    )
    parser.add_argument(
        "--file", default=DEFAULT_FILE, help="待办存储文件路径（默认 %(default)s）"
    )
    sub = parser.add_subparsers(dest="command", required=True)
    p_add = sub.add_parser("add", help="新增待办事项")
    p_add.add_argument("name", help="事项名称")
    p_add.add_argument("description", nargs="?", default="", help="事项描述（可选）")
    return parser


def main(argv: list[str] | None = None) -> int:
    """CLI 入口：add 子命令写入一条待办并打印确认信息。"""
    parser = build_parser()
    args = parser.parse_args(argv)
    name = args.name.strip()
    if not name:
        parser.error("事项名称不能为空")
    path = Path(args.file)
    todos = load_todos(path)
    record = add_todo(todos, name, args.description.strip())
    save_todos(path, todos)
    print(f"已新增待办：{record['name']}（id={record['id']}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
