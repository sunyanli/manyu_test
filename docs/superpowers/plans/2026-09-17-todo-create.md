# Todo Create Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the minimal "create todo" closed loop — an internal user can add a todo item (name + description) that persists in the browser, with validation and feedback.

**Architecture:** Single-file static HTML page. No backend, no build step, no framework. State persists in `localStorage` under a fixed key. The UI is a form with two fields (title required, description optional), a submit button, and a toast for feedback. All logic lives in one IIFE in a `<script>` block.

**Tech Stack:** HTML5, vanilla JavaScript (ES2017+), CSS3, `localStorage` Web API. No dependencies. Runs in any modern browser.

## Global Constraints

- Single deliverable file: `todo.html` at repository root (already exists from prior stage; this plan refines/verifies it).
- No external dependencies — no CDNs, no npm, no build tools.
- No backend or server required; opens directly via `file://`.
- Title field is required, max 100 chars; description is optional, max 500 chars.
- Each todo gets a unique `id` (timestamp-based) and an ISO-8601 `createdAt`.
- Target users: internal users — UI copy in Chinese (zh-CN).
- Minimum closed loop: **creation only**. No list, edit, delete, or filter features in this plan.
- Persisted storage key: `aiwork_todos`.
- Git read-only constraint: no git write operations from this plan's execution.

---

## File Structure

Only one file is involved:

- **`todo.html`** (repository root) — the entire application: markup, styles, and JavaScript. Responsible for rendering the form, validating input, constructing the todo object, persisting it to `localStorage`, and showing toast feedback.

No other files are created or modified. Tests are manual/browser-based because the deliverable is a dependency-free static page (no test runner exists in this repo). Verification steps use the browser's DevTools console and `localStorage` inspection.

---

### Task 1: Form Markup & Styles

**Files:**
- Modify: `todo.html` (entire `<head>` + `<body>` markup section)

**Interfaces:**
- Produces: DOM element IDs that later JS depends on — `#todoForm` (form), `#title` (text input), `#description` (textarea), `#toast` (feedback div).

- [ ] **Step 1: Verify existing markup matches the required structure**

Open `todo.html` and confirm these elements exist with these IDs:

```html
<form id="todoForm" autocomplete="off">
    <input type="text" id="title" maxlength="100" required>
    <textarea id="description" maxlength="500"></textarea>
    <button type="submit" class="btn">保存待办</button>
</form>
<div id="toast" class="toast"></div>
```

The prior-stage `todo.html` already contains this structure. If any element is missing or ID differs, add/fix it. Otherwise no change is needed in this step.

- [ ] **Step 2: Verify styles cover form, inputs, button, and toast states**

Confirm the `<style>` block defines:
- `.container` — centered card
- `input[type="text"]`, `textarea` — full-width inputs with focus styles
- `.btn` — primary button with hover/active states
- `.toast`, `.toast.success`, `.toast.error` — feedback banner variants

These already exist in the prior artifact. No change needed unless a class is absent.

- [ ] **Step 3: Manual smoke check**

Open `todo.html` in a browser. Confirm the page renders a centered card titled "新增待办事项" with a name input, description textarea, and a "保存待办" button. The toast area should be invisible initially.

---

### Task 2: Persistence Layer (localStorage)

**Files:**
- Modify: `todo.html` — `<script>` block, the `getTodos` and `saveTodo` functions

**Interfaces:**
- Produces:
  - `getTodos() → Array<{id:number,title:string,description:string,createdAt:string}>` — reads and parses `localStorage['aiwork_todos']`; returns `[]` on missing/corrupt data.
  - `saveTodo(todo: {id,title,description,createdAt}) → void` — appends one todo and writes back.

- [ ] **Step 1: Write a failing console test for the persistence functions**

Open `todo.html` in a browser, then in DevTools Console run:

```js
// Reset state
localStorage.removeItem('aiwork_todos');

// Should start empty
console.assert(JSON.stringify(getTodos()) === '[]', 'empty initial state');

// Save one
saveTodo({ id: 1, title: '测试', description: '描述', createdAt: '2026-09-17T00:00:00.000Z' });

// Should now contain exactly one
const result = getTodos();
console.assert(result.length === 1, 'one item after save');
console.assert(result[0].title === '测试', 'title persisted');
console.assert(result[0].description === '描述', 'description persisted');
```

Expected: FAIL — `getTodos`/`saveTodo` are not in global scope (they live inside the IIFE).

> Note: Because the script is wrapped in an IIFE, these functions are not reachable from console. To make them testable, either (a) temporarily expose them on `window` during development, or (b) test via the UI path in Task 3. The canonical verification is the integration path (Task 3, Step 2). If using approach (a), add `window.getTodos = getTodos; window.saveTodo = saveTodo;` inside the IIFE for testing, then remove before finalizing.

- [ ] **Step 2: Verify the persistence implementation**

Confirm the `<script>` block contains:

```js
const STORAGE_KEY = 'aiwork_todos';

function getTodos() {
    try {
        const data = localStorage.getItem(STORAGE_KEY);
        return data ? JSON.parse(data) : [];
    } catch {
        return [];
    }
}

function saveTodo(todo) {
    const todos = getTodos();
    todos.push(todo);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(todos));
}
```

The prior-stage `todo.html` already contains this exact implementation (lines ~165-180). If missing or differing, apply the above. The `try/catch` guards against corrupt JSON.

- [ ] **Step 3: Run the console test again**

With functions exposed (or via UI path), re-run the assertions from Step 1.

Expected: PASS — all `console.assert` calls produce no output (assertions hold).

- [ ] **Step 4: Verify corruption recovery**

