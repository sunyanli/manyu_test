# 俄罗斯方块（单文件 HTML）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付一个自包含的静态 `index.html`，双击（`file://`）即可开始并完成一局标准俄罗斯方块（完整手感 + 三种挑战模式），并附一份不被页面引用的 `README.md`。

**Architecture:** 单文件内部分为两个 `<script>` 区块，单向依赖：外壳（`Input → Renderer → Bootstrap`）依赖内核，内核不接触 DOM。内核（`CONST / Tetromino / Board / Randomizer / Scoring / Modes / GameState`）为纯逻辑，挂到 `window.Tetris = { Board, Tetromino, Randomizer, Scoring, Modes }`（设计 §6.11），使 Node 可抽取断言；渲染是状态的纯函数输出，不反向修改状态。

**Tech Stack:** 原生 HTML5 + CSS3 + 浏览器 Canvas 2D + ES2019 语法（classic script，**非** ES Module）。无 npm、无构建、测试框架为零，自测用一次性 Node 脚本（`node tools/selftest.mjs`）。

来源：`.agents/20261016-实现一个俄罗斯方块小游戏_直接在html/design.md`（需求澄清节点产出，用户已确认 Q1–Q3）。

---

## Global Constraints

以下为全项目级约束，**每个任务的要求都隐含包含本节**（数值与设计要求逐字一致）：

- 交付形态：单文件 `index.html` + 独立 `README.md`；`README.md` 不被页面引用（删掉它游戏照常运行）。
- 打开方式：双击 `file://` 直接可玩 → **禁止 `type="module"`**，禁止要求任何本地服务器、构建步骤、安装步骤。
- 零第三方运行时依赖：无 CDN、无外部字体/图片/音频/脚本；源码中不得出现 `<script src=`、`<link href=`、`http://`、`https://`、`import `（远程导入）。
- 无持久化：不得读写 `localStorage` / `sessionStorage` / `document.cookie`；最高分只保留在当前会话内存中。
- 无音效、无移动端触控：不得出现任何 `touchstart` / `touchmove` / `touchend` / `pointerdown` 触控监听，不得渲染屏幕按键。
- 界面文案：中文。
- 目标浏览器：Chrome / Edge / Firefox / Safari 近两个大版本，桌面优先；可用 `const`/`let`、箭头函数、模板字符串、`Object.assign`、`Array#includes`、`String#padStart`。
- 棋盘参数：`COLS = 10`、`ROWS = 20`、`HIDDEN_ROWS = 2`、`CELL = 30`（CSS 像素，按 `devicePixelRatio` 适配高清屏）。
- 计分（Guideline）：单/双/三/四行 = `100/300/500/800 × 当前等级`；软降 1 分/格；硬降 2 分/格；每 10 行升 1 级。
- 模式：马拉松（无尽）/ 40 行竞速（`SPRINT_LINES = 40`）/ 限时冲刺（`ULTRA_MS = 120000`）；起始等级可选 `1–15`。
- 手感参数：DAS `170ms`、ARR `50ms`、锁定延迟 `500ms`（最多 15 次移动/旋转重置锁延）、软降为当前重力的 20 倍、消行动画 `120ms`、`dt` 上限 `250ms`。
- 键位（基于 `event.code`）：`←/→` 移动、`↑`/`X` 顺时针、`Z`/`Ctrl` 逆时针、`↓` 软降、`Space` 硬降、`C`/`Shift` Hold、`P`/`Esc` 暂停、`Enter` 开始/重开。
- 范围外（YAGNI）：移动端触控与屏幕按键、音效、最高分持久化、多人对战、排行榜/账号、关卡编辑器、皮肤、成就、AI 自动游玩、PWA、多语言、存档续玩。
- 不引入 npm / 构建工具 / 测试框架；内核自测只用一次性 Node 断言脚本。

---

## 前置条件与验证降级路径

- **运行游戏**：只需要一个现代浏览器，双击 `index.html`。无任何安装步骤、无构建步骤。
- **开发期自测（可选）**：需要 Node ≥ 18，用于 `node tools/selftest.mjs` 与 `node --check`。它是**开发工具依赖，不是交付物的运行依赖**。
- **若执行环境没有 Node**（本仓库当前环境已实测：`node` / `nodejs` / `bun` / `deno` 均不存在，只有 `python3`），按下列**等价降级路径**执行，不得以“跑不了所以跳过”收尾：
  1. **语法 + 加载验证**：用浏览器打开 `index.html`，DevTools Console 无 `SyntaxError` / `ReferenceError`、页面正常渲染，即等价于 `node --check` + 加载成功；反之视为失败，按报错定位。
  2. **内核断言**：把 `tools/selftest.mjs` 中 `ok/eq/sortedCells` 与各任务断言块粘贴到该页面的 DevTools Console 执行——页面已加载内核，`CONST / Tetromino / Board / Randomizer / Scoring / Modes / GameState` 都在全局词法作用域内可直接引用；同时把脚本里的 `html` 定义替换为 `const html = document.documentElement.outerHTML;`，Task 9 的静态合规断言即可照常运行。浏览器控制台里的 `console.log('selftest: ...')` 汇总行与 Node 下一致。
  3. **README 断言**（Task 10）：无法在 Console 中执行，改为人工核对 Task 10 Step 4 清单。
  4. 若同时只有 `python3`，可用 `python3 -c` 复现 Task 9 的合规红线正则（无外链、无 `type="module"`、无触控、无本地存储）。

---

## File Structure

```
.
├── index.html         # 交付物 1：游戏本体，自包含、零依赖、双击即玩
├── README.md          # 交付物 2：玩法/键位/模式/参数说明（页面不引用）
└── tools/
    └── selftest.mjs   # 开发期自测脚本（非运行交付物；页面不引用，删除整个 tools/ 不影响游戏）
```

`index.html` 内部结构（分节注释固定，便于单人维护长文件）：

```
index.html
├── <head>  meta / title / <style>（主题变量、布局、覆盖层、按钮）
├── <body>  main#app
│            ├── canvas#board                        游戏区
│            ├── aside#sidebar
│            │     ├── canvas#hold / canvas#next
│            │     ├── #score / #level / #lines / #progress-label / #progress
│            │     └── 键位说明
│            └── #overlay（#overlay-title / #overlay-sub / #mode-list / #level-list）
├── <script id="tetris-core">   CONST → Tetromino → Board → Randomizer → Scoring → Modes → GameState → 导出
└── <script id="tetris-shell">  Input → Renderer → Bootstrap
```

拆分边界：**内核区块**（`tetris-core`）不得出现 `document` / `window` 之外的宿主依赖（只允许最后一行写 `window.Tetris`），保证可被 Node 沙箱整段执行；**外壳区块**（`tetris-shell`）持有 DOM、事件与主循环，不持有任何规则数值（数值全部来自 `CONST`）。

---

## 跨任务接口契约（Interfaces）

后续每个任务只写自己那一部分代码，下列名称/签名是任务之间的契约，**任何任务都不得改名或改变语义**：

| 模块 | 签名 | 任务 |
| --- | --- | --- |
| `CONST` | 对象常量：`COLS/ROWS/HIDDEN_ROWS/CELL/NEXT_COUNT/COLORS/GHOST_STROKE/SCORE_TABLE/SOFT_DROP_POINTS_PER_CELL/HARD_DROP_POINTS_PER_CELL/LEVEL_UP_LINES/START_LEVEL_MIN/START_LEVEL_MAX/LOCK_DELAY_MS/LOCK_RESET_LIMIT/CLEAR_ANIM_MS/DAS_MS/ARR_MS/SOFT_DROP_FACTOR/MAX_DT_MS/SPRINT_LINES/ULTRA_MS/MIN_GRAVITY_MS/KEYS` | 2 |
| `Tetromino` | `TYPES: string[]`、`SHAPES: {[t]:{size,rotations: {x,y}[4][]}}`、`cells(type, rot) -> {x,y}[]`、`kicksFor(type, from, to) -> [dx,dy][]`、`spawnX(type) -> number`、`spawnY() -> number` | 2 |
| `Board` | `create() -> board`、`clone(board)`、`collides(board, type, x, y, rot) -> bool`、`merge(board, type, x, y, rot) -> board`、`fullRows(board) -> number[]`、`clearRows(board, rows) -> board`、`clearLines(board) -> {board, cleared, rows}`、`isEmptyRow(board, r) -> bool`、常量 `COLS/ROWS/HIDDEN_ROWS/TOTAL_ROWS` | 3 |
| `Randomizer` | `create(seed?) -> {next(): string, peek(n): string[]}`，7-bag | 4 |
| `Scoring` | `linePoints(cleared, level) -> number`、`softDropPoints(cells) -> number`、`hardDropPoints(cells) -> number`、`levelFor(totalLines, startLevel) -> number`、`gravityMs(level) -> number`、`applyClear({score,lines,level,startLevel}, cleared) -> {score,lines,level}` | 5 |
| `Modes` | `list: Mode[]`、`byId(id) -> Mode`、`isOver(mode, state) -> bool`、`formatMs(ms) -> 'mm:ss.cc'`；`Mode = {id,label,goal,isFinished(state),resultText(state),progressLabel,progressText(state)}` | 6 |
| `GameState` | `STATUS{READY,PLAYING,PAUSED,GAME_OVER}`、`create() -> state`、`start(state, modeId, startLevel, seed?) -> state`、`update(state, dtMs) -> state`、`applyIntent(state, intent) -> state`、`ghostY(state) -> number`、`isGrounded(state) -> bool`、`spawn(state, forcedType?) -> state`、`hold(state) -> bool`、`hardDrop(state) -> number`、`lockPiece(state)`、`togglePause(state)`、`toReady(state) -> state` | 7 |
| 页面导出 | `window.Tetris = { Board, Tetromino, Randomizer, Scoring, Modes }` | 7 |
| `Input`（外壳） | `handleDown(code, isRepeat) -> intent\|null`、`handleUp(code) -> intent\|null`、`poll(dtMs) -> intent[]`、`reset()`、`actionFor(code) -> string\|null` | 8 |
| `Renderer`（外壳） | `attach({board, next, hold})`、`bindStats(els)`、`setup(canvas, cssW, cssH) -> ctx`、`draw(state, ui)` | 8 |

`intent` 形状：`{type:'move', dx}`、`{type:'rotate', dir}`、`{type:'softDrop', on}`、`{type:'hardDrop'}`、`{type:'hold'}`、`{type:'pause'}`。
`state` 关键字段：`status / mode / startLevel / board / piece{type,rot,x,y} / next / hold / canHold / score / lines / level / elapsedMs / topOut / dropAccMs / dropIntervalMs / softDropping / lockTimerMs / lockResets / grounded / clearing{rows,timerMs} / rng / message`。

---

## Task 1: 页面骨架与自测脚手架

**Files:**
- Create: `index.html`
- Create: `tools/selftest.mjs`

**Interfaces:**
- Consumes: 无
- Produces: `index.html` 中两个脚本区块 `<script id="tetris-core">` / `<script id="tetris-shell">` 与全部 DOM 元素 id（`board / next / hold / overlay / overlay-title / overlay-sub / mode-list / level-list / score / level / lines / progress-label / progress`）；`tools/selftest.mjs` 的抽取/断言能力（`extractScript(id)`、`ok()`、`eq()`、`// @@ASSERTIONS@@` 插入标记）

- [ ] **Step 1: 创建 index.html 骨架（本次不含游戏逻辑）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>俄罗斯方块 · 单文件版</title>
<style>
  :root {
    --bg: #070b18;
    --panel: #0f172a;
    --panel-border: rgba(148, 163, 184, 0.25);
    --text: #e2e8f0;
    --muted: #94a3b8;
    --accent: #22d3ee;
  }
  * { box-sizing: border-box; }
  html, body {
    margin: 0;
    height: 100%;
    background: radial-gradient(circle at 50% 0%, #16203c 0%, var(--bg) 60%);
    color: var(--text);
    font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  }
  #app {
    display: flex;
    gap: 18px;
    align-items: flex-start;
    justify-content: center;
    padding: 24px;
    min-height: 100%;
    position: relative;
  }
  #board {
    background: #0b1020;
    border: 1px solid var(--panel-border);
    border-radius: 10px;
    box-shadow: 0 0 32px rgba(34, 211, 238, 0.18);
  }
  #sidebar { display: flex; flex-direction: column; gap: 12px; width: 200px; }
  .panel {
    background: var(--panel);
    border: 1px solid var(--panel-border);
    border-radius: 10px;
    padding: 10px 12px;
  }
  .panel h2 {
    margin: 0 0 6px;
    font-size: 12px;
    letter-spacing: 2px;
    color: var(--muted);
    font-weight: 600;
  }
  .stats > div { display: flex; justify-content: space-between; padding: 2px 0; font-size: 14px; }
  .stats .k { color: var(--muted); }
  .stats .v { font-variant-numeric: tabular-nums; font-weight: 700; }
  .keys { font-size: 12px; color: var(--muted); line-height: 1.8; }
  #overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(3, 7, 18, 0.82);
    backdrop-filter: blur(2px);
  }
  #overlay.hidden { display: none; }
  .overlay-card {
    width: min(560px, 92vw);
    background: var(--panel);
    border: 1px solid var(--panel-border);
    border-radius: 14px;
    padding: 20px 22px;
    text-align: center;
  }
  .overlay-card h1 { margin: 0 0 6px; font-size: 22px; }
  #overlay-sub { margin: 0 0 14px; color: var(--muted); font-size: 14px; white-space: pre-line; }
  .choice-list { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-bottom: 12px; }
  .choice-list.hidden { display: none; }
  .choice {
    background: #111c33;
    color: var(--text);
    border: 1px solid var(--panel-border);
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 13px;
    cursor: pointer;
    font-family: inherit;
  }
  .choice.active { border-color: var(--accent); color: var(--accent); box-shadow: 0 0 12px rgba(34, 211, 238, 0.35); }
  .hint { margin: 0; font-size: 12px; color: var(--muted); }
