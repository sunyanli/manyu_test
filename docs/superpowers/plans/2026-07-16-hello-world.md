# Hello World Cross-Repo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a cross-repository Hello World: `manyu_test` defines `greet()`, `manyu_test1` consumes it via cross-repo import.

**Architecture:** Upstream repo `manyu_test` exposes a `greet()` function in `hello.py`. Downstream repo `manyu_test1` imports it via `sys.path` injection, resolving the upstream worktree by relative path. No packaging, no pip install — pure path-based cross-repo linkage.

**Tech Stack:** Python 3.12, standard library only (`pathlib`, `sys`).

---

## Global Constraints

- Python 3.12+
- Zero external dependencies
- Cross-repo linkage via `sys.path` (not pip/packaging)
- Upstream API: `greet(name: str = "World") -> str` — backward-compatible, new params defaulted
- Git read-only throughout (no commits/pushes)

---

## Task 1: Upstream — `hello.py` in manyu_test

**Files:**
- Create: `manyu_test/hello.py`

**Interfaces:**
- Produces: `greet(name: str = "World") -> str`

- [ ] **Step 1: Write the module with `greet()` function**

```python
"""Hello World module — upstream definition for cross-repo collaboration demo."""


def greet(name: str = "World") -> str:
    """Return a greeting string.

    Args:
        name: The entity to greet. Defaults to "World".

    Returns:
        A greeting message.
    """
    return f"Hello, {name}!"


if __name__ == "__main__":
    print(greet())
```

- [ ] **Step 2: Verify the module runs standalone**

Run: `cd manyu_test && python3 hello.py`
Expected: prints `Hello, World!`

- [ ] **Step 3: Commit**

```bash
git add hello.py
git commit -m "feat: add hello.py with greet() function"
```

---

## Task 2: Downstream — `main.py` in manyu_test1

**Files:**
- Create: `manyu_test1/main.py`

**Interfaces:**
- Consumes: `greet(name: str = "World") -> str` from `manyu_test/hello.py`

- [ ] **Step 1: Write the consumer with cross-repo import**

```python
"""Consumer — imports greet() from manyu_test across repository boundaries."""

import sys
from pathlib import Path

# Cross-repo dependency: add manyu_test to the import path
_MANYU_TEST = Path(__file__).resolve().parent.parent / "manyu_test-cred-test-20260716022903"
if str(_MANYU_TEST) not in sys.path:
    sys.path.insert(0, str(_MANYU_TEST))

from hello import greet  # noqa: E402

if __name__ == "__main__":
    print(greet("World"))
```

- [ ] **Step 2: Verify cross-repo execution**

Run: `cd manyu_test1 && python3 main.py`
Expected: prints `Hello, World!`

- [ ] **Step 3: Commit**

```bash
git add main.py
git commit -m "feat: add main.py consuming greet() from manyu_test"
```

---

## Cross-Repo Integration Verification

- [ ] **Integration check: Confirm downstream correctly resolves upstream**

Run from `manyu_test1`:
```bash
python3 -c "
import sys
from pathlib import Path
_MANYU_TEST = Path('.').resolve().parent / 'manyu_test-cred-test-20260716022903'
sys.path.insert(0, str(_MANYU_TEST))
from hello import greet
assert greet('CrossRepo') == 'Hello, CrossRepo!'
print('PASS: cross-repo integration verified')
"
```
Expected: `PASS: cross-repo integration verified`

---

## Self-Review

### 1. Spec Coverage
- ✅ Hello World upstream definition (`hello.py` with `greet()`) → Task 1
- ✅ Downstream consumption across repository boundary → Task 2
- ✅ Integration verification → Integration check section

### 2. Placeholder Scan
- ✅ No TBD, TODO, or "implement later" present
- ✅ All code blocks contain complete, runnable code
- ✅ All commands have exact expected output

### 3. Type Consistency
- ✅ `greet(name: str = "World") -> str` — consistent across Task 1 (producer) and Task 2 (consumer)
- ✅ Path `manyu_test-cred-test-20260716022903` — consistent across all references