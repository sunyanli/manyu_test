# Plan: Daily To-Do CLI

## Goal
Help internal users record daily to-do items via a simple CLI tool.

## Global Constraints
- Language: Python 3 (no external dependencies beyond stdlib)
- Storage: local JSON file (`todos.json`)
- Code style: follow existing project conventions (snake_case, docstrings)
- Tests: pytest-style, covering happy path and edge cases

## Tasks

### Task 1: Implement Todo CLI
**Description:** Create a Python CLI tool that allows internal users to create and list daily to-do items. Each item has a name and a target user field.

**Acceptance Criteria:**
- [ ] `python todo.py add "Buy groceries" --user "张三"` creates a todo item with name "Buy groceries" and target user "张三"
- [ ] `python todo.py list` displays all todo items
- [ ] Items are persisted to `todos.json` between runs
- [ ] Duplicate item names for the same user are rejected
- [ ] Empty item names are rejected with an error message
- [ ] Tests cover: add, list, duplicate rejection, empty name rejection
- [ ] All tests pass with clean output

**Files:** `todo.py`, `test_todo.py`