</style>
</head>
<body>
<main id="app">
  <canvas id="board" aria-label="游戏区"></canvas>

  <aside id="sidebar">
    <section class="panel">
      <h2>HOLD</h2>
      <canvas id="hold" aria-label="暂存区"></canvas>
    </section>
    <section class="panel">
      <h2>NEXT</h2>
      <canvas id="next" aria-label="预览区"></canvas>
    </section>
    <section class="panel stats">
      <div><span class="k">分数</span><span class="v" id="score">0</span></div>
      <div><span class="k">等级</span><span class="v" id="level">1</span></div>
      <div><span class="k">消行</span><span class="v" id="lines">0</span></div>
      <div><span class="k" id="progress-label">等级</span><span class="v" id="progress">1</span></div>
    </section>
    <section class="panel keys">
      ← → 移动<br />
      ↑ / X 顺时针<br />
      Z / Ctrl 逆时针<br />
      ↓ 软降　Space 硬降<br />
      C / Shift 暂存<br />
      P 暂停　Enter 开始
    </section>
  </aside>

  <div id="overlay">
    <div class="overlay-card">
      <h1 id="overlay-title">俄罗斯方块</h1>
      <p id="overlay-sub">选择模式与起始等级，按 Enter 开始</p>
      <div id="mode-list" class="choice-list"></div>
      <div id="level-list" class="choice-list"></div>
      <p class="hint">← → 选择模式 · ↑ ↓ 选择起始等级 · Enter 开始/重开 · P 暂停</p>
    </div>
  </div>
</main>

<script id="tetris-core">
  /* ==========================================================================
   * 内核（纯逻辑，不接触 DOM）
   * 顺序：CONST → Tetromino → Board → Randomizer → Scoring → Modes → GameState → 导出
   * ========================================================================== */
</script>

<script id="tetris-shell">
  /* ==========================================================================
   * 外壳（渲染 / 输入 / 主循环；不持有规则数值，全部取自 CONST）
   * 顺序：Input → Renderer → Bootstrap
   * ========================================================================== */
</script>
</body>
</html>
```

- [ ] **Step 2: 创建自测脚手架 tools/selftest.mjs**

```js
#!/usr/bin/env node
/**
 * 开发期自测脚本（不属于运行交付物：页面不引用它，删除整个 tools/ 目录不影响游戏）。
 * 用法：node tools/selftest.mjs
 * 原理：从 index.html 抽取 <script id="tetris-core"> 区块，在 node:vm 沙箱中执行，
 *       对内核纯逻辑（消行/碰撞/SRS 踢墙/计分/7-bag/模式判定/状态机）跑断言。
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import vm from 'node:vm';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const html = readFileSync(path.join(root, 'index.html'), 'utf8');

function extractScript(id) {
  const re = new RegExp('<script id="' + id + '">([\\s\\S]*?)<\\/script>');
  const m = html.match(re);
  if (!m) throw new Error('未找到 <script id="' + id + '"> 区块');
  return m[1];
}

// ---- 1) 在沙箱中执行内核区块 ----
const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
const ctx = vm.createContext(sandbox);

let coreError = null;
try {
  vm.runInContext(extractScript('tetris-core'), ctx, { filename: 'tetris-core.js' });
} catch (e) {
  coreError = e;
}

// 内核顶层声明位于该 realm 的全局词法作用域：后续脚本可直接读取，用于自测取用内部对象。
// 用 typeof 保护，未实现到该任务的模块取到 undefined，不影响已有断言。
function expose(names) {
  const body = names
    .map(function (n) { return n + ': typeof ' + n + " !== 'undefined' ? " + n + ' : undefined'; })
    .join(', ');
  return vm.runInContext('({' + body + '})', ctx, { filename: 'expose-core.js' });
}

const core = expose(['CONST', 'Tetromino', 'Board', 'Randomizer', 'Scoring', 'Modes', 'GameState']);
const { Board, Tetromino, Randomizer, Scoring, Modes, GameState, CONST } = core;

let passed = 0;
const failures = [];

function ok(name, cond, extra) {
  if (cond) { passed++; return; }
  failures.push(name + (extra ? ' :: ' + extra : ''));
}
function eq(name, actual, expected) {
  ok(name, JSON.stringify(actual) === JSON.stringify(expected),
    'actual=' + JSON.stringify(actual) + ' expected=' + JSON.stringify(expected));
}
function sortedCells(cells) {
  return cells.map(function (c) { return c.x + ',' + c.y; }).sort();
}

// @@ASSERTIONS@@

// ---- 汇总 ----
console.log('selftest: ' + passed + ' passed, ' + failures.length + ' failed');
if (failures.length > 0) {
  failures.forEach(function (f) { console.error('  FAIL ' + f); });
  process.exit(1);
}
```

- [ ] **Step 3: 在 `// @@ASSERTIONS@@` 标记处插入骨架断言**

```js
ok('内核脚本区块可执行且无错误', coreError === null, coreError && coreError.message);
ok('index.html 含外壳脚本区块', /<script id="tetris-shell">[\s\S]*<\/script>/.test(html));
['board', 'next', 'hold', 'overlay', 'overlay-title', 'overlay-sub', 'mode-list', 'level-list', 'score', 'level', 'lines', 'progress-label', 'progress']
  .forEach(function (id) {
    ok('存在元素 #' + id, new RegExp('id="' + id + '"').test(html));
  });
```

- [ ] **Step 4: 运行自测脚本**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: 15 passed, 0 failed`（验收条件是 **failed 为 0**，无 `FAIL` 行）

- [ ] **Step 5: 人工确认页面可直接打开**

Run: `python3 -c "print(open('index.html', encoding='utf-8').read().count('<script'))"` → Expected: `2`
说明：本步骤只做静态确认；真实“双击打开”验收在 Task 10 Step 4 的手工清单中完成（需在有图形界面的环境）。

- [ ] **Step 6: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat: 单文件页面骨架与内核自测脚手架"
```

---

## Task 2: CONST 常量与 Tetromino（7 方块 4 旋转态 + SRS 踢墙表）

**Files:**
- Modify: `index.html`（`<script id="tetris-core">` 区块内新增 `CONST`、`Tetromino`）
- Modify: `tools/selftest.mjs`（`// @@ASSERTIONS@@` 处新增断言）

**Interfaces:**
- Consumes: 无
- Produces: `CONST`（见契约表）、`Tetromino.TYPES`、`Tetromino.SHAPES[type].size`、`Tetromino.SHAPES[type].rotations[rot]`（`{x,y}[]`）、`Tetromino.cells(type, rot)`、`Tetromino.kicksFor(type, from, to)`、`Tetromino.spawnX(type)`、`Tetromino.spawnY()`

- [ ] **Step 1: 写入 CONST 常量**

把下面内容追加到 `<script id="tetris-core">` 区块内：

```js
  /* ========== 常量 ========== */
  const CONST = {
    COLS: 10,
    ROWS: 20,
    HIDDEN_ROWS: 2,
    CELL: 30,
    NEXT_COUNT: 3,

    COLORS: {
      I: '#22d3ee', O: '#facc15', T: '#c084fc',
      S: '#4ade80', Z: '#f87171', J: '#60a5fa', L: '#fb923c'
    },
    GHOST_STROKE: 'rgba(226, 232, 240, 0.45)',

    SCORE_TABLE: { 1: 100, 2: 300, 3: 500, 4: 800 },
    SOFT_DROP_POINTS_PER_CELL: 1,
    HARD_DROP_POINTS_PER_CELL: 2,
    LEVEL_UP_LINES: 10,
    START_LEVEL_MIN: 1,
    START_LEVEL_MAX: 15,

    LOCK_DELAY_MS: 500,
    LOCK_RESET_LIMIT: 15,
    CLEAR_ANIM_MS: 120,
    DAS_MS: 170,
    ARR_MS: 50,
    SOFT_DROP_FACTOR: 20,
    MAX_DT_MS: 250,
    MIN_GRAVITY_MS: 16,

    SPRINT_LINES: 40,
    ULTRA_MS: 120000,

    KEYS: {
      MOVE_LEFT: ['ArrowLeft'],
      MOVE_RIGHT: ['ArrowRight'],
      ROTATE_CW: ['ArrowUp', 'KeyX'],
      ROTATE_CCW: ['KeyZ', 'ControlLeft', 'ControlRight'],
      SOFT_DROP: ['ArrowDown'],
      HARD_DROP: ['Space'],
      HOLD: ['KeyC', 'ShiftLeft', 'ShiftRight'],
      PAUSE: ['KeyP', 'Escape'],
      START: ['Enter']
    }
  };
```

- [ ] **Step 2: 写入 Tetromino（基础矩阵 + 预计算 4 个旋转态 + SRS 踢墙表）**

```js
  /* ========== 方块定义与 SRS 旋转/踢墙表 ========== */
  const Tetromino = (function () {
    // 出生姿态（行、列坐标 -> 单元格），3x3 或 4x4 包围盒
    const BASE = {
      I: { size: 4, cells: [[0, 1], [1, 1], [2, 1], [3, 1]] },
      O: { size: 2, cells: [[0, 0], [1, 0], [0, 1], [1, 1]] },
      T: { size: 3, cells: [[1, 0], [0, 1], [1, 1], [2, 1]] },
      S: { size: 3, cells: [[1, 0], [2, 0], [0, 1], [1, 1]] },
      Z: { size: 3, cells: [[0, 0], [1, 0], [1, 1], [2, 1]] },
      J: { size: 3, cells: [[0, 0], [0, 1], [1, 1], [2, 1]] },
      L: { size: 3, cells: [[2, 0], [0, 1], [1, 1], [2, 1]] }
    };

    const TYPES = ['I', 'O', 'T', 'S', 'Z', 'J', 'L'];

    // 顺时针旋转：(x, y) -> (size - 1 - y, x)
    function rotateCW(cells, size) {
      return cells.map(function (c) { return [size - 1 - c[1], c[0]]; });
    }

    function buildRotations(def) {
      const out = [];
      let cells = def.cells.map(function (c) { return c.slice(); });
      if (def.size === 2) {
        const flat = cells.map(function (c) { return { x: c[0], y: c[1] }; });
        return [flat, flat, flat, flat];
      }
      for (let i = 0; i < 4; i++) {
        out.push(cells.map(function (c) { return { x: c[0], y: c[1] }; }));
        cells = rotateCW(cells, def.size);
      }
      return out;
    }

    const SHAPES = {};
    TYPES.forEach(function (t) {
      SHAPES[t] = { size: BASE[t].size, rotations: buildRotations(BASE[t]) };
    });

    // SRS 官方偏移表为「y 轴向上」，本实现屏幕坐标 y 轴向下，故 dy 全部取反。
    // 键 "起始态>目标态"（旋转态 0=出生, 1=R, 2=2, 3=L），值 = 依次试探的 [dx, dy]。
    const KICKS_JLSTZ = {
      '0>1': [[0, 0], [-1, 0], [-1, -1], [0, 2], [-1, 2]],
      '1>0': [[0, 0], [1, 0], [1, 1], [0, -2], [1, -2]],
      '1>2': [[0, 0], [1, 0], [1, 1], [0, -2], [1, -2]],
      '2>1': [[0, 0], [-1, 0], [-1, -1], [0, 2], [-1, 2]],
      '2>3': [[0, 0], [1, 0], [1, -1], [0, 2], [1, 2]],
      '3>2': [[0, 0], [-1, 0], [-1, 1], [0, -2], [-1, -2]],
      '3>0': [[0, 0], [-1, 0], [-1, 1], [0, -2], [-1, -2]],
      '0>3': [[0, 0], [1, 0], [1, -1], [0, 2], [1, 2]]
    };
    const KICKS_I = {
      '0>1': [[0, 0], [-2, 0], [1, 0], [-2, 1], [1, -2]],
      '1>0': [[0, 0], [2, 0], [-1, 0], [2, -1], [-1, 2]],
      '1>2': [[0, 0], [-1, 0], [2, 0], [-1, -2], [2, 1]],
      '2>1': [[0, 0], [1, 0], [-2, 0], [1, 2], [-2, -1]],
      '2>3': [[0, 0], [2, 0], [-1, 0], [2, -1], [-1, 2]],
      '3>2': [[0, 0], [-2, 0], [1, 0], [-2, 1], [1, -2]],
      '3>0': [[0, 0], [1, 0], [-2, 0], [1, 2], [-2, -1]],
      '0>3': [[0, 0], [-1, 0], [2, 0], [-1, -2], [2, 1]]
    };

    const SPAWN_X = { I: 3, O: 4, T: 3, S: 3, Z: 3, J: 3, L: 3 };

    function kicksFor(type, from, to) {
      if (type === 'O') return [[0, 0]];
      const table = type === 'I' ? KICKS_I : KICKS_JLSTZ;
      return table[from + '>' + to] || [[0, 0]];
    }
    function cells(type, rot) {
      const rotations = SHAPES[type].rotations;
      return rotations[((rot % 4) + 4) % 4];
    }
    function spawnX(type) { return SPAWN_X[type]; }
    function spawnY() { return 0; } // 恰好落在 2 行隐藏缓冲区内

    return {
      TYPES: TYPES, SHAPES: SHAPES, SPAWN_X: SPAWN_X,
      cells: cells, kicksFor: kicksFor, spawnX: spawnX, spawnY: spawnY
    };
  })();
```

- [ ] **Step 3: 在 `// @@ASSERTIONS@@` 处插入 Tetromino 断言**

