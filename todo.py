#!/usr/bin/env python3
"""Daily to-do CLI tool.

Manage daily to-do items with name and target user fields.
Items are persisted to a local JSON file.
"""

import argparse
import json
import os
import sys

DEFAULT_TODOS_PATH = "todos.json"


def _todos_path():
    """Return the path to the todos JSON file, respecting TODOS_PATH env var."""
    return os.environ.get("TODOS_PATH", DEFAULT_TODOS_PATH)


def load_todos():
    """Load todo items from the JSON persistence file.

    Returns:
        list[dict]: List of todo items, each with 'name' and 'user' keys.
    """
    path = _todos_path()
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_todos(todos):
    """Save todo items to the JSON persistence file.

    Args:
        todos: List of dicts with 'name' and 'user' keys.
    """
    path = _todos_path()
    with open(path, "w", encoding="utf-8") as f:
        json.dump(todos, f, ensure_ascii=False, indent=2)


def add_todo(name, user):
    """Add a new todo item for the given user.

    Args:
        name: The name/description of the todo item (must be non-empty).
        user: The target user for this todo item.

    Returns:
        str: Success message.

    Raises:
        SystemExit: If name is empty or a duplicate for the same user.
    """
    if not name.strip():
        print("Error: item name cannot be empty.", file=sys.stderr)
        sys.exit(1)

    todos = load_todos()
    for item in todos:
        if item["name"] == name and item["user"] == user:
            print(
                f"Error: item '{name}' for user '{user}' already exists.",
                file=sys.stderr,
            )
            sys.exit(1)

    item = {"name": name, "user": user}
    todos.append(item)
    save_todos(todos)
    print(f"Added: '{name}' for {user}")


def list_todos():
    """Print all todo items to stdout."""
    todos = load_todos()
    if not todos:
        print("No todo items.")
        return
    for i, item in enumerate(todos, start=1):
        print(f"{i}. [{item['user']}] {item['name']}")


def main():
    """Parse CLI arguments and dispatch to the appropriate command."""
    parser = argparse.ArgumentParser(
        description="Daily to-do CLI tool",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    # add subcommand
    add_parser = subparsers.add_parser("add", help="Add a new todo item")
    add_parser.add_argument("name", help="Name of the todo item")
    add_parser.add_argument(
        "--user", required=True, help="Target user for this todo item"
    )

    # list subcommand
    subparsers.add_parser("list", help="List all todo items")

    args = parser.parse_args()

    if args.command == "add":
        add_todo(args.name, args.user)
    elif args.command == "list":
        list_todos()


if __name__ == "__main__":
    main()