In DevTools Console:

```js
localStorage.setItem('aiwork_todos', 'not-json{{{');
console.assert(getTodos().length === 0, 'corrupt data returns empty array');
```

Expected: PASS — no throw, returns `[]`.

---

### Task 3: Submit Handler — Validation, Object Construction, Feedback

**Files:**
- Modify: `todo.html` — `<script>` block, the `form.addEventListener('submit', ...)` handler and `showToast`

**Interfaces:**
- Consumes: `getTodos()`, `saveTodo(todo)` from Task 2; DOM IDs from Task 1.
- Produces: A working submit flow: validate title → build todo object → persist → toast success → reset form.

- [ ] **Step 1: Write a failing manual test for the submit flow**

Open `todo.html` in browser. In DevTools Console preset clean state:

```js
localStorage.removeItem('aiwork_todos');
```

Leave the title field empty, click "保存待办".

Expected behavior: red error toast "请输入事项名称", title input gets focus, nothing saved.

Then type "完成报告" in title, "周五前提交" in description, click "保存待办".

Expected behavior: green success toast "✅ 添加成功！", form resets, one item in storage.

Verify in Console:

```js
const todos = JSON.parse(localStorage.getItem('aiwork_todos'));
console.assert(todos.length === 1, 'one todo saved');
console.assert(todos[0].title === '完成报告', 'title correct');
console.assert(todos[0].description === '周五前提交', 'description correct');
console.assert(typeof todos[0].id === 'number', 'id is number');
console.assert(!isNaN(new Date(todos[0].createdAt).getTime()), 'createdAt is valid ISO date');
```

- [ ] **Step 2: Verify the submit handler implementation**

Confirm the `<script>` block contains:

```js
function showToast(message, type) {
    toast.textContent = message;
    toast.className = 'toast ' + type;
    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}

form.addEventListener('submit', function(e) {
    e.preventDefault();

    const title = titleInput.value.trim();
    if (!title) {
        showToast('请输入事项名称', 'error');
        titleInput.focus();
        return;
    }

    const todo = {
        id: Date.now(),
        title: title,
        description: descInput.value.trim(),
        createdAt: new Date().toISOString()
    };

    saveTodo(todo);
    showToast('✅ 添加成功！', 'success');
    form.reset();
    titleInput.focus();
});
```

The prior-stage `todo.html` already contains this implementation (lines ~182-211). If any part differs or is missing, apply the above.

- [ ] **Step 3: Run the manual test from Step 1**

Perform both the empty-title and valid-submit scenarios.

Expected: PASS — all behaviors and console assertions hold.

- [ ] **Step 4: Verify multiple saves accumulate**

In Console reset, then submit two valid todos via the UI:

```js
localStorage.removeItem('aiwork_todos');
```

Submit "任务A" (no description), then "任务B" (description "详情B").

```js
const todos = JSON.parse(localStorage.getItem('aiwork_todos'));
console.assert(todos.length === 2, 'two todos accumulated');
console.assert(todos[0].title === '任务A', 'first todo');
console.assert(todos[1].title === '任务B', 'second todo');
console.assert(todos[1].description === '详情B', 'second description');
```

Expected: PASS — array grows; order preserved.

- [ ] **Step 5: Verify maxlength enforcement**

Type a 101-char string into the title field. Because `maxlength="100"` is set on the input, the browser truncates to 100 chars. Submit. Verify the saved title length is 100:

```js
const todos = JSON.parse(localStorage.getItem('aiwork_todos'));
const last = todos[todos.length - 1];
console.assert(last.title.length === 100, 'title capped at 100');
```

Expected: PASS.

- [ ] **Step 6: Commit (if git writes were permitted — they are NOT in this pipeline)**

> ⚠️ Git write operations are prohibited by the pipeline constraints. This step is documented for completeness; skip it. The plan executor in a normal context would run:
> ```bash
> git add todo.html
> git commit -m "feat: minimal todo create closed loop"
> ```

---

## Self-Review

**1. Spec coverage:**
- "新增待办事项" (add todo) → Task 3 submit handler builds and persists a todo object. ✅
- "事项名称和描述" (name + description) → Task 1 form has `#title` (required) and `#description` (optional). ✅
- "内部用户" (internal users) → zh-CN copy, simple form, no auth. ✅
- "最小闭环：仅创建" (minimum loop: creation only) → No list/edit/delete tasks present. ✅
- Persistence → Task 2 localStorage layer. ✅
- Validation → Task 3 empty-title check + maxlength. ✅
- Feedback → Task 3 toast success/error. ✅

No spec gaps.

**2. Placeholder scan:**
- No "TBD", "TODO", "implement later", "add validation", "handle edge cases", or "similar to Task N" found.
- Every code step shows the actual code.
- Every test step shows actual runnable assertions.

No placeholders.

**3. Type consistency:**
- `STORAGE_KEY = 'aiwork_todos'` — same in Task 2 and Task 3. ✅
- `getTodos()` returns array — used consistently. ✅
- `saveTodo(todo)` takes `{id,title,description,createdAt}` — Task 3 constructs exactly this shape. ✅
- DOM IDs `todoForm`, `title`, `description`, `toast` — defined in Task 1, consumed in Task 3. ✅
- `showToast(message, type)` — defined and called with `'success'`/`'error'` matching CSS classes `.toast.success`/`.toast.error`. ✅

No type mismatches.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-17-todo-create.md`. Two execution options:

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints.

> Pipeline auto-mode: proceed with **Inline Execution** (option 2) is the default when no human selects; however, this pipeline stage's deliverable is the plan document itself, which is now complete. Implementation execution belongs to the next pipeline stage.