```js
// ===== Task 2: CONST / Tetromino =====
eq('7 种方块', Tetromino.TYPES.length, 7);
Tetromino.TYPES.forEach(function (t) {
  const shape = Tetromino.SHAPES[t];
  eq(t + ' 有 4 个旋转态', shape.rotations.length, 4);
  for (let r = 0; r < 4; r++) {
    const list = Tetromino.cells(t, r);
    eq(t + ' 旋转态 ' + r + ' 为 4 格', list.length, 4);
    ok(t + ' 旋转态 ' + r + ' 无重复格', sortedCells(list).length === new Set(sortedCells(list)).size);
  }
  ok(t + ' 颜色已定义', typeof CONST.COLORS[t] === 'string');
});
eq('O 四个旋转态相同', sortedCells(Tetromino.cells('O', 0)), sortedCells(Tetromino.cells('O', 3)));
eq('I 旋转态 1 为竖条', sortedCells(Tetromino.cells('I', 1)), ['2,0', '2,1', '2,2', '2,3']);
eq('T 旋转态 1 正确', sortedCells(Tetromino.cells('T', 1)), ['1,0', '1,1', '1,2', '2,1']);
eq('旋转态 5 回绕到 1', sortedCells(Tetromino.cells('T', 5)), sortedCells(Tetromino.cells('T', 1)));
const TRANSITIONS = [[0, 1], [1, 0], [1, 2], [2, 1], [2, 3], [3, 2], [3, 0], [0, 3]];
['I', 'T', 'S', 'Z', 'J', 'L'].forEach(function (t) {
  TRANSITIONS.forEach(function (tr) {
    const kicks = Tetromino.kicksFor(t, tr[0], tr[1]);
    eq(t + ' ' + tr[0] + '>' + tr[1] + ' 踢墙候选 5 个', kicks.length, 5);
    eq(t + ' ' + tr[0] + '>' + tr[1] + ' 首个候选为 (0,0)', kicks[0], [0, 0]);
    ok(t + ' ' + tr[0] + '>' + tr[1] + ' 候选位移在 SRS 量级内', kicks.every(function (k) {
      return Math.abs(k[0]) <= 2 && Math.abs(k[1]) <= 2;
    }));
  });
});
['I', 'O', 'T', 'S', 'Z', 'J', 'L'].forEach(function (t) {
  ok(t + ' 出生列在界内', Tetromino.spawnX(t) >= 0 && Tetromino.spawnX(t) < CONST.COLS);
});
eq('I 的 0>1 第 2 候选为 (-2,0)', Tetromino.kicksFor('I', 0, 1)[1], [-2, 0]);
eq('O 不旋转（唯一候选）', Tetromino.kicksFor('O', 0, 1), [[0, 0]]);
```

- [ ] **Step 4: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0** 即通过；本任务结束后 `passed` ≥ 210）

- [ ] **Step 5: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 常量与 7 种方块的 SRS 旋转/踢墙表"
```

---

## Task 3: Board（碰撞 / 合入 / 消行，纯函数）

**Files:**
- Modify: `index.html`（内核区块新增 `Board`）
- Modify: `tools/selftest.mjs`

**Interfaces:**
- Consumes: `CONST.COLS/ROWS/HIDDEN_ROWS`、`Tetromino.cells`
- Produces: `Board.create/clone/collides/merge/fullRows/clearRows/clearLines/isEmptyRow`，常量 `Board.COLS/ROWS/HIDDEN_ROWS/TOTAL_ROWS`（`TOTAL_ROWS = 22`），棋盘为 `TOTAL_ROWS × COLS` 的二维数组（元素为 `null` 或方块类型字符串）；`y < 0` 视为合法（隐藏缓冲区上方），`y >= TOTAL_ROWS` 为越界

- [ ] **Step 1: 写入 Board**

```js
  /* ========== 棋盘（纯函数） ========== */
  const Board = (function () {
    const COLS = CONST.COLS;
    const ROWS = CONST.ROWS;
    const HIDDEN_ROWS = CONST.HIDDEN_ROWS;
    const TOTAL_ROWS = ROWS + HIDDEN_ROWS;

    function create() {
      const board = [];
      for (let r = 0; r < TOTAL_ROWS; r++) board.push(new Array(COLS).fill(null));
      return board;
    }

    function clone(board) {
      return board.map(function (row) { return row.slice(); });
    }

    function collides(board, type, x, y, rot) {
      const list = Tetromino.cells(type, rot);
      for (let i = 0; i < list.length; i++) {
        const cx = x + list[i].x;
        const cy = y + list[i].y;
        if (cx < 0 || cx >= COLS) return true;
        if (cy >= TOTAL_ROWS) return true;
        if (cy >= 0 && board[cy][cx] !== null) return true;
      }
      return false;
    }

    function merge(board, type, x, y, rot) {
      const next = clone(board);
      const list = Tetromino.cells(type, rot);
      for (let i = 0; i < list.length; i++) {
        const cx = x + list[i].x;
        const cy = y + list[i].y;
        if (cy >= 0 && cy < TOTAL_ROWS && cx >= 0 && cx < COLS) next[cy][cx] = type;
      }
      return next;
    }

    function fullRows(board) {
      const rows = [];
      for (let r = 0; r < TOTAL_ROWS; r++) {
        let full = true;
        for (let c = 0; c < COLS; c++) {
          if (board[r][c] === null) { full = false; break; }
        }
        if (full) rows.push(r);
      }
      return rows;
    }

    function clearRows(board, rows) {
      const set = {};
      rows.forEach(function (r) { set[r] = true; });
      const kept = [];
      for (let r = 0; r < TOTAL_ROWS; r++) {
        if (!set[r]) kept.push(board[r].slice());
      }
      while (kept.length < TOTAL_ROWS) kept.unshift(new Array(COLS).fill(null));
      return kept;
    }

    function clearLines(board) {
      const rows = fullRows(board);
      if (rows.length === 0) return { board: board, cleared: 0, rows: [] };
      return { board: clearRows(board, rows), cleared: rows.length, rows: rows };
    }

    function isEmptyRow(board, r) {
      for (let c = 0; c < COLS; c++) if (board[r][c] !== null) return false;
      return true;
    }

    return {
      create: create, clone: clone, collides: collides, merge: merge,
      fullRows: fullRows, clearRows: clearRows, clearLines: clearLines,
      isEmptyRow: isEmptyRow,
      COLS: COLS, ROWS: ROWS, HIDDEN_ROWS: HIDDEN_ROWS, TOTAL_ROWS: TOTAL_ROWS
    };
  })();
```

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入 Board 断言**

```js
// ===== Task 3: Board =====
eq('棋盘高度 = ROWS + HIDDEN_ROWS', Board.TOTAL_ROWS, 22);
eq('新棋盘 22 行', Board.create().length, 22);
ok('新棋盘全空', Board.create().every(function (row) { return row.every(function (v) { return v === null; }); }));
ok('越左越界碰撞', Board.collides(Board.create(), 'O', -1, 0, 0) === true);
ok('越右越界碰撞', Board.collides(Board.create(), 'O', 9, 0, 0) === true);
ok('越底越界碰撞', Board.collides(Board.create(), 'O', 0, 21, 0) === true);
ok('隐藏缓冲区上方（y<0）不算越界', Board.collides(Board.create(), 'O', 0, -1, 0) === false);
ok('空棋盘出生点不碰撞', Board.collides(Board.create(), 'T', Tetromino.spawnX('T'), Tetromino.spawnY(), 0) === false);

let occupied = Board.create();
occupied[21][0] = 'I';
ok('与已固定格重叠碰撞', Board.collides(occupied, 'O', 0, 20, 0) === true);
ok('与已固定格相邻不碰撞', Board.collides(occupied, 'O', 1, 20, 0) === false);

const mergeBase = Board.create();
const merged = Board.merge(mergeBase, 'O', 4, 20, 0);
eq('合入后 4 格', merged[20].filter(function (v) { return v !== null; }).length + merged[21].filter(function (v) { return v !== null; }).length, 4);
eq('合入类型正确', merged[20][4], 'O');
ok('merge 不修改入参棋盘', mergeBase.every(function (row) { return row.every(function (v) { return v === null; }); }) && merged !== mergeBase);

// 单行消除
const oneRow = Board.create();
for (let c = 0; c < 10; c++) oneRow[21][c] = 'J';
oneRow[20][3] = 'S';
const clearedOne = Board.clearLines(oneRow);
eq('单行消除 cleared=1', clearedOne.cleared, 1);
eq('消除行索引', clearedOne.rows, [21]);
ok('上方方块整体下移', clearedOne.board[21][3] === 'S');
ok('消除后顶行为空', Board.isEmptyRow(clearedOne.board, 0));

// 四行同时消除
const fourRows = Board.create();
for (let r = 18; r < 22; r++) for (let c = 0; c < 10; c++) fourRows[r][c] = 'Z';
fourRows[17][0] = 'T';
const clearedFour = Board.clearLines(fourRows);
eq('四行消除 cleared=4', clearedFour.cleared, 4);
eq('四行消除索引', clearedFour.rows, [18, 19, 20, 21]);
ok('四行消除后 4 行下移', clearedFour.board[21][0] === 'T' && clearedFour.board[20][0] === null);
ok('四行消除后棋盘尺寸不变', clearedFour.board.length === 22 && clearedFour.board[0].length === 10);

// 不满行不清除
const notFull = Board.create();
notFull[21][9] = null;
for (let c = 0; c < 9; c++) notFull[21][c] = 'L';
eq('未满行 cleared=0', Board.clearLines(notFull).cleared, 0);
eq('未满行无索引', Board.clearLines(notFull).rows, []);
```

- [ ] **Step 3: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 235）

- [ ] **Step 4: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 棋盘碰撞、合入与消行纯函数"
```

---

## Task 4: Randomizer（7-bag 随机）

**Files:**
- Modify: `index.html`（内核区块新增 `Randomizer`）
- Modify: `tools/selftest.mjs`

**Interfaces:**
- Consumes: `Tetromino.TYPES`、`CONST.NEXT_COUNT`
- Produces: `Randomizer.create(seed?) -> { next(), peek(n) }`（`next()` 取下一块并返回类型；`peek(n)` 只读预览、不消费；相同 `seed` 产生相同序列）

- [ ] **Step 1: 写入 Randomizer**

```js
  /* ========== 7-bag 随机（可注入种子，便于自测复现） ========== */
  const Randomizer = (function () {
    const TYPES = Tetromino.TYPES;

    function mulberry32(seed) {
      let a = seed >>> 0;
      return function () {
        a = (a + 0x6D2B79F5) >>> 0;
        let t = a;
        t = Math.imul(t ^ (t >>> 15), t | 1);
        t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
      };
    }

    function create(seed) {
      const rand = mulberry32(typeof seed === 'number' ? seed : Date.now());
      let bag = [];

      function shuffle(list) {
        for (let i = list.length - 1; i > 0; i--) {
          const j = Math.floor(rand() * (i + 1));
          const tmp = list[i]; list[i] = list[j]; list[j] = tmp;
        }
        return list;
      }
      function refill() { bag = bag.concat(shuffle(TYPES.slice())); }
      function next() { if (bag.length === 0) refill(); return bag.shift(); }
      function peek(n) {
        const k = n || CONST.NEXT_COUNT;
        while (bag.length < k) refill();
        return bag.slice(0, k);
      }

      return { next: next, peek: peek };
    }

    return { create: create };
  })();
```

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入 Randomizer 断言**

```js
// ===== Task 4: Randomizer =====
const rngA = Randomizer.create(20261016);
const draws = [];
for (let i = 0; i < 70; i++) draws.push(rngA.next());
eq('70 次抽取共 70 个', draws.length, 70);
ok('每个 7 连段恰为 7 种各一次',
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9].every(function (b) {
    const seg = draws.slice(b * 7, b * 7 + 7);
    return new Set(seg).size === 7 && seg.every(function (t) { return Tetromino.TYPES.indexOf(t) >= 0; });
  }));
Tetromino.TYPES.forEach(function (t) {
  eq('70 次抽取中 ' + t + ' 出现 10 次', draws.filter(function (d) { return d === t; }).length, 10);
});

const rngB = Randomizer.create(20261016);
const drawsB = [];
for (let i = 0; i < 14; i++) drawsB.push(rngB.next());
eq('同种子序列一致', drawsB, draws.slice(0, 14));

const rngC = Randomizer.create(999);
const drawsC = [];
for (let i = 0; i < 14; i++) drawsC.push(rngC.next());
ok('不同种子序列不同', JSON.stringify(drawsC) !== JSON.stringify(drawsB));

const rngD = Randomizer.create(7);
const preview = rngD.peek(3);
eq('peek 返回 3 个', preview.length, 3);
eq('peek 不消费队列', rngD.peek(3), preview);
eq('peek 与后续 next 顺序一致', [rngD.next(), rngD.next(), rngD.next()], preview);
```

- [ ] **Step 3: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 250）

- [ ] **Step 4: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 7-bag 随机与可注入种子"
```

---

## Task 5: Scoring（计分表、软降/硬降加分、等级与重力曲线）

**Files:**
- Modify: `index.html`（内核区块新增 `Scoring`）
- Modify: `tools/selftest.mjs`

**Interfaces:**
- Consumes: `CONST.SCORE_TABLE/SOFT_DROP_POINTS_PER_CELL/HARD_DROP_POINTS_PER_CELL/LEVEL_UP_LINES/MIN_GRAVITY_MS`
- Produces: `Scoring.linePoints(cleared, level)`、`Scoring.softDropPoints(cells)`、`Scoring.hardDropPoints(cells)`、`Scoring.levelFor(totalLines, startLevel)`、`Scoring.gravityMs(level)`、`Scoring.applyClear(state, cleared) -> {score, lines, level}`

- [ ] **Step 1: 写入 Scoring**

```js
  /* ========== 计分 / 等级 / 重力 ========== */
  const Scoring = (function () {
    function linePoints(cleared, level) {
      return (CONST.SCORE_TABLE[cleared] || 0) * level;
    }
    function softDropPoints(cells) {
      return cells * CONST.SOFT_DROP_POINTS_PER_CELL;
    }
    function hardDropPoints(cells) {
      return cells * CONST.HARD_DROP_POINTS_PER_CELL;
    }
    function levelFor(totalLines, startLevel) {
      return startLevel + Math.floor(totalLines / CONST.LEVEL_UP_LINES);
    }
    // Guideline 速度曲线：(0.8 - (level-1)*0.007)^(level-1) 秒，下限 MIN_GRAVITY_MS
    function gravityMs(level) {
      const l = Math.max(1, Math.min(level, 20));
      const seconds = Math.pow(0.8 - (l - 1) * 0.007, l - 1);
      return Math.max(seconds * 1000, CONST.MIN_GRAVITY_MS);
    }
    // 消行用「消除瞬间」的等级计分，再用累计行数推进等级
    function applyClear(state, cleared) {
      const score = state.score + linePoints(cleared, state.level);
      const lines = state.lines + cleared;
      return { score: score, lines: lines, level: levelFor(lines, state.startLevel) };
    }

    return {
      linePoints: linePoints,
      softDropPoints: softDropPoints,
      hardDropPoints: hardDropPoints,
      levelFor: levelFor,
      gravityMs: gravityMs,
      applyClear: applyClear
    };
  })();
