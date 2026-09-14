# Task 1 Report: Implement Todo CLI

## What was implemented

A daily to-do CLI tool (`todo.py`) with two subcommands:

- **`add <name> --user <user>`** — creates a todo item, persists to `todos.json`
- **`list`** — displays all persisted todo items in numbered format

### Files

| File | Purpose |
|------|---------|
| `todo.py` | CLI entry point: argparse dispatch, JSON persistence, add/list logic |
| `test_todo.py` | 7 integration tests via `subprocess` with isolated tempdirs |

### Design decisions

- **No external dependencies** — stdlib only (`argparse`, `json`, `os`, `sys`)
- **`TODOS_PATH` env var** — allows tests to use isolated JSON files without polluting the working directory
- **`--user` is required** — the spec examples all include it; argparse enforces this
- **Duplicate detection** — same `name` + same `user` combination is rejected
- **Empty name** — rejected with a clear stderr message
- **Empty list** — prints "No todo items." rather than crashing

## Test results

```
$ python3 -m unittest test_todo -v
test_add_creates_item ... ok
test_add_without_user ... ok
test_duplicate_rejected ... ok
test_empty_name_rejected ... ok
test_list_displays_items ... ok
test_list_empty ... ok
test_persistence_between_runs ... ok

Ran 7 tests in 0.400s
OK
```

### Smoke test against acceptance criteria

| Criterion | Result |
|-----------|--------|
| `add "Buy groceries" --user "张三"` | ✅ Creates item, persists to JSON |
| `list` displays all items | ✅ Numbered list with `[user] name` format |
| Persisted to `todos.json` | ✅ JSON file created and readable across runs |
| Duplicate rejection | ✅ Stderr: "already exists", exit code 1 |
| Empty name rejection | ✅ Stderr: "cannot be empty", exit code 1 |
| Tests cover all required cases | ✅ add, list, duplicate, empty name covered |
| All tests pass | ✅ 7/7 OK |

## TDD Evidence: RED → GREEN

### RED phase
```
$ python3 -m unittest test_todo -v
FAILED (failures=6)
```
All tests failed because `todo.py` did not exist yet.

### GREEN phase
After implementing `todo.py`:
```
$ python3 -m unittest test_todo -v
Ran 7 tests in 0.400s
OK
```

## Commit

- **SHA:** `6490a7c`
- **Subject:** `feat: Implement daily to-do CLI tool`
- **Files:** `todo.py` (113 lines), `test_todo.py` (100 lines)
- **Net:** +213 insertions

## Self-review findings

- All acceptance criteria met ✅
- snake_case naming throughout ✅
- All functions have docstrings ✅
- No external dependencies (stdlib only) ✅
- No overbuilding — only `add` and `list` as specified ✅
- Test isolation via `tempfile.mkdtemp()` and `TODOS_PATH` env var ✅
- Minor note: `test_add_without_user` relies on argparse default error behavior for missing `--user` — valid but could be fragile if argparse behavior changes

## Issues or concerns

None. The implementation is clean, tested, and meets all acceptance criteria.

## Review fix report (468142e)

### What was fixed

1. **Pytest-style rewrite** — Replaced `unittest.TestCase` class with plain `test_*` functions using raw `assert`. Removed `unittest` import. Tests now use `tempfile.TemporaryDirectory()` as a context manager instead of `setUp`/`tearDown`.

2. **`sys.exit(1)` → `ValueError`** — `add_todo()` now raises `ValueError` on validation failures (empty name, duplicate). The `try/except` + `sys.exit(1)` is moved to `main()`. This makes `add_todo()` import-safe.

3. **`name.strip()` consistency** — `add_todo()` now strips the name once at entry (`name = name.strip()`) and uses the stripped value for both the empty check and duplicate detection/storage.

4. **Misleading docstring** — Updated `test_add_without_user` docstring to: `"--user is required; omitting it should fail with a non-zero exit code."`

5. **Trailing newlines** — Added trailing newline to both `todo.py` and `test_todo.py`.

### Test results

```
$ python3 -c "
import test_todo, traceback
...
"
test_add_creates_item... ok
test_add_without_user... ok
test_duplicate_rejected... ok
test_empty_name_rejected... ok
test_list_displays_items... ok
test_list_empty... ok
test_persistence_between_runs... ok

7 run, 7 passed, 0 failed
```

### Concerns

- `pytest` is not installed in the environment; tests are plain `assert`-style functions but were verified with a simple inline runner. They are compatible with `pytest` once installed.

## Final review fix report (7dcd110)

### Issues addressed

| ID | Severity | Issue | Fix |
|----|----------|-------|-----|
| C1 | Critical | `load_todos()` crashes on corrupted JSON | Wrapped `json.load(f)` in `try/except json.JSONDecodeError`, prints to stderr and `sys.exit(1)` |
| I1 | Important | `save_todos()` not atomic — data loss risk | Write to `.tmp` file then `os.replace(tmp_path, path)` for atomic rename |
| I2 | Important | `test_persistence_between_runs` uses same runner | Two independent runners (`_run1` for add, `_run2` for list) clearly demonstrate cross-invocation persistence |
| I3 | Important | No unit tests — only subprocess CLI tests | Added `TestTodoFunctions(unittest.TestCase)` class with 7 unit tests covering `load_todos`, `save_todos`, `add_todo` |

### Files changed

| File | Changes |
|------|---------|
| `todo.py` | +1 import (`tempfile`), +5 lines try/except in `load_todos()`, +2 lines atomic write in `save_todos()` |
| `test_todo.py` | I2: 2-runner refactor; I3: +80 lines `TestTodoFunctions` class with 7 unit tests |

### Test results

```
Integration tests (subprocess CLI):  7/7 passed
Unit tests (TestTodoFunctions):      7/7 passed
Total:                              14/14 passed
```

### Unit test coverage (new)

| Test | What it covers |
|------|---------------|
| `test_load_todos_valid_json` | Parsing valid JSON into list[dict] |
| `test_load_todos_empty_file` | No file → returns `[]` |
| `test_load_todos_corrupted_json` | Corrupted JSON → `SystemExit(1)` (catches C1-class bugs) |
| `test_add_todo_valid` | Happy path: add, returns message, persists correctly |
| `test_add_todo_empty_name` | Empty name → `ValueError` |
| `test_add_todo_duplicate` | Same name+user → `ValueError` |
| `test_save_and_load_roundtrip` | `save_todos()` → `load_todos()` round-trip fidelity |

### Concerns

None. All fixes are minimal, targeted, and verified.