```

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入 Scoring 断言**

```js
// ===== Task 5: Scoring =====
eq('单行 100 x 等级', Scoring.linePoints(1, 1), 100);
eq('四行 800 x 等级', Scoring.linePoints(4, 3), 2400);
eq('零行不加分', Scoring.linePoints(0, 5), 0);
eq('软降 1 分/格', Scoring.softDropPoints(7), 7);
eq('硬降 2 分/格', Scoring.hardDropPoints(7), 14);
eq('每 10 行升 1 级', Scoring.levelFor(10, 1), 2);
eq('起始等级作为基准', Scoring.levelFor(0, 15), 15);
eq('起始等级 5 消 25 行', Scoring.levelFor(25, 5), 7);
eq('等级 1 重力 1000ms', Scoring.gravityMs(1), 1000);
ok('等级越高越快', Scoring.gravityMs(1) > Scoring.gravityMs(2) && Scoring.gravityMs(2) > Scoring.gravityMs(5) && Scoring.gravityMs(5) > Scoring.gravityMs(10));
ok('高速档不低于下限', Scoring.gravityMs(10) >= 16 && Scoring.gravityMs(15) === 16 && Scoring.gravityMs(20) === 16);
eq('消行累计分数与等级', Scoring.applyClear({ score: 0, lines: 0, level: 1, startLevel: 1 }, 4), { score: 800, lines: 4, level: 1 });
eq('达 10 行升级', Scoring.applyClear({ score: 800, lines: 6, level: 1, startLevel: 1 }, 4), { score: 1600, lines: 10, level: 2 });
eq('升级后按新等级计分', Scoring.applyClear({ score: 1600, lines: 10, level: 2, startLevel: 1 }, 1), { score: 1800, lines: 11, level: 2 });
```

- [ ] **Step 3: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 265）

- [ ] **Step 4: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 计分表、等级推进与重力曲线"
```

---

## Task 6: Modes（马拉松 / 40 行竞速 / 限时冲刺，数据化配置）

**Files:**
- Modify: `index.html`（内核区块新增 `Modes`）
- Modify: `tools/selftest.mjs`

**Interfaces:**
- Consumes: `CONST.SPRINT_LINES/ULTRA_MS`
- Produces: `Modes.list`（顺序固定 `marathon, sprint, ultra`）、`Modes.byId(id)`、`Modes.isOver(mode, state)`、`Modes.formatMs(ms)`；每个 `Mode` 含 `id/label/goal/isFinished(state)/resultText(state)/progressLabel/progressText(state)`。`isFinished` 只判定「模式目标」；「堆顶」由 `GameState` 以 `state.topOut` 判定，二者由 `Modes.isOver` 合并

- [ ] **Step 1: 写入 Modes**

```js
  /* ========== 挑战模式（数据化配置，核心规则不分叉） ========== */
  const Modes = (function () {
    function pad2(n) { return String(n).padStart(2, '0'); }

    // 毫秒 -> mm:ss.cc（用于 Sprint 用时与 Ultra 倒计时展示）
    function formatMs(ms) {
      const total = Math.max(0, Math.floor(ms));
      const m = Math.floor(total / 60000);
      const s = Math.floor((total % 60000) / 1000);
      const cs = Math.floor((total % 1000) / 10);
      return pad2(m) + ':' + pad2(s) + '.' + pad2(cs);
    }

    const list = [
      {
        id: 'marathon',
        label: '马拉松',
        goal: '无尽冲分，堆顶结束',
        isFinished: function () { return false; },
        resultText: function (s) {
          return '总分 ' + s.score + '｜等级 ' + s.level + '｜消行 ' + s.lines;
        },
        progressLabel: '等级',
        progressText: function (s) { return String(s.level); }
      },
      {
        id: 'sprint',
        label: '40 行竞速',
        goal: '最快消满 40 行',
        isFinished: function (s) { return s.lines >= CONST.SPRINT_LINES; },
        resultText: function (s) {
          return '用时 ' + formatMs(s.elapsedMs) + '｜消行 ' + s.lines;
        },
        progressLabel: '用时',
        progressText: function (s) { return formatMs(s.elapsedMs); }
      },
      {
        id: 'ultra',
        label: '限时冲刺',
        goal: '120 秒内冲最高分',
        isFinished: function (s) { return s.elapsedMs >= CONST.ULTRA_MS; },
        resultText: function (s) {
          return '总分 ' + s.score + '｜消行 ' + s.lines;
        },
        progressLabel: '剩余',
        progressText: function (s) { return formatMs(Math.max(0, CONST.ULTRA_MS - s.elapsedMs)); }
      }
    ];

    function byId(id) {
      for (let i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
      return list[0];
    }
    function isOver(mode, state) {
      return state.topOut === true || mode.isFinished(state) === true;
    }

    return { list: list, byId: byId, isOver: isOver, formatMs: formatMs };
  })();
```

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入 Modes 断言**

```js
// ===== Task 6: Modes =====
eq('三种模式且顺序固定', Modes.list.map(function (m) { return m.id; }), ['marathon', 'sprint', 'ultra']);
eq('byId 命中', Modes.byId('sprint').id, 'sprint');
eq('byId 未命中回退首个', Modes.byId('nope').id, 'marathon');
eq('mm:ss.cc 格式 0', Modes.formatMs(0), '00:00.00');
eq('mm:ss.cc 格式 61234', Modes.formatMs(61234), '01:01.23');
eq('mm:ss.cc 负数归零', Modes.formatMs(-5), '00:00.00');

const marathon = Modes.byId('marathon');
const sprint = Modes.byId('sprint');
const ultra = Modes.byId('ultra');
ok('马拉松永不达成目标', marathon.isFinished({ score: 999999, lines: 999, elapsedMs: 1e9, level: 20, topOut: false }) === false);
ok('40 行未满未结束', sprint.isFinished({ lines: 39, elapsedMs: 1000, score: 0, level: 1, topOut: false }) === false);
ok('40 行满即结束', sprint.isFinished({ lines: 40, elapsedMs: 1000, score: 0, level: 1, topOut: false }) === true);
ok('Ultra 未到点未结束', ultra.isFinished({ elapsedMs: 119999, lines: 0, score: 0, level: 1, topOut: false }) === false);
ok('Ultra 到点即结束', ultra.isFinished({ elapsedMs: 120000, lines: 0, score: 0, level: 1, topOut: false }) === true);
eq('Sprint 结算含用时', sprint.resultText({ lines: 40, elapsedMs: 91234, score: 1200, level: 3, topOut: false }), '用时 01:31.23｜消行 40');
eq('Ultra 结算含总分', ultra.resultText({ lines: 12, elapsedMs: 120000, score: 3400, level: 2, topOut: false }), '总分 3400｜消行 12');
eq('Ultra 进度为倒计时', ultra.progressText({ elapsedMs: 30000, lines: 0, score: 0, level: 1, topOut: false }), '01:30.00');
eq('Ultra 进度不为负', ultra.progressText({ elapsedMs: 130000, lines: 0, score: 0, level: 1, topOut: false }), '00:00.00');
ok('堆顶即结束（马拉松也是）', Modes.isOver(marathon, { topOut: true, lines: 0, elapsedMs: 0, score: 0, level: 1 }) === true);
ok('未堆顶且未达标则不结束', Modes.isOver(sprint, { topOut: false, lines: 3, elapsedMs: 5000, score: 0, level: 1 }) === false);
```

- [ ] **Step 3: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 285）

- [ ] **Step 4: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 三种挑战模式的数据化配置与结算文案"
```

---

## Task 7: GameState（状态机、重力/锁定延迟、Hold/Next、堆顶结束）与内核导出

**Files:**
- Modify: `index.html`（内核区块新增 `GameState` 与最后一行导出）
- Modify: `tools/selftest.mjs`

**Interfaces:**
- Consumes: `CONST`、`Board`、`Tetromino`、`Randomizer`、`Scoring`、`Modes`
- Produces: `GameState.STATUS`、`GameState.create()`、`GameState.start(state, modeId, startLevel, seed?)`、`GameState.update(state, dtMs)`、`GameState.applyIntent(state, intent)`、`GameState.ghostY(state)`、`GameState.isGrounded(state)`、`GameState.spawn(state, forcedType?)`、`GameState.hold(state)`、`GameState.hardDrop(state)`、`GameState.lockPiece(state)`、`GameState.togglePause(state)`、`GameState.toReady(state)`；并写入 `window.Tetris = { Board, Tetromino, Randomizer, Scoring, Modes }`（设计 §6.11 的公开导出，**不含** `GameState`）

- [ ] **Step 1: 写入 GameState**

```js
  /* ========== 游戏状态机与时间推进（不接触 DOM） ========== */
  const GameState = (function () {
    const STATUS = { READY: 'READY', PLAYING: 'PLAYING', PAUSED: 'PAUSED', GAME_OVER: 'GAME_OVER' };

    function create() {
      return {
        status: STATUS.READY,
        mode: Modes.list[0],
        startLevel: CONST.START_LEVEL_MIN,
        board: null,
        piece: null,
        next: [],
        hold: null,
        canHold: true,
        score: 0,
        lines: 0,
        level: CONST.START_LEVEL_MIN,
        elapsedMs: 0,
        topOut: false,
        dropAccMs: 0,
        dropIntervalMs: 0,
        softDropping: false,
        lockTimerMs: 0,
        lockResets: 0,
        grounded: false,
        clearing: null,
        rng: null,
        message: ''
      };
    }

    function start(state, modeId, startLevel, seed) {
      const mode = Modes.byId(modeId);
      const level = Math.max(CONST.START_LEVEL_MIN, Math.min(CONST.START_LEVEL_MAX, startLevel || CONST.START_LEVEL_MIN));
      state.mode = mode;
      state.startLevel = level;
      state.level = level;
      state.board = Board.create();
      state.rng = Randomizer.create(typeof seed === 'number' ? seed : Math.floor(Math.random() * 4294967295));
      state.next = state.rng.peek(CONST.NEXT_COUNT);
      state.hold = null;
      state.canHold = true;
      state.score = 0;
      state.lines = 0;
      state.elapsedMs = 0;
      state.topOut = false;
      state.dropAccMs = 0;
      state.dropIntervalMs = Scoring.gravityMs(level);
      state.softDropping = false;
      state.lockTimerMs = 0;
      state.lockResets = 0;
      state.grounded = false;
      state.clearing = null;
      state.message = '';
      state.piece = null;
      state.status = STATUS.PLAYING;
      spawn(state);
      return state;
    }

    function isGrounded(state) {
      const p = state.piece;
      if (!p) return false;
      return Board.collides(state.board, p.type, p.x, p.y + 1, p.rot);
    }

    function ghostY(state) {
      const p = state.piece;
      if (!p) return 0;
      let y = p.y;
      while (!Board.collides(state.board, p.type, p.x, y + 1, p.rot)) y++;
      return y;
    }

    function topOut(state, message) {
      state.status = STATUS.GAME_OVER;
      state.topOut = true;
      state.softDropping = false;
      state.message = message;
    }

    function spawn(state, forcedType) {
      const type = forcedType || state.rng.next();
      state.next = state.rng.peek(CONST.NEXT_COUNT);
      const piece = { type: type, rot: 0, x: Tetromino.spawnX(type), y: Tetromino.spawnY() };
      state.canHold = true;
      state.dropAccMs = 0;
      state.lockTimerMs = 0;
      state.lockResets = 0;
      state.grounded = false;
      state.piece = piece;
      if (Board.collides(state.board, piece.type, piece.x, piece.y, piece.rot)) {
        topOut(state, '堆顶');
      }
      return state;
    }

    function afterMoveSucceeded(state) {
      if (state.grounded && state.lockResets < CONST.LOCK_RESET_LIMIT) {
        state.lockTimerMs = 0;
        state.lockResets++;
      }
      if (!isGrounded(state)) {
        state.grounded = false;
        state.lockTimerMs = 0;
      }
    }

    function tryMove(state, dx, dy) {
      const p = state.piece;
      if (!p) return false;
      if (Board.collides(state.board, p.type, p.x + dx, p.y + dy, p.rot)) return false;
      p.x += dx;
      p.y += dy;
      afterMoveSucceeded(state);
      return true;
    }

    function tryRotate(state, dir) {
      const p = state.piece;
      if (!p || p.type === 'O') return false;
      const from = p.rot;
      const to = ((from + dir) % 4 + 4) % 4;
      const kicks = Tetromino.kicksFor(p.type, from, to);
      for (let i = 0; i < kicks.length; i++) {
        const nx = p.x + kicks[i][0];
        const ny = p.y + kicks[i][1];
        if (!Board.collides(state.board, p.type, nx, ny, to)) {
          p.x = nx;
          p.y = ny;
          p.rot = to;
          afterMoveSucceeded(state);
          return true;
        }
      }
      return false; // 踢墙全部失败：静默保持原姿态
    }

    function hold(state) {
      if (!state.canHold || !state.piece) return false;
      const current = state.piece.type;
      if (state.hold === null) {
        state.hold = current;
        spawn(state);
      } else {
        const swap = state.hold;
        state.hold = current;
        state.piece = { type: swap, rot: 0, x: Tetromino.spawnX(swap), y: Tetromino.spawnY() };
        state.next = state.rng.peek(CONST.NEXT_COUNT);
        state.dropAccMs = 0;
        state.lockTimerMs = 0;
        state.lockResets = 0;
        state.grounded = false;
        if (Board.collides(state.board, state.piece.type, state.piece.x, state.piece.y, state.piece.rot)) {
          topOut(state, '堆顶');
        }
      }
      state.canHold = false;
      return true;
    }

    function lockPiece(state) {
      const p = state.piece;
      if (!p) return;
      state.board = Board.merge(state.board, p.type, p.x, p.y, p.rot);
      state.piece = null;
      state.grounded = false;
      state.lockTimerMs = 0;
      state.lockResets = 0;
      const rows = Board.fullRows(state.board);
      if (rows.length > 0) {
        state.clearing = { rows: rows, timerMs: 0 }; // 动画期间暂停重力与锁定
        state.softDropping = false;
      } else {
        spawn(state);
      }
    }

    function finishClearing(state) {
      const rows = state.clearing.rows;
      state.board = Board.clearRows(state.board, rows);
      const applied = Scoring.applyClear(
        { score: state.score, lines: state.lines, level: state.level, startLevel: state.startLevel },
        rows.length
      );
      state.score = applied.score;
      state.lines = applied.lines;
      if (applied.level > state.level) {
        state.level = applied.level;
        state.dropIntervalMs = Scoring.gravityMs(state.level);
        state.message = 'LEVEL ' + state.level;
      }
      state.clearing = null;
      spawn(state);
    }

    function hardDrop(state) {
      const p = state.piece;
      if (!p) return 0;
      const target = ghostY(state);
      const cells = target - p.y;
      p.y = target;
      state.score += Scoring.hardDropPoints(cells);
      lockPiece(state);
      return cells;
    }

    function update(state, dtMs) {
      if (state.status !== STATUS.PLAYING) return state;
      const dt = Math.max(0, Math.min(dtMs, CONST.MAX_DT_MS)); // 钳制，避免切后台回来瞬间掉多格
      state.elapsedMs += dt;

      if (state.mode.isFinished(state)) { // Ultra 倒计时归零
        topOut(state, '时间到');
        return state;
      }

      if (state.clearing) {
        state.clearing.timerMs += dt;
        if (state.clearing.timerMs >= CONST.CLEAR_ANIM_MS) finishClearing(state);
        return state;
      }

      const p = state.piece;
      if (!p) return state;

      const interval = state.softDropping
        ? Math.max(state.dropIntervalMs / CONST.SOFT_DROP_FACTOR, CONST.MIN_GRAVITY_MS)
        : state.dropIntervalMs;

      state.dropAccMs += dt;
      while (state.dropAccMs >= interval) {
        state.dropAccMs -= interval;
        if (!Board.collides(state.board, p.type, p.x, p.y + 1, p.rot)) {
          p.y += 1;
          state.grounded = false;
          state.lockTimerMs = 0;
          if (state.softDropping) state.score += Scoring.softDropPoints(1);
        } else {
          state.grounded = true;
          break;
        }
      }

      if (isGrounded(state)) {
        state.grounded = true;
        state.lockTimerMs += dt;
        if (state.lockTimerMs >= CONST.LOCK_DELAY_MS) lockPiece(state);
      } else if (state.grounded) {
        state.grounded = false;
        state.lockTimerMs = 0;
      }
      return state;
    }

    function applyIntent(state, intent) {
      if (!intent) return state;
      if (intent.type === 'pause') {
        togglePause(state);
        return state;
      }
      if (state.status !== STATUS.PLAYING) return state; // 幂等：忽略非法状态下的操作
      switch (intent.type) {
        case 'move': tryMove(state, intent.dx, 0); break;
        case 'rotate': tryRotate(state, intent.dir); break;
        case 'softDrop':
          state.softDropping = intent.on === true;
          state.dropAccMs = 0;
          break;
        case 'hardDrop': hardDrop(state); break;
        case 'hold': hold(state); break;
      }
      return state;
    }

    function togglePause(state) {
      if (state.status === STATUS.PLAYING) {
        state.status = STATUS.PAUSED;
        state.softDropping = false;
      } else if (state.status === STATUS.PAUSED) {
        state.status = STATUS.PLAYING;
      }
      return state;
    }

    function toReady(state) {
      state.status = STATUS.READY;
      state.softDropping = false;
      return state;
    }

    return {
      STATUS: STATUS,
      create: create,
      start: start,
      spawn: spawn,
      update: update,
      applyIntent: applyIntent,
      tryMove: tryMove,
      tryRotate: tryRotate,
      hold: hold,
      hardDrop: hardDrop,
      lockPiece: lockPiece,
      ghostY: ghostY,
      isGrounded: isGrounded,
      togglePause: togglePause,
      toReady: toReady
    };
  })();
```

- [ ] **Step 2: 写入内核公开导出（设计 §6.11）**

在内核区块的**最后一行**追加：

```js
  /* ========== 对外导出（自测/调试用） ========== */
  window.Tetris = {
    Board: Board,
    Tetromino: Tetromino,
    Randomizer: Randomizer,
    Scoring: Scoring,
    Modes: Modes
  };
```

- [ ] **Step 3: 在 `// @@ASSERTIONS@@` 处插入 GameState 断言**

```js
// ===== Task 7: GameState =====
eq('公开导出恰好 5 个模块', Object.keys(sandbox.window.Tetris).sort(), ['Board', 'Modes', 'Randomizer', 'Scoring', 'Tetromino']);
ok('导出对象可调用', sandbox.window.Tetris.Board === Board && sandbox.window.Tetris.Modes === Modes);

const gs = GameState.start(GameState.create(), 'marathon', 5, 42);
eq('开局状态为 PLAYING', gs.status, 'PLAYING');
eq('起始等级生效', gs.level, 5);
eq('重力按起始等级', gs.dropIntervalMs, Scoring.gravityMs(5));
eq('Next 队列 3 个', gs.next.length, 3);
ok('当前方块已生成', gs.piece !== null && Tetromino.TYPES.indexOf(gs.piece.type) >= 0);
eq('开局分数为 0', gs.score, 0);

const gsSeedA = GameState.start(GameState.create(), 'marathon', 1, 1234);
const gsSeedB = GameState.start(GameState.create(), 'marathon', 1, 1234);
eq('同种子首块一致', gsSeedA.piece.type, gsSeedB.piece.type);
eq('同种子队列一致', gsSeedA.next, gsSeedB.next);

// 左右移动与边界
const gsMove = GameState.start(GameState.create(), 'marathon', 1, 7);
const x0 = gsMove.piece.x;
GameState.applyIntent(gsMove, { type: 'move', dx: -1 });
eq('左移一格', gsMove.piece.x, x0 - 1);
for (let i = 0; i < 20; i++) GameState.applyIntent(gsMove, { type: 'move', dx: -1 });
ok('不越左墙', gsMove.piece.x >= 0);
for (let i = 0; i < 20; i++) GameState.applyIntent(gsMove, { type: 'move', dx: 1 });
ok('不越右墙', gsMove.piece.x + 3 <= 10);

// 旋转（含回绕）
const gsRot = GameState.start(GameState.create(), 'marathon', 1, 8);
GameState.spawn(gsRot, 'T');
GameState.applyIntent(gsRot, { type: 'rotate', dir: 1 });
eq('顺时针旋转到 1', gsRot.piece.rot, 1);
GameState.applyIntent(gsRot, { type: 'rotate', dir: -1 });
eq('逆时针回到 0', gsRot.piece.rot, 0);

// SRS 踢墙：I 块立起贴左墙后可借 (2,0) 踢墙转平（I 竖条占包围盒第 2 列 -> x=-2 即贴左墙）
const gsKick = GameState.start(GameState.create(), 'marathon', 1, 9);
GameState.spawn(gsKick, 'I');
gsKick.piece.rot = 1;
gsKick.piece.x = -2;
ok('竖 I 贴左墙不碰撞', Board.collides(gsKick.board, 'I', -2, gsKick.piece.y, 1) === false);
ok('直接转平会碰撞', Board.collides(gsKick.board, 'I', -2, gsKick.piece.y, 0) === true);
ok('踢墙转平成功', GameState.tryRotate(gsKick, -1) === true);
eq('踢墙后旋转态为 0', gsKick.piece.rot, 0);
eq('踢墙后借用 +2 位移', gsKick.piece.x, 0);

// 踢墙全失败时保持原姿态：只留 1 格宽竖井，任何旋转都不可能成功
const gsStuck = GameState.start(GameState.create(), 'marathon', 1, 10);
GameState.spawn(gsStuck, 'I');
for (let r = 0; r < Board.TOTAL_ROWS; r++) {
  for (let c = 0; c < CONST.COLS; c++) gsStuck.board[r][c] = (c === 4) ? null : 'Z';
}
gsStuck.piece.rot = 1;
gsStuck.piece.x = 2; // 竖 I（包围盒第 2 列）正好落在第 4 列
gsStuck.piece.y = 10;
ok('竖井中当前姿态不碰撞', Board.collides(gsStuck.board, 'I', 2, 10, 1) === false);
eq('竖井中旋转失败（踢墙候选全部碰撞）', GameState.tryRotate(gsStuck, 1), false);
eq('失败后旋转态不变', gsStuck.piece.rot, 1);
eq('失败后位置不变', [gsStuck.piece.x, gsStuck.piece.y], [2, 10]);

// 软降：加速下落 + 加分
const gsSoft = GameState.start(GameState.create(), 'marathon', 1, 11);
GameState.applyIntent(gsSoft, { type: 'softDrop', on: true });
GameState.update(gsSoft, 250);
ok('开启软降后 250ms 内即下落', gsSoft.piece.y > 0);
ok('软降加分（1 分/格）', gsSoft.score > 0);
GameState.applyIntent(gsSoft, { type: 'softDrop', on: false });
ok('关闭软降后状态还原', gsSoft.softDropping === false);

// 硬降：落到幽灵位置、加分、立即锁定并生成新块
const gsHard = GameState.start(GameState.create(), 'marathon', 1, 12);
const ghost = GameState.ghostY(gsHard);
const hardCells = GameState.hardDrop(gsHard);
eq('硬降格数 = 幽灵位移', hardCells, ghost - Tetromino.spawnY());
eq('硬降加分 2/格', gsHard.score, hardCells * 2);
ok('硬降后已合入棋盘', gsHard.board[21].some(function (v) { return v !== null; }) || gsHard.board[20].some(function (v) { return v !== null; }));
ok('硬降后立即生成新块', gsHard.piece !== null);

// 锁定延迟：贴地后 300ms 未锁定、再 300ms 锁定
const gsLock = GameState.start(GameState.create(), 'marathon', 1, 13);
GameState.spawn(gsLock, 'O');
gsLock.piece.y = 20; // 直接贴底
GameState.update(gsLock, 250);
ok('锁定延迟内仍可操作', gsLock.piece !== null && gsLock.status === 'PLAYING');
GameState.update(gsLock, 250);
GameState.update(gsLock, 250);
ok('超过锁定延迟后棋盘出现固定方块', gsLock.board[21].filter(function (v) { return v !== null; }).length >= 2);

// 消行动画与计分：底行只差 3..6 列，横躺的 I 硬降后正好补满
const gsClear = GameState.start(GameState.create(), 'marathon', 1, 14);
[0, 1, 2, 7, 8, 9].forEach(function (c) { gsClear.board[21][c] = 'J'; });
GameState.spawn(gsClear, 'I'); // 出生姿态横躺：包围盒第 1 行、第 0..3 列 -> 棋盘第 3..6 列
GameState.hardDrop(gsClear);
ok('触发消行动画', gsClear.clearing !== null && gsClear.clearing.rows.indexOf(21) >= 0);
GameState.update(gsClear, 250);
ok('动画结束后清行', gsClear.clearing === null && gsClear.lines === 1);
ok('单行消除加分 100 x 等级', gsClear.score >= 100);

// Hold：首次存入、二次取回、同一方块落地前不可重复
const gsHold = GameState.start(GameState.create(), 'marathon', 1, 15);
const firstType = gsHold.piece.type;
ok('首次 Hold 成功', GameState.hold(gsHold) === true);
eq('Hold 槽记录首块', gsHold.hold, firstType);
ok('同一方块落地前不可重复 Hold', GameState.hold(gsHold) === false);
GameState.spawn(gsHold); // 模拟落地后新块
const typeSecond = gsHold.piece.type;
ok('落地后可再次 Hold', GameState.hold(gsHold) === true);
eq('取回 Hold 中的方块', gsHold.piece.type, firstType);
eq('Hold 槽换为原方块', gsHold.hold, typeSecond);

// 暂停：冻结推进与计时
const gsPause = GameState.start(GameState.create(), 'marathon', 1, 16);
GameState.applyIntent(gsPause, { type: 'pause' });
eq('暂停状态', gsPause.status, 'PAUSED');
const yBefore = gsPause.piece.y;
const elapsedBefore = gsPause.elapsedMs;
GameState.update(gsPause, 250);
eq('暂停期间方块冻结', gsPause.piece.y, yBefore);
eq('暂停期间计时不累加', gsPause.elapsedMs, elapsedBefore);
GameState.applyIntent(gsPause, { type: 'pause' });
eq('恢复为 PLAYING', gsPause.status, 'PLAYING');

// 堆顶结束
const gsTop = GameState.start(GameState.create(), 'marathon', 1, 17);
for (let r = 0; r < 4; r++) for (let c = 0; c < 10; c++) gsTop.board[r][c] = 'S';
GameState.spawn(gsTop, 'O');
eq('出生点碰撞即结束', gsTop.status, 'GAME_OVER');
eq('标记堆顶', gsTop.topOut, true);
ok('结束信息非空', gsTop.message.length > 0);
ok('结束态可回到 READY', GameState.toReady(gsTop).status === 'READY');

// 40 行竞速：消满 40 行即结束
const gsSprint = GameState.start(GameState.create(), 'sprint', 1, 18);
gsSprint.lines = 40;
GameState.update(gsSprint, 16);
eq('Sprint 达标即结算', gsSprint.status, 'GAME_OVER');

// 限时冲刺：到点即结束
const gsUltra = GameState.start(GameState.create(), 'ultra', 1, 19);
gsUltra.elapsedMs = CONST.ULTRA_MS - 100;
GameState.update(gsUltra, 250);
eq('Ultra 到点即结算', gsUltra.status, 'GAME_OVER');
ok('Ultra 结算含总分', Modes.byId('ultra').resultText(gsUltra).indexOf('总分') >= 0);

// dt 钳制：一次超大 dt 不会瞬间掉多格
const gsDt = GameState.start(GameState.create(), 'marathon', 1, 20);
GameState.update(gsDt, 100000);
ok('超大 dt 被钳制（未直接落地锁定）', gsDt.piece !== null && gsDt.piece.y <= 1);
```

- [ ] **Step 4: 运行自测并确认全部通过**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 340）
若出现 `FAIL`：先看断言名——踢墙相关失败最常见原因是踢墙表 y 方向写反（本实现为屏幕坐标，dy 与 SRS 官方表相反）。

- [ ] **Step 5: 语法自检（不执行，仅 parse）**

Run: `node --check <(sed -n '/<script id="tetris-core">/,/<\/script>/p' index.html | sed '1d;$d')`
Expected: 无输出（退出码 0）
说明：若环境不支持进程替换，用 `sed -n '/<script id="tetris-core">/,/<\/script>/p' index.html | sed '1d;$d' > /tmp/core.js && node --check /tmp/core.js && rm /tmp/core.js`。

- [ ] **Step 6: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(core): 游戏状态机、重力/锁定延迟、Hold/Next 与模式结算"
```

---

## Task 8: Input（DAS/ARR 输入归一化）与 Renderer（Canvas 绘制）

**Files:**
- Modify: `index.html`（`<script id="tetris-shell">` 区块内新增 `Input`、`Renderer`）
- Modify: `tools/selftest.mjs`（新增外壳静态断言）

**Interfaces:**
- Consumes: `CONST`、`Modes`、`Tetromino`、`Board`、`GameState`、DOM（`canvas` / 文本节点）
- Produces: `Input.actionFor(code)`、`Input.handleDown(code, isRepeat) -> intent|null`、`Input.handleUp(code) -> intent|null`、`Input.poll(dtMs) -> intent[]`、`Input.reset()`；`Renderer.attach({board, next, hold})`、`Renderer.bindStats(els)`、`Renderer.setup(canvas, cssW, cssH) -> ctx`、`Renderer.draw(state)`、`Renderer.drawMini(ctx, type, size, boxW, boxH)`

- [ ] **Step 1: 写入 Input（键盘 → intent 归一化 + DAS/ARR）**

把下面内容写入 `<script id="tetris-shell">` 区块内：

```js
  /* ==========================================================================
   * 外壳 · Input：键盘事件 -> intent（归一化），左右移动支持 DAS/ARR 自动重复
   * ========================================================================== */
  const Input = (function () {
    let leftHeld = false, rightHeld = false;
    let leftTimer = 0, rightTimer = 0;
    let leftCharged = false, rightCharged = false;

    function actionFor(code) {
      const K = CONST.KEYS;
      if (K.MOVE_LEFT.indexOf(code) >= 0) return 'left';
      if (K.MOVE_RIGHT.indexOf(code) >= 0) return 'right';
      if (K.ROTATE_CW.indexOf(code) >= 0) return 'rotateCW';
      if (K.ROTATE_CCW.indexOf(code) >= 0) return 'rotateCCW';
      if (K.SOFT_DROP.indexOf(code) >= 0) return 'softDrop';
      if (K.HARD_DROP.indexOf(code) >= 0) return 'hardDrop';
      if (K.HOLD.indexOf(code) >= 0) return 'hold';
      return null;
    }

    function intentFor(action) {
      if (action === 'left') return { type: 'move', dx: -1 };
      if (action === 'right') return { type: 'move', dx: 1 };
      if (action === 'rotateCW') return { type: 'rotate', dir: 1 };
      if (action === 'rotateCCW') return { type: 'rotate', dir: -1 };
      if (action === 'hardDrop') return { type: 'hardDrop' };
      if (action === 'hold') return { type: 'hold' };
      if (action === 'softDropOn') return { type: 'softDrop', on: true };
      if (action === 'softDropOff') return { type: 'softDrop', on: false };
      return null;
    }

    function handleDown(code, isRepeat) {
      const action = actionFor(code);
      if (!action) return null;

      if (action === 'left' || action === 'right') {
        if (isRepeat) return null; // 系统自动重复忽略：重复由 poll 按 DAS/ARR 驱动
        if (action === 'left') { leftHeld = true; leftTimer = 0; leftCharged = false; }
        else { rightHeld = true; rightTimer = 0; rightCharged = false; }
        return intentFor(action);
      }
      if (isRepeat) return null; // 旋转/硬降/暂存只响应按下沿
      if (action === 'softDrop') return intentFor('softDropOn');
      return intentFor(action);
    }

    function handleUp(code) {
      const action = actionFor(code);
      if (action === 'left') { leftHeld = false; leftCharged = false; leftTimer = 0; }
      else if (action === 'right') { rightHeld = false; rightCharged = false; rightTimer = 0; }
      else if (action === 'softDrop') return intentFor('softDropOff');
      return null;
    }

    function poll(dtMs) {
      const out = [];
      if (leftHeld) {
        leftTimer += dtMs;
        if (!leftCharged) {
          if (leftTimer >= CONST.DAS_MS) { leftCharged = true; leftTimer = 0; out.push(intentFor('left')); }
        } else {
          while (leftTimer >= CONST.ARR_MS) { leftTimer -= CONST.ARR_MS; out.push(intentFor('left')); }
        }
      }
      if (rightHeld) {
        rightTimer += dtMs;
        if (!rightCharged) {
          if (rightTimer >= CONST.DAS_MS) { rightCharged = true; rightTimer = 0; out.push(intentFor('right')); }
        } else {
          while (rightTimer >= CONST.ARR_MS) { rightTimer -= CONST.ARR_MS; out.push(intentFor('right')); }
        }
      }
      return out;
    }

    function reset() {
      leftHeld = false; rightHeld = false;
      leftTimer = 0; rightTimer = 0;
      leftCharged = false; rightCharged = false;
    }

    return { actionFor: actionFor, handleDown: handleDown, handleUp: handleUp, poll: poll, reset: reset };
  })();
```

- [ ] **Step 2: 写入 Renderer（棋盘 / 幽灵块 / Hold / Next / 侧栏数字）**

```js
  /* ==========================================================================
   * 外壳 · Renderer：把 state 渲染为像素（不修改 state）
   * ========================================================================== */
  const Renderer = (function () {
    const CELL = CONST.CELL;
    let ctxBoard = null, ctxNext = null, ctxHold = null;
    let statsEls = null;

    // 逻辑尺寸固定，按 devicePixelRatio 放大 backing store，保证高清屏不糊
    function setup(canvas, cssW, cssH) {
      const dpr = window.devicePixelRatio || 1;
      canvas.width = Math.round(cssW * dpr);
      canvas.height = Math.round(cssH * dpr);
      canvas.style.width = cssW + 'px';
      canvas.style.height = cssH + 'px';
      const ctx = canvas.getContext('2d');
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      return ctx;
    }

    function attach(canvases) {
      ctxBoard = setup(canvases.board, CONST.COLS * CELL, CONST.ROWS * CELL);
      ctxNext = setup(canvases.next, 96, CONST.NEXT_COUNT * 64);
      ctxHold = setup(canvases.hold, 96, 64);
    }

    function bindStats(els) { statsEls = els; }

    function roundRect(ctx, px, py, size, radius) {
      const r = Math.max(1, Math.min(radius, size / 2));
      ctx.beginPath();
      ctx.moveTo(px + r, py);
      ctx.lineTo(px + size - r, py);
      ctx.quadraticCurveTo(px + size, py, px + size, py + r);
      ctx.lineTo(px + size, py + size - r);
      ctx.quadraticCurveTo(px + size, py + size, px + size - r, py + size);
      ctx.lineTo(px + r, py + size);
      ctx.quadraticCurveTo(px, py + size, px, py + size - r);
      ctx.lineTo(px, py + r);
      ctx.quadraticCurveTo(px, py, px + r, py);
      ctx.closePath();
    }

    function drawCell(ctx, px, py, size, color, alpha) {
      const a = alpha === undefined ? 1 : alpha;
      ctx.globalAlpha = a;
      ctx.fillStyle = color;
      roundRect(ctx, px, py, size, Math.max(2, size * 0.18));
      ctx.fill();
      ctx.globalAlpha = a * 0.35;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(px + size * 0.18, py + size * 0.13, size * 0.64, size * 0.14);
      ctx.globalAlpha = 1;
    }

    function drawGhostCell(ctx, px, py, size) {
      ctx.globalAlpha = 1;
      ctx.strokeStyle = CONST.GHOST_STROKE;
      ctx.lineWidth = 2;
      roundRect(ctx, px + 2, py + 2, size - 4, Math.max(2, (size - 4) * 0.18));
      ctx.stroke();
    }

    // 方块在自身包围盒内的最小/最大行列，用于在槽位中居中
    function boxOf(type, rot) {
      const list = Tetromino.cells(type, rot);
      let minX = 99, maxX = -99, minY = 99, maxY = -99;
      list.forEach(function (c) {
        if (c.x < minX) minX = c.x;
        if (c.x > maxX) maxX = c.x;
        if (c.y < minY) minY = c.y;
        if (c.y > maxY) maxY = c.y;
      });
      return { minX: minX, minY: minY, w: maxX - minX + 1, h: maxY - minY + 1 };
    }

    function drawMini(ctx, type, size, boxW, boxH) {
      ctx.clearRect(0, 0, boxW, boxH);
      if (!type) return;
      const box = boxOf(type, 0);
      const ox = (boxW - box.w * size) / 2 - box.minX * size;
      const oy = (boxH - box.h * size) / 2 - box.minY * size;
      Tetromino.cells(type, 0).forEach(function (c) {
        drawCell(ctx, ox + c.x * size, oy + c.y * size, size, CONST.COLORS[type]);
      });
    }

    function drawNext(state) {
      const size = 20;
      const slotH = 64;
      ctxNext.clearRect(0, 0, 96, CONST.NEXT_COUNT * slotH);
      state.next.forEach(function (type, i) {
        const box = boxOf(type, 0);
        const ox = (96 - box.w * size) / 2 - box.minX * size;
        const oy = i * slotH + (slotH - box.h * size) / 2 - box.minY * size;
        Tetromino.cells(type, 0).forEach(function (c) {
          drawCell(ctxNext, ox + c.x * size, oy + c.y * size, size, CONST.COLORS[type]);
        });
      });
    }

    function drawHold(state) {
      drawMini(ctxHold, state.hold, 22, 96, 64);
    }

    function drawBoard(state) {
      const W = CONST.COLS * CELL;
      const H = CONST.ROWS * CELL;
      ctxBoard.clearRect(0, 0, W, H);
      ctxBoard.fillStyle = '#0b1020';
      ctxBoard.fillRect(0, 0, W, H);

      ctxBoard.strokeStyle = 'rgba(148, 163, 184, 0.12)';
      ctxBoard.lineWidth = 1;
      for (let c = 1; c < CONST.COLS; c++) {
        ctxBoard.beginPath();
        ctxBoard.moveTo(c * CELL, 0);
        ctxBoard.lineTo(c * CELL, H);
        ctxBoard.stroke();
      }
      for (let r = 1; r < CONST.ROWS; r++) {
        ctxBoard.beginPath();
        ctxBoard.moveTo(0, r * CELL);
        ctxBoard.lineTo(W, r * CELL);
        ctxBoard.stroke();
      }

      // 已固定方块（隐藏缓冲行不绘制；消行动画期间整行闪白）
      const flash = state.clearing ? Math.min(1, state.clearing.timerMs / CONST.CLEAR_ANIM_MS) : 0;
      for (let r = Board.HIDDEN_ROWS; r < Board.TOTAL_ROWS; r++) {
        const dr = r - Board.HIDDEN_ROWS;
        for (let c = 0; c < CONST.COLS; c++) {
          const t = state.board[r][c];
          if (!t) continue;
          const isFlashing = flash > 0 && state.clearing.rows.indexOf(r) >= 0;
          drawCell(ctxBoard, c * CELL + 1, dr * CELL + 1, CELL - 2, isFlashing ? '#f8fafc' : CONST.COLORS[t]);
        }
      }

      const p = state.piece;
      if (!p) return;

      // 幽灵块：当前姿态的硬降落点
      const gy = GameState.ghostY(state);
      if (gy !== p.y) {
        Tetromino.cells(p.type, p.rot).forEach(function (c) {
          const cy = gy + c.y - Board.HIDDEN_ROWS;
          if (cy < 0) return;
          drawGhostCell(ctxBoard, (p.x + c.x) * CELL + 1, cy * CELL + 1, CELL - 2);
        });
      }
      // 当前方块
      Tetromino.cells(p.type, p.rot).forEach(function (c) {
        const cy = p.y + c.y - Board.HIDDEN_ROWS;
        if (cy < 0) return; // 仍在隐藏缓冲区，不绘制
        drawCell(ctxBoard, (p.x + c.x) * CELL + 1, cy * CELL + 1, CELL - 2, CONST.COLORS[p.type]);
      });
    }

    function drawStats(state) {
      if (!statsEls) return;
      statsEls.score.textContent = String(state.score);
      statsEls.level.textContent = String(state.level);
      statsEls.lines.textContent = String(state.lines);
      statsEls.progressLabel.textContent = state.mode.progressLabel;
      statsEls.progress.textContent = state.mode.progressText(state);
    }

    function draw(state) {
      drawBoard(state);
      drawNext(state);
      drawHold(state);
      drawStats(state);
    }

    return { attach: attach, bindStats: bindStats, setup: setup, draw: draw, drawMini: drawMini };
  })();
```

- [ ] **Step 3: 在 `// @@ASSERTIONS@@` 处插入外壳静态断言**

```js
// ===== Task 8: 外壳结构（静态断言；外壳依赖 DOM，无法在 Node 中执行）=====
ok('已定义渲染入口 drawBoard', /function drawBoard\(state\)/.test(html));
ok('已定义幽灵块绘制', /drawGhostCell/.test(html));
ok('已定义 Next/Hold 绘制', /function drawNext\(state\)/.test(html) && /function drawHold\(state\)/.test(html));
ok('侧栏数字与模式进度已接线', /progressLabel\.textContent/.test(html) && /progressText\(state\)/.test(html));
ok('输入归一化为 intent', /function intentFor\(action\)/.test(html) && /type: 'hardDrop'/.test(html));
ok('DAS/ARR 由 poll 驱动', /function poll\(dtMs\)/.test(html) && /CONST\.DAS_MS/.test(html) && /CONST\.ARR_MS/.test(html));
ok('旋转/硬降忽略系统自动重复', /if \(isRepeat\) return null; \/\/ 旋转\/硬降\/暂存只响应按下沿/.test(html));
```

- [ ] **Step 4: 语法自检**

Run: `sed -n '/<script id="tetris-shell">/,/<\/script>/p' index.html | sed '1d;$d' > /tmp/shell.js && node --check /tmp/shell.js && rm /tmp/shell.js`
Expected: 无输出（退出码 0）

- [ ] **Step 5: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0** 即通过；本任务结束后 `passed` ≥ 345）

- [ ] **Step 6: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(shell): 输入归一化（DAS/ARR）与 Canvas 渲染层"
```

---

## Task 9: Bootstrap（主循环、事件绑定、模式选择面板、切后台自动暂停）

**Files:**
- Modify: `index.html`（`<script id="tetris-shell">` 区块内追加 Bootstrap IIFE）
- Modify: `tools/selftest.mjs`（新增接线与合规性静态断言）

**Interfaces:**
- Consumes: `CONST`、`Modes`、`GameState`、`Input`、`Renderer`、全部 DOM 元素
- Produces: 自执行引导逻辑：`requestAnimationFrame(loop)` 主循环、`keydown/keyup` 绑定、`blur`/`visibilitychange` 自动暂停、`resize` 重算画布、READY 面板的键鼠双通道交互、`ui = { modeIndex, startLevel, bestScore, lastStatus }` 会话内最高分

- [ ] **Step 1: 追加 Bootstrap**

```js
  /* ==========================================================================
   * 外壳 · Bootstrap：主循环、事件绑定、面板交互
   * ========================================================================== */
  (function bootstrap() {
    const boardCanvas = document.getElementById('board');
    const nextCanvas = document.getElementById('next');
    const holdCanvas = document.getElementById('hold');
    const overlay = document.getElementById('overlay');
    const el = {
      title: document.getElementById('overlay-title'),
      sub: document.getElementById('overlay-sub'),
      modes: document.getElementById('mode-list'),
      levels: document.getElementById('level-list'),
      score: document.getElementById('score'),
      level: document.getElementById('level'),
      lines: document.getElementById('lines'),
      progressLabel: document.getElementById('progress-label'),
      progress: document.getElementById('progress')
    };

    // 所有游戏内按键：统一 preventDefault，避免方向键/空格滚动页面
    const ALL_CODES = Object.keys(CONST.KEYS).reduce(function (acc, key) {
      return acc.concat(CONST.KEYS[key]);
    }, []);

    const state = GameState.create();
    const ui = {
      modeIndex: 0,
      startLevel: CONST.START_LEVEL_MIN,
      bestScore: 0, // 仅当前会话内存：不做持久化，刷新即清零
      lastStatus: GameState.STATUS.READY
    };
    let lastTs = 0;

    function layout() {
      Renderer.attach({ board: boardCanvas, next: nextCanvas, hold: holdCanvas });
    }

    function buildChoices() {
      Modes.list.forEach(function (mode, i) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'choice';
        btn.textContent = mode.label + '（' + mode.goal + '）';
        btn.addEventListener('click', function () {
          ui.modeIndex = i;
          syncOverlay();
          btn.blur();
        });
        el.modes.appendChild(btn);
      });
      for (let lv = CONST.START_LEVEL_MIN; lv <= CONST.START_LEVEL_MAX; lv++) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'choice';
        btn.dataset.level = String(lv);
        btn.textContent = 'Lv ' + lv;
        btn.addEventListener('click', function () {
          ui.startLevel = lv;
          syncOverlay();
          btn.blur();
        });
        el.levels.appendChild(btn);
      }
    }

    function syncOverlay() {
      const status = state.status;
      const ready = status === GameState.STATUS.READY;

      overlay.classList.toggle('hidden', status === GameState.STATUS.PLAYING);
      el.modes.classList.toggle('hidden', !ready);
      el.levels.classList.toggle('hidden', !ready);

      Array.prototype.forEach.call(el.modes.children, function (btn, i) {
        btn.classList.toggle('active', i === ui.modeIndex);
      });
      Array.prototype.forEach.call(el.levels.children, function (btn) {
        btn.classList.toggle('active', Number(btn.dataset.level) === ui.startLevel);
      });

      if (ready) {
        el.title.textContent = '俄罗斯方块';
        el.sub.textContent = '选择模式与起始等级，按 Enter 开始';
      } else if (status === GameState.STATUS.PAUSED) {
        el.title.textContent = '已暂停';
        el.sub.textContent = '按 P 继续';
      } else if (status === GameState.STATUS.GAME_OVER) {
        el.title.textContent = state.topOut ? '本局结束 · ' + state.message : '目标达成';
        el.sub.textContent = state.mode.resultText(state) + '\n本会话最佳：' + ui.bestScore + '（按 Enter 返回选择）';
      }
    }

    function startGame() {
      GameState.start(state, Modes.list[ui.modeIndex].id, ui.startLevel);
      syncOverlay();
    }

    function onEnter() {
      if (state.status === GameState.STATUS.READY) startGame();
      else if (state.status === GameState.STATUS.GAME_OVER) {
        GameState.toReady(state);
        syncOverlay();
      }
    }

    // READY 面板：← → 选模式，↑ ↓ 选起始等级
    function onReadyNavigation(code) {
      if (code === CONST.KEYS.MOVE_LEFT[0]) {
        ui.modeIndex = (ui.modeIndex + Modes.list.length - 1) % Modes.list.length;
      } else if (code === CONST.KEYS.MOVE_RIGHT[0]) {
        ui.modeIndex = (ui.modeIndex + 1) % Modes.list.length;
      } else if (code === CONST.KEYS.ROTATE_CW[0]) {
        ui.startLevel = Math.min(CONST.START_LEVEL_MAX, ui.startLevel + 1);
      } else if (code === CONST.KEYS.SOFT_DROP[0]) {
        ui.startLevel = Math.max(CONST.START_LEVEL_MIN, ui.startLevel - 1);
      } else {
        return false;
      }
      syncOverlay();
      return true;
    }

    function onKeyDown(e) {
      const code = e.code;
      if (ALL_CODES.indexOf(code) >= 0) e.preventDefault();
      const isRepeat = e.repeat === true;

      if (code === CONST.KEYS.START[0]) { // Enter：开始 / 重开
        if (!isRepeat) onEnter();
        return;
      }
      if (state.status === GameState.STATUS.READY) {
        if (!isRepeat) onReadyNavigation(code);
        return;
      }
      if (code === CONST.KEYS.PAUSE[0] || code === CONST.KEYS.PAUSE[1]) {
        if (isRepeat) return;
        if (state.status === GameState.STATUS.GAME_OVER) {
          GameState.toReady(state);
          syncOverlay();
          return;
        }
        GameState.applyIntent(state, { type: 'pause' });
        syncOverlay();
        return;
      }
      const intent = Input.handleDown(code, isRepeat);
      if (intent) GameState.applyIntent(state, intent);
    }

    function onKeyUp(e) {
      const intent = Input.handleUp(e.code);
      if (intent) GameState.applyIntent(state, intent);
    }

    // 失焦 / 切后台：清空按键并自动暂停，避免“卡住一直往下掉”
    function autoPause() {
      Input.reset();
      if (state.status === GameState.STATUS.PLAYING) {
        GameState.applyIntent(state, { type: 'pause' });
        syncOverlay();
      }
      lastTs = 0; // 恢复后重新起算，配合 dt 钳制杜绝瞬间掉落
    }

    function loop(ts) {
      const dt = lastTs === 0 ? 0 : ts - lastTs;
      lastTs = ts;

      const intents = Input.poll(dt);
      for (let i = 0; i < intents.length; i++) GameState.applyIntent(state, intents[i]);
      GameState.update(state, dt);
      Renderer.draw(state);

      if (state.status !== ui.lastStatus) {
        if (state.status === GameState.STATUS.GAME_OVER && state.score > ui.bestScore) {
          ui.bestScore = state.score;
        }
        syncOverlay();
        ui.lastStatus = state.status;
      }
      requestAnimationFrame(loop);
    }

    buildChoices();
    layout();
    Renderer.bindStats(el);
    syncOverlay();

    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    window.addEventListener('blur', autoPause);
    document.addEventListener('visibilitychange', function () {
      if (document.hidden) autoPause();
    });
    window.addEventListener('resize', function () {
      layout(); // 重算画布 backing store，避免模糊/错位
    });

    requestAnimationFrame(loop);
  })();
```

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入合规性与接线静态断言**

```js
// ===== Task 9: 接线与验收标准 1/2/19/20 的静态合规断言 =====
ok('无 type="module"（file:// 可用）', !/type\s*=\s*"module"/.test(html));
ok('无外部脚本引用', !/<script[^>]+src=/i.test(html));
ok('无外链样式/媒体资源', !/<link[^>]+href=/i.test(html) && !/<(img|audio|video)[^>]+src=/i.test(html));
ok('无 http/https 远程地址', !/https?:\/\//i.test(html));
ok('无触控事件监听', !/touch(start|move|end|cancel)|pointerdown|pointerup|ontouch/i.test(html));
ok('无本地存储 / Cookie 依赖', !/localStorage|sessionStorage|document\.cookie/.test(html));
ok('主循环使用 requestAnimationFrame', /requestAnimationFrame\(loop\)/.test(html));
ok('游戏按键统一 preventDefault', /if \(ALL_CODES\.indexOf\(code\) >= 0\) e\.preventDefault\(\);/.test(html));
ok('失焦自动暂停已接线', /addEventListener\('blur', autoPause\)/.test(html));
ok('切后台自动暂停已接线', /addEventListener\('visibilitychange'/.test(html) && /document\.hidden/.test(html));
ok('resize 重算画布', /addEventListener\('resize'/.test(html) && /function layout\(\)/.test(html));
ok('READY 面板可键鼠双通道操作', /onReadyNavigation/.test(html) && /addEventListener\('click'/.test(html));
ok('最高分只在内存中（无持久化写入）', /ui\.bestScore/.test(html) && !/bestScore.*localStorage/.test(html));
ok('DPR 适配', /devicePixelRatio/.test(html));
```

- [ ] **Step 3: 语法自检**

Run: `sed -n '/<script id="tetris-shell">/,/<\/script>/p' index.html | sed '1d;$d' > /tmp/shell.js && node --check /tmp/shell.js && rm /tmp/shell.js`
Expected: 无输出（退出码 0）

- [ ] **Step 4: 运行自测**

Run: `node tools/selftest.mjs`
Expected: 末行为 `selftest: <N> passed, 0 failed`（**failed 为 0**；本任务结束后 `passed` ≥ 360）

- [ ] **Step 5: Commit**

```bash
git add index.html tools/selftest.mjs
git commit -m "feat(shell): 主循环、事件绑定、模式面板与自动暂停"
```

---

## Task 10: README.md 与端到端验收

**Files:**
- Create: `README.md`
- Modify: `tools/selftest.mjs`（仅新增 README 存在性断言，见 Step 3）

**Interfaces:**
- Consumes: 已完成的 `index.html`
- Produces: `README.md`（玩法 / 键位 / 模式 / 参数说明；**不被页面引用**）

- [ ] **Step 1: 创建 README.md**

````markdown
# 俄罗斯方块（单文件 HTML）

一个自包含的俄罗斯方块小游戏：**双击 `index.html` 即可开始**。无需联网、无需本地服务器、无需安装任何依赖。

## 如何打开

1. 双击（或右键“用浏览器打开”）`index.html`。
2. 在开始面板用键盘或鼠标选择模式与起始等级，按 `Enter` 开始。
3. 已用 Chrome / Edge / Firefox / Safari 近两个大版本验证（桌面端）。

> 本说明文档不参与运行：删除它（甚至删除 `tools/` 目录）游戏照常可玩。

## 键位

| 操作 | 按键 |
| --- | --- |
| 左移 / 右移 | `←` / `→`（长按支持 DAS 自动重复） |
| 顺时针旋转 | `↑` 或 `X` |
| 逆时针旋转 | `Z` 或 `Ctrl` |
| 软降 | `↓`（按住持续加速，每格 +1 分） |
| 硬降 | `Space`（直接落底并锁定，每格 +2 分） |
| 暂存（Hold） | `C` 或 `Shift`（同一方块落地前只能换一次） |
| 暂停 / 继续 | `P` 或 `Esc` |
| 开始 / 重开 | `Enter` |
| 选择模式 | 开始面板 `←` / `→`（或鼠标点选） |
| 选择起始等级 | 开始面板 `↑` / `↓`（或鼠标点选） |

## 模式

| 模式 | 目标 | 结束条件 | 结算展示 |
| --- | --- | --- | --- |
| 马拉松 | 无尽冲分 | 方块堆顶 | 总分 / 等级 / 消行数 |
| 40 行竞速 | 最快消满 40 行 | 消满 40 行，或堆顶 | 用时（`mm:ss.cc`）/ 消行数 |
| 限时冲刺 | 120 秒内冲最高分 | 倒计时归零，或堆顶 | 总分 / 消行数 |

- 起始等级可选 `1–15`，直接决定初始下落速度与计分等级系数。
- 暂停或切后台期间计时不累加，恢复后从原值继续。
- 「本会话最佳」只在当前页面内存中保留，刷新即清零（不做持久化）。

## 计分与速度

- 单/双/三/四行 = `100 / 300 / 500 / 800 × 当前等级`；软降每格 +1，硬降每格 +2。
- 每消 10 行升 1 级；下落速度按 Guideline 曲线 `(0.8 - (level-1) × 0.007) ^ (level-1)` 秒逐级加快，并设最小间隔下限。

## 可调参数

所有手感与规则参数集中在 `index.html` 中 `<script id="tetris-core">` 顶部的 `CONST` 对象，改完保存刷新即生效：

| 常量 | 含义 | 默认值 |
| --- | --- | --- |
| `CELL` | 单元格像素尺寸 | `30` |
| `DAS_MS` / `ARR_MS` | 长按起步延迟 / 连发间隔 | `170` / `50` |
| `LOCK_DELAY_MS` / `LOCK_RESET_LIMIT` | 锁定延迟 / 最大重置次数 | `500` / `15` |
| `SOFT_DROP_FACTOR` | 软降为当前重力的倍数 | `20` |
| `CLEAR_ANIM_MS` | 消行闪白时长 | `120` |
| `SCORE_TABLE` | 消行计分表 | `{1:100, 2:300, 3:500, 4:800}` |
| `SPRINT_LINES` / `ULTRA_MS` | 竞速行数 / 冲刺时长 | `40` / `120000` |
| `START_LEVEL_MIN` / `START_LEVEL_MAX` | 起始等级范围 | `1` / `15` |
| `KEYS` | 键位映射（基于 `event.code`） | 见上表 |

## 开发期自测（可选）

```bash
node tools/selftest.mjs
```

从 `index.html` 抽取内核脚本在 Node 沙箱中执行，断言覆盖消行、碰撞、SRS 踢墙、计分、7-bag 分布、三种模式判定与状态机推进。脚本与游戏运行无关，可随时删除。

## 明确不做（范围外）

移动端触控与屏幕按键、音效、最高分持久化、多人在线对战、排行榜/账号、关卡编辑器、皮肤系统、成就系统、AI 自动游玩、PWA 离线安装、多语言、存档续玩。
````

- [ ] **Step 2: 在 `// @@ASSERTIONS@@` 处插入 README 断言**

在自测脚本顶部读取 README（放在 `extractScript` 定义之后即可）：

```js
const readme = readFileSync(path.join(root, 'README.md'), 'utf8');
```

断言：

```js
// ===== Task 10: 交付物完整性与 README 覆盖度 =====
ok('README.md 存在且非空', readme.length > 200);
ok('README 覆盖打开方式', /如何打开/.test(readme) && /index\.html/.test(readme));
ok('README 覆盖完整键位表', ['左移', '硬降', '暂存', '暂停', '顺时针', '逆时针'].every(function (k) { return readme.indexOf(k) >= 0; }));
ok('README 覆盖三种模式', /马拉松/.test(readme) && /40 行竞速/.test(readme) && /限时冲刺/.test(readme));
ok('README 覆盖可调参数（CONST）', /CONST/.test(readme) && /LOCK_DELAY_MS/.test(readme) && /SPRINT_LINES/.test(readme));
ok('README 说明不做持久化', /持久化/.test(readme));
ok('页面不引用 README', !/README\.md/.test(html));
ok('页面不引用 tools/', !/tools\//.test(html));
```

- [ ] **Step 3: 交付物与合规终检**

```bash
ls -1 index.html README.md tools/selftest.mjs
node tools/selftest.mjs
sed -n '/<script id="tetris-core">/,/<\/script>/p' index.html | sed '1d;$d' | node --check /dev/stdin
sed -n '/<script id="tetris-shell">/,/<\/script>/p' index.html | sed '1d;$d' | node --check /dev/stdin
wc -l index.html README.md tools/selftest.mjs
```
说明（无 Node 时的等价做法）：跳过两条 `node --check`，改为浏览器打开页面并确认 DevTools Console 无报错；`node tools/selftest.mjs` 按「前置条件与验证降级路径」第 2 条在 Console 中执行。

Expected:
- 三个文件都存在；`selftest: <N> passed, 0 failed`（或 Console 中输出同一汇总行且无 FAIL）；两段脚本 `node --check` 均无输出（无 Node 时以浏览器无报错替代）；
- `index.html` 行数落在 700–1000 行区间（设计 §4 预估 700–900 行，含样式与注释允许略高）；
- `grep` 类检查改用下面这条（本仓库禁用 grep，用 node 代替）：

```bash
node -e "const h=require('fs').readFileSync('index.html','utf8');const bad=[/https?:\/\//i,/type=\"module\"/,/<script[^>]+src=/i,/localStorage|sessionStorage/,/touch(start|move|end)/i];const hit=bad.filter(r=>r.test(h));console.log(hit.length===0?'compliance: OK':'compliance: FAIL '+hit);process.exit(hit.length?1:0)"
```
Expected: `compliance: OK`

- [ ] **Step 4: 手工验收（双击打开，逐条对照设计 §7 验收标准）**

在有图形界面的环境双击 `index.html`，按下列清单逐项打勾（**不接受“看起来没问题”**，必须逐条操作）：

| # | 验收项 | 操作 | 期望 |
| --- | --- | --- | --- |
| 1 | file:// 直接可玩 | 断网后双击打开 | 正常进入模式选择面板，DevTools 无报错、无网络请求 |
| 2 | 画面无控制台报错 | 打开 DevTools Console | 无 error / warning |
| 3 | 7 种方块与颜色 | 连玩约 30 块 | `I O T S Z J L` 均出现且颜色可区分 |
| 4 | 移动与旋转即时反馈 | `←/→ ↑ Z` | 每次按键立即生效 |
| 5 | 贴墙/堆叠旋转借踢墙 | 把 I 贴左墙后旋转 | 能立起/放平，不卡死 |
| 6 | 幽灵块 | 观察落点提示 | 幽灵位置 = 实际硬降落点 |
| 7 | 消行 | 填满一行 | 整行闪白后消失，上方整体下移 |
| 8 | 计分 | 消 1/2/3/4 行 | 分别 +100/300/500/800 × 等级 |
| 9 | 软降/硬降加分 | 软降 5 格、硬降 10 格 | +5 / +20 |
| 10 | 等级加速 | 消满 10 行 | 等级 +1，下落明显变快 |
| 11 | Hold | `C` 两次 | 首次存入；同一块落地前第二次无效；落地后可再换 |
| 12 | 模式与起始等级选择 | 面板切换 | 选择生效（等级高则初始更快） |
| 13 | 马拉松 | 堆到顶 | 结算显示总分/等级/消行 |
| 14 | 40 行竞速 | 消满 40 行 | 立即结算并显示用时 |
| 15 | 限时冲刺 | 等到倒计时 0 | 立即结算并显示总分 |
| 16 | 结算后重开 | 结算面板按 Enter | 返回选择面板，可换模式/等级重开 |
| 17 | 暂停正确 | `P` 暂停 10 秒再继续 | 方块冻结、计时不跳变、无时间泄漏 |
| 18 | 切后台自动暂停 | 切到别的标签页再回来 | 自动暂停，无瞬间掉落 |
| 19 | 无持久化 | 刷新页面 | 回到模式选择，历史进度不残留 |
| 20 | 全键盘可操作、无触控代码 | 全程只用键盘；移动端模拟器打开 | 面板与游戏均可键盘完成；无屏幕按键 |

- [ ] **Step 5: 关键截图留档（设计 §7-22）**

截取 4 张图并放到仓库外或按团队约定存放（本仓库不加图片资源）：模式选择面板 / 游戏中（含幽灵块与侧栏） / 暂停遮罩 / 三种模式结算遮罩。

- [ ] **Step 6: 最终提交**

```bash
git add index.html README.md tools/selftest.mjs
git commit -m "feat: 单文件俄罗斯方块（完整手感 + 三种挑战模式）与说明文档"
```

---

## Self-Review

### 1. Spec Coverage（设计 §1–§11 逐节对照）

| 设计要求 | 落实位置 |
| --- | --- |
| §2/§4 单文件、零依赖、`file://` 直接打开 | Task 1（骨架）、Task 9 Step 2（`type="module"`/外链/`http(s)` 断言）、Task 10 Step 3 |
| §4 交付独立 `README.md` 且不被页面引用 | Task 10 Step 1/2（`页面不引用 README` 断言） |
| §5 分层：内核不接触 DOM、外壳持有 DOM | Task 1 File Structure + Task 2–7 内核 / Task 8–9 外壳 |
| §6.1 常量与数据模型（10×20、隐藏 2 行、7 色、键位） | Task 2 |
| §6.2 棋盘碰撞/合入/消行纯函数 | Task 3 |
| §6.3 SRS 旋转与踢墙（JLSTZ / I 双表、依次试探、全失败保持原姿态） | Task 2（表）、Task 7（`tryRotate`）、断言「踢墙转平 +2」「全失败不变」 |
| §6.4 7-bag 随机（可注入种子） | Task 4 |
| §6.5 主循环、dt 钳制、状态机、重力曲线、锁定延迟、消行动画 | Task 7（`update`）、Task 9（`loop`）、Task 5（`gravityMs`） |
| §6.6 Next 队列 3 个、Hold（落地前一次） | Task 2/7（`NEXT_COUNT`、`canHold`）、Task 8（`drawNext`） |
| §6.7 三种模式数据化配置 + 起始等级 1–15 + 会话内最高分 + 结算重开 | Task 6、Task 9（面板、`ui.bestScore`） |
| §6.8 输入：DAS/ARR、按下沿、preventDefault、失焦暂停、无触控 | Task 8（`Input`）、Task 9（绑定与断言） |
| §6.9 渲染：网格/固定块/当前块/幽灵、圆角+高光、DPR、侧栏、覆盖层、resize | Task 8（`Renderer`）、Task 9（`syncOverlay`/`layout`） |
| §6.10 错误与边界（堆顶、踢墙失败、切后台、暂停计时、幂等） | Task 7（`topOut`/`tryRotate`/`togglePause`）、Task 9（`autoPause`） |
| §6.11 可测试性（`window.Tetris` 五模块 + Node 断言，不引框架） | Task 1（脚手架）、Task 7 Step 2/Step 3 |
| §7-A/B/C 验收标准 1–20 | Task 9 Step 2（可自动化的 1/2/19/20）、Task 10 Step 4（人工清单 1–20） |
| §7-D 验证方式（正文已写「Node 内核断言 + 手工清单 + 截图」） | Step 3 改为 `node tools/selftest.mjs`（仓库禁用 grep，故不用 grep 校验） |
| §8 范围外 | Global Constraints + Task 9 静态断言（无触控/无音效/无持久化） |
| §9 风险（踢墙抄写错误、手感主观、模式分支分散） | Task 2 表结构断言、Task 7 定向场景断言、`CONST` 集中参数、Task 6 数据化模式 |

无遗漏章节；§10/§11 为交付物与确认结果，已落到 File Structure 与 Global Constraints。

### 2. Placeholder Scan

- 全文无 `TBD` / `TODO` / “稍后补充” / “类似 Task N” / “添加适当的错误处理” 等表述。
- 每个代码步骤都给出**完整可粘贴**的代码；每个验证步骤都给出精确命令与期望输出（`failed 为 0` 为硬验收条件）。
- 本文档唯一允许的“占位”是 `// @@ASSERTIONS@@` 标记本身：它是 `tools/selftest.mjs` 中**真实存在**的插入点，Task 1 创建、Task 2–10 逐次在其上方插入断言块，属文件结构而非内容占位。

### 3. Type & Naming Consistency

- 棋盘变量统一命名为 `board`（`Board.collides/merge/fullRows/clearRows/clearLines` 的入参与返回值均为 `board`；`clearLines` 返回 `{ board, cleared, rows }`），未出现 `grid` / `layers` 等别名。
- 旋转态编号统一为 `0=出生, 1=R, 2=2, 3=L`，踢墙表键固定 `'起始>目标'`；`Tetromino.cells(type, rot)` 内部对 `rot` 取正模，`cells('T', 5) === cells('T', 1)`。
- `GameState.spawn(state, forcedType?)` 的第二个参数在测试中被使用（`GameState.spawn(gs, 'I')`），实现中为 `forcedType || state.rng.next()`，语义一致。
- `state.clearing` 恒为 `null` 或 `{ rows: number[], timerMs: number }`；`Renderer.drawBoard` 与 `GameState.update` 读取字段一致。
- `intent.type` 取值集合（`move/rotate/softDrop/hardDrop/hold/pause`）在 `Input.intentFor`、`GameState.applyIntent`、Task 9 的 `{ type: 'pause' }` 调用三处完全一致。
- `ui` 字段（`modeIndex/startLevel/bestScore/lastStatus`）仅在 Task 9 内部使用，未与 `state` 字段混用。
- 分数/等级推进只在 `Scoring.applyClear` 一处发生，`GameState` 不重复实现计分算术。

### 4. 风险与回退

| 风险 | 本计划的处理 |
| --- | --- |
| 踢墙表抄错导致「某些旋转失效」 | Task 2 断言逐个旋转态验证格数/唯一性并锁定候选序列；Task 7 用「I 贴左墙借 `(2,0)` 踢墙转平」「1 格宽竖井内旋转失败且姿态不变」两个定向场景兜底 |
| 单文件过长难维护 | 固定分节注释 + 内核/外壳两区块隔离；若未来要工程化，按 §5 分层直接迁到多文件 |
| 手感参数主观 | `CONST` 集中一处可调，README 提供参数表 |
| 外壳（DOM 相关）无法自动断言 | 用静态断言锁定接线与合规红线（Task 8/9），再用 Task 10 Step 4 的 20 条人工清单兜底；不引入测试框架（符合设计 §6.11） |
| 计划与实现漂移 | 每个任务以「`node tools/selftest.mjs` 无 FAIL + `node --check` 两段脚本通过」作为进入下一任务的门槛 |

---

## 执行说明（Handoff）

本计划已保存。执行阶段（编码节点）请按 Task 1 → Task 10 顺序逐任务执行，每个任务完成后运行 `node tools/selftest.mjs` 与 `node --check`，**只有 `failed 为 0` 才进入下一任务**；Task 10 的人工清单是最终验收的唯一凭据。
