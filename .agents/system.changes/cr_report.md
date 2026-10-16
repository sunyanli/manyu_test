# Code Review Report

> **Change** 俄罗斯方块单文件 HTML 小游戏（`index.html` + `README.md` + `tools/selftest.mjs`）
> **分支 / Commit** `AI/task-DEV-9d10e310-7901-11f1-8a9f-59ecae612580-fe192bfe-0d5c-46e9-9dcd-30a68fb26c1c` / `070af4a`（`[auto-dev] 编码实现 (stage: 编码实现, round: 1)`）
> **日期** 2026-10-16 · **审查者** AiWork（skill: `dtazziboot-java-code-review` v1.1.0）
> **配套清单** `.agents/system.changes/2026-10-16-tetris-html-cr-checklist.md`

---

## 1. 审查范围与预检

| 项 | 内容 |
|---|---|
| 变更（`git show --stat HEAD`） | `README.md` +71 / `index.html` +1283 / `tools/selftest.mjs` +549 = 3 文件 1903 行新增 |
| spec 输入 | `.agents/20261016-实现一个俄罗斯方块小游戏_直接在html/design.md`（系分设计，2479 行）；`.agents/plans/tetris.md` = `docs/superpowers/plans/2026-10-16-tetris-html.md`（实施计划，2339 行） |
| 变更范围外 | `hello.py`、`cred-helper-test.txt`、`.superpowers/`、`.agents/plans/login.md`（历史任务遗留，未纳入审查） |
| 二进制文件 | 无 |

### 1.1 Java 守卫的处置（重要说明）

技能预检规定「无任何 `.java` 文件 → 立即终止。本技能仅适用于 Java 代码审查」。本次变更**不含 `.java` 文件**，守卫条件成立；但本节点产物验收条件**强制要求**生成 `.agents/system.changes/cr_report.md` 并给出 `blocker_count`。

处置（按「产物硬性约束优先于 skill 内部约定」）：

1. 不终止审查流程，避免本节点无产物；
2. Java 专属规则整节标 `N/A(非 Java)`：`bug-pattern-checklist.md` 的 B/M/I（120 条阿里 Java 缺陷规则）、G/S 中 Java/中间件专属条目（锁/事务/SQL/鉴权/密钥/上传/反序列化等）——本变更无对应实现面；
3. 对本次真实变更对象（HTML + 内联 JS + Node ESM 脚本 + Markdown）**按同等四维**执行：功能核对（Step 2，REQ ↔ spec ↔ 代码）、可读性（Step 3，A1–A7 含语言等价）、可靠性/安全/Bug 模式（Step 4，含脚本预扫与人工误报复核、JS 等价面补扫）、自定义扩展（Step 5）；
4. 规则 ID 不适用处一律以 `path:line` + 实测证据替代，保证结论可核销。

### 1.2 验证环境限制（诚实声明）

- 本环境**无浏览器/无头浏览器**，无法执行设计附录 A 的「手工端到端验收」与附录 B 的「截图留档」；因此本报告**未**做任何视觉/可玩性人工确认，也未声称完成浏览器渲染核验。
- 已执行的替代验证（设计附录 B 已把「无 Node 环境降级」列为等价路径，此处是反向：有 Node、无浏览器）：`node tools/selftest.mjs`（内核断言 + 静态合规断言 + DOM 桩冒烟）、`scan-all-rules.sh`、以及本次自建的沙箱复算探针（见 §6）。
- 因此 REQ-02（无控制台报错）的结论限于「Node + DOM 桩下两区块加载与 3 帧主循环零异常 + 静态红线全部通过」，真实浏览器行为仍待人工验收。

---

## 2. Step 1 — 执行队列

| # | 文件 | 归属原因 | 结论 |
|---|------|----------|------|
| 1 | `index.html` | 游戏本体（内核 + 外壳 + 样式 + DOM），承载 F01–F27 | ⚠️ 已审有问题 |
| 2 | `tools/selftest.mjs` | 开发期自测脚本（F28，非运行交付物） | ⚠️ 已审有问题 |
| 3 | `README.md` | 文档交付物（F03，页面不引用） | ✅ 已审 |

队列无 `⬜ 待审`；过程文档（`.agents/**`、`docs/**`）仅作 spec 依据，不列为代码审查对象。

---

## 3. Step 2 — 功能性检查（REQ ↔ spec ↔ 代码）

REQ 全部来源于 spec 原文锚点，逐条绑定变更文件与代码/执行证据（完整表见 checklist Step 2）。

| 结论 | 条目 |
|---|---|
| ✅ 通过（18） | REQ-01 `file://` 双击即玩·无服务器/无网络 · REQ-02 零依赖无报错（含环境限制） · REQ-03 README 独立且页面不引用 · REQ-04 7 方块 + 7-bag + 配色 · REQ-05 五键即时反馈 · REQ-06 SRS 踢墙 · REQ-07 幽灵块与硬降落点一致 · REQ-08 满行/多行消除 · REQ-09 Guideline 计分 · REQ-10 等级加速重力 · REQ-11 Hold 限一次 · REQ-12 面板模式与起始等级 · REQ-13 三模式目标与结算 · REQ-14 结算后重开 · REQ-15 暂停/切后台冻结不累加 · REQ-16 无持久化 · REQ-17 全键盘可完成无触控 · REQ-18 参数集中 `CONST` |
| ❌ 不符（P1，2） | REQ-19 引导期自检（`CONST_001`/R-CONST-03/04/R-BOOT-09） · REQ-20 渲染降级（`RENDER_001`） |
| ⚠️ 偏差（P2，3） | REQ-21 未走 `Modes.isOver`（行为等价） · REQ-22 堆顶文案 `'堆顶'` ≠ `'方块堆顶'` · REQ-23 `index.html` 行数超建议上限 |

**关键合规实证（人工逐项比对，非脚本推断）**

| 契约 | 证据 |
|---|---|
| SRS 踢墙表（design.md:654-800 / 5.2） | `index.html:227-246` 的 JLSTZ `K1 0>1/1>0/1>2/2>1/2>3/3>2/3>0/0>3` 与 I 表 8 组候选，逐项与 SRS 官方表（y 轴向上）取反 `dy` 后**完全一致** |
| 7-bag 与袋内完整性 | `index.html:365-385` Fisher–Yates + `peek` 跨袋补足；实测 70 次抽取 10 段各 7 种不重复 |
| 计分/等级/重力 | `index.html:392-409`；等级 1 = 1000ms、单调不增、下限 16ms 实测通过 |
| 状态机幂等（`STATE_001`） | `index.html:745-763` 非法状态下 `pause` 之外 intent 一律忽略 |
| 契约表 S01–S26 | 26 项签名/命名与实现逐一对齐（`window.Tetris` 恰为 5 个内核模块，`index.html:801-807`；断言 selftest.mjs:222-223） |

---

## 4. Step 3 — 可读性检查（A1–A7）

| 结论 | 内容 |
|---|---|
| ✅ 通过 | A1.2 UTF-8 · A1.3 无 Tab · A3.1 K&R · A3.6 分节空行 · A3.7/A3.8 空格风格 · A4.x 命名（常量 `UPPER_SNAKE_CASE`、命名空间 `PascalCase`、字段 `lowerCamelCase`）· A6/A7 注释与分节文档（`index.html:135-138`、`811-814` 区块说明；关键不变量均有注释） |
| ⚠️ 观察 | A2.1–A2.5 非 Java，等价检查通过（`tools/selftest.mjs:8-11` 内建 import 无通配、按序）；A3.2 三处 catch 非空但仅赋值 → 复核为可接受（见 §5.1 G16.2） |
| ❌ 命中（P2） | **A3.4 行宽 >120 共 11 处**（全部由 `scan-all-rules.sh` 命中并经人工复核确认）：`index.html:520`；`tools/selftest.mjs:66,121,123,193,195,196,197,215,216,296`；A3.5 同源换行风格 |
| N/A | A1.1、A3.3（Java 4 空格缩进）、A5.x（`@Override` 等注解实践）→ `N/A(非 Java)` |

---

## 5. Step 4 — 可靠性 / 安全 / Bug 模式

### 5.0 自动化预扫（强制，已执行）

命令：`bash .../references/script/scan-all-rules.sh index.html tools/selftest.mjs README.md .`
结果：`14 findings (P0=3, P1=0, P2=11) | 52/222 rules scanned`，`exit=1`（原始输出逐行收录于 checklist §4.0）。

### 5.1 命中复核（技能要求：脚本需人工复核误报）

| 命中 | 结论 | 复核依据 |
|---|---|---|
| `[P0] G16.2 CatchWithoutLogging` ×3（`tools/selftest.mjs:33/483/508`） | **误报，不计阻塞** | 三处 catch 均为「捕获 → 交断言输出」：`:33`→`:64` 断言并用 `coreError.message` 输出；`:483`→`:518` 打印 `stack`；`:508`→`:511` 打印 `stack`。异常未被静默吞没；且该文件是开发期脚本（design.md:2448），不在交付运行路径。 |
| `[P2] A3.4 LineWidthExceeded` ×11 | **成立** | 见 §4 |
| 其余脚本未覆盖项 | 由 LLM 逐条补扫 | 见 5.2 / 5.3 |

> 说明：脚本报告的 3 条 `P0` 若照单全收将形成 3 个「阻塞」，但规则原意（Java 生产代码 catch 不记日志导致不可观测）在「非交付的断言脚本」上不成立，故按技能「人工确认误报」条款核销为 0 阻塞。

### 5.2 可靠性（G1–G17）与安全（S1–S10）

| 结论 | 内容 |
|---|---|
| N/A（非 Java / 无对应面） | G1–G5（并发/幂等/事务/SQL/缓存）、G6–G7（超时重试/限流）、G11（线程池）、G14（灰度）、S1–S10 全部（SQL 注入/XSS/命令注入/SSRF/反序列化/认证授权/上传/访问控制/数据安全/CSRF-CORS）——本系统无服务端、无网络、无存储、无第三方脚本（design.md:38-42/49-50）。 |
| ✅ 通过 | G9 边界条件（越界四向 + `y<0` 合法，`index.html:285-295`，断言 selftest.mjs:108-117）· G10 空值/默认值（`board===null`、`piece===null` 全入口判空）· G15 兼容性（仅用设计允许的特性集，design.md:44）· G17 可应急（参数集中 `CONST`） |
| ⚠️ 部分 | G8 内存/资源（无泄漏路径；`Board.clone` 每次锁块 O(220) 拷贝，规模可接受）· G12 时钟（内核 `dt` 已钳制 `MAX_DT_MS=250`，输入层未钳制 → P2-1）· G16 异常处理（内核「安全值」策略落地：`kicksFor` 回退 `index.html:253`、`Modes.byId` 回退 `index.html:477-479`、`applyIntent` 幂等 `index.html:751`；缺口集中在引导/渲染层） |
| ❌ P1 | G13 可观测性：设计要求的本地观测通道 `CONST_001`/`RENDER_001`（design.md:2312、425、433、2031）在 `index.html` 中**完全缺失**（实测 `CONST_001` 出现 0 次）→ 与 REQ-19/REQ-20 合并为同一 P1 簇 |
| 安全等价检查（主动执行） | XSS：全部 DOM 写入走 `textContent`（`index.html:1122/1135/1062-1066/1161-1168`），全文无 `innerHTML`/`insertAdjacentHTML`/`eval`/`new Function`；无 `http(s)://`、无外链脚本/样式/媒体（selftest.mjs:386-388）；无 `localStorage/sessionStorage/cookie`（selftest.mjs:390）→ 无注入面、无敏感数据面。 |

### 5.3 Bug 模式（B/M/I）与 JS 等价面补扫

`bug-pattern-checklist.md`（120 条阿里 Java 缺陷规则）整节 **`N/A(非 Java)`**；为不以「不适用」替代审查，按等价面（未定义/未使用变量、类型与状态一致性、越界与别名、异步与事件边界、死代码、防御性缺口）人工补扫，命中如下（明细见 checklist §4.3）：

| 编号 | 等级 | 定位 | 摘要 |
|---|---|---|---|
| P2-1 | P2 | `index.html:1246-1249` | `Input.poll(dt)` 用未钳制 `dt`，长卡顿后 DAS/ARR 单帧批量连发（与 `index.html:696` 的内核钳制不一致） |
| P2-2 | P2 | `index.html:659` | 消行时静默 `softDropping=false`，按住 ↓ 的玩家消行后软降失效，须松开再按（**本次探针实测复现**） |
| P2-3 | P2 | `index.html:699` / `480-482` | R-GS-13 未落地：`update` 直接调 `mode.isFinished`，`Modes.isOver` 无调用点（行为等价） |
| P2-4 | P2 | `index.html:579/641` | 堆顶 `message` 文案 `'堆顶'` ≠ 设计 `'方块堆顶'` |
| P2-5 | P2 | `index.html:677` | `state.message = 'LEVEL n'` 为死写入（无消费者，升级提示不可见） |
| P2-6 | P2 | `index.html:1006` / `775-779` | READY 首帧未绘制空白网格（提前 return 在网格绘制之前）；`toReady` 不重置 `board`，GAME_OVER→READY 残留旧堆叠 |
| P2-7 | P2 | `index.html`（整文件） | 1283 行超设计建议 700–1000 行（字节 43817 B < 60KB 达标） |
| P2-8 | P2 | 见 §4 | A3.4 行宽超限 11 处 |
| P2-9 | P2 | `index.html:392-394/411-415` | `SCORING_001` 要求 `cleared ∉ 0..4` 不推进等级，实现仍 `lines += cleared`（当前不可达） |
| P2-10 | P2 | `index.html:209-212` | O 的 4 个旋转态共享同一数组引用（潜在别名风险） |

---

## 6. Step 5 — 自定义扩展检查

`N/A(未启用自定义规则)`：`references/customized-checklist.md` 仅含示例项（U1.1）；仓库无 `REVIEW.md` 或团队私有审查规范（项目指令检测：none）。

---

## 7. 结论

- **合并建议**：**修复后合并**（无 P0 阻塞；2 项 P1 为设计明文的错误处理契约未落地，建议合并前补齐）
- **P0（阻塞）**：无
- **P1（合并前应修复）**：
  1. `index.html` 缺引导期自检 —— R-CONST-03/04（design.md:545-546）、R-BOOT-09（design.md:2031）、`CONST_001`（design.md:537）；不通过时应 `console.error('CONST_001: …')` 并终止进入主循环。
  2. `Renderer` 缺 `RENDER_001` 降级 —— design.md:433 要求「canvas 缺失/取不到 2D 上下文时跳过该层绘制，逻辑推进不受影响」；当前 `index.html:909-918` 会抛异常并淹没整个 `bootstrap`（主循环不启动）。
- **P2（可选改进）**：P2-1 ~ P2-10（§5.3），另含 `index.html` 行数超建议上限与 11 处 A3.4 行宽。
- **一句话**：交付物「双击即玩」的硬约束、玩法完整度与计分/踢墙/计时等核心规则经断言与独立探针双重验证**全部成立**（`410 passed, 0 failed`，零 P0）；主要欠账是设计明文的**引导自检与渲染降级**两条错误处理契约未实现，以及少量手感/文案/风格瑕疵。

### 7.1 问题片段

- **P1** `R-CONST-03/04 + R-BOOT-09 + CONST_001` `index.html:1264-1279` —— 引导末尾直接 `buildChoices()/layout()/syncOverlay()` 后进入主循环，**无任何 `CONST` 合法性 / DOM 齐全性自检**，也无 `CONST_001` 诊断输出。
  片段范围：`index.html:1264-1279`（文件非 Java：`N/A(非 Java)` 不适用，按同等证据要求给出片段）

```
L1264|    buildChoices();
L1265|    layout();
L1266|    Renderer.bindStats(el);
L1267|    syncOverlay();
L1268|
L1269|    window.addEventListener('keydown', onKeyDown);
...
L1279|    requestAnimationFrame(loop);   // 问题：CONST/DOM 预校验缺失即进入主循环（R-CONST-03/04、R-BOOT-09 未落地）
L1280|  })();
```

spec 证据（design.md:545-546、2031）：`引导期自检：COLS>0、ROWS>0、HIDDEN_ROWS>=0、CELL>0、NEXT_COUNT>0、START_LEVEL_MIN<=START_LEVEL_MAX | 页面加载时一次 | 输出 CONST_001，终止引导`。
影响：`COLS/CELL` 等被误改为 `0` 时，`update` 的 `while (state.dropAccMs >= interval)`（`index.html:721`）可能以 0 间隔自旋，且玩家看不到任何错误提示。

- **P1** `RENDER_001` `index.html:909-918` —— 取元素/上下文失败即抛异常，未按契约降级跳过该层绘制。
  片段范围：`index.html:909-926`

```
L909|    function setup(canvas, cssW, cssH) {
L910|      const dpr = window.devicePixelRatio || 1;
L911|      canvas.width = Math.round(cssW * dpr);      // 问题：canvas 为 null 即 TypeError
...
L915|      const ctx = canvas.getContext('2d');        // 问题：上下文获取失败未降级
L916|      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
L917|      return ctx;
L918|    }
```

spec 证据（design.md:433）：`RENDER_001 | 目标 canvas 元素缺失或 2D 上下文获取失败 | 跳过该层绘制，逻辑推进不受影响（渲染与逻辑解耦）`。

- **P2** `A3.4` `index.html:520` / `tools/selftest.mjs:66,121,123,193,195,196,197,215,216,296` —— 行宽超 120 字符。
  片段范围：`index.html:520`

```
L520|      const level = Math.max(CONST.START_LEVEL_MIN, Math.min(CONST.START_LEVEL_MAX, startLevel || CONST.START_LEVEL_MIN));
```

- **P2** `P2-2 软降状态失同步` `index.html:656-662` —— 消行分支顺带清了 `softDropping`，而消行动画期重力已由 `update` 提前返回挡住（`index.html:707-711`），该清除纯属副作用。
  片段范围：`index.html:656-662`

```
L656|      const rows = Board.fullRows(state.board);
L657|      if (rows.length > 0) {
L658|        state.clearing = { rows: rows, timerMs: 0 }; // 动画期间暂停重力与锁定
L659|        state.softDropping = false;   // 问题：按键仍按住，但软降被静默清除（探针实测：消 1 行后 softDropping=false）
L660|      } else {
L661|        spawn(state);
L662|      }
```

- **P2** `P2-1 输入层 dt 未钳制` `index.html:1245-1251`

```
L1245|    function loop(ts) {
L1246|      const dt = lastTs === 0 ? 0 : ts - lastTs;   // 问题：未做 MAX_DT_MS 钳制
L1247|      lastTs = ts;
L1248|
L1249|      const intents = Input.poll(dt);              // 问题：DAS/ARR 按原始 dt 累加，卡顿后单帧批量连发
L1250|      for (let i = 0; i < intents.length; i++) GameState.applyIntent(state, intents[i]);
L1251|      GameState.update(state, dt);                 // 内核侧已钳制（index.html:696）
```

> 其余 `⚠️` 项（P2-3 ~ P2-10）为单行/短片段问题，定位与证据已在 checklist §4.3 逐条给出 `path:line`；本节不再重复贴片段。**所有命中均在 HTML/JS/ESM 文件，不存在 Java 片段**（`N/A(非 Java)` 用于 Java 专属规则本身）。

---

## 8. 修复任务列表

> 与 §3–§7 中 `❌/⚠️` 一一对应；先 P0、再 P1、最后 P2。

### P0

- 无（本次审查未发现阻塞级问题；脚本预扫报告的 3 条 `G16.2` 已复核为误报，依据见 §5.1）。

### P1

- [ ] **P1** `index.html:1264-1279` — 在引导末尾（`buildChoices()` 之前）增加引导期自检：校验 `COLS>0`、`ROWS>0`、`HIDDEN_ROWS>=0`、`CELL>0`、`NEXT_COUNT>0`、`START_LEVEL_MIN<=START_LEVEL_MAX`，以及 `KEYS` 覆盖移动/旋转/软降/硬降/暂存/暂停/开始全部动作；不通过时 `console.error('CONST_001: …')` 并 `return`（不进入主循环）。（R-CONST-03/04、R-BOOT-09）
- [ ] **P1** `index.html:909-926` + `index.html:1083-1085` — 为 `Renderer.setup/attach` 增加 `RENDER_001` 降级：元素缺失或 `getContext('2d')` 失败时记录一次 `console.error('RENDER_001: …')` 并跳过该层绘制（置对应 `ctx*` 为 `null`，绘制函数入口判空返回），保证逻辑推进不受影响；同时在引导自检中校验 13 个 DOM 元素齐全。

### P2（可选）

- [ ] **P2** `index.html:656-662` — 移除消行分支的 `state.softDropping = false`（或改为由输入层在按键抬起时统一收敛），避免按住 ↓ 消行后软降静默失效。
- [ ] **P2** `index.html:1245-1251` — `Input.poll` 改传钳制后的 `dt`（`Math.min(dt, CONST.MAX_DT_MS)`），消除 DAS/ARR 突发连发。
- [ ] **P2** `index.html:699` — 按 R-GS-13 改为 `update` 末尾统一调用 `Modes.isOver(state.mode, state)` 判定结束，消除 `Modes.isOver` 死导出。
- [ ] **P2** `index.html:579,641` — 堆顶 `message` 文案改为 `'方块堆顶'`，与设计一致。
- [ ] **P2** `index.html:677` — 删除或落地 `state.message = 'LEVEL ' + state.level`（当前无消费者，为死写入）。
- [ ] **P2** `index.html:1006`、`index.html:775-779` — ①`drawBoard` 把背景/网格绘制移到 `!state.board` 提前返回之前，满足 R-BOOT-08「READY 空白网格已绘制」；②`toReady` 时清空 `state.board`，避免 GAME_OVER 旧堆叠残留在 READY 遮罩下。
- [ ] **P2** `index.html`（整文件） — 拆分/精简外壳重复绘制代码，使行数回落到设计建议的 700–1000 行区间（当前 1283 行）。
- [ ] **P2** `index.html:520`；`tools/selftest.mjs:66,121,123,193,195,196,197,215,216,296` — 按 A3.4 折行至 ≤120 字符。
- [ ] **P2** `index.html:392-394,411-415` — 为 `Scoring.linePoints/applyClear` 增加 `cleared ∉ 0..4` 时「计 0 分且不推进等级 + 开发期告警」的防御分支（SCORING_001）。
- [ ] **P2** `index.html:209-212` — O 的 4 个旋转态改为各自独立数组（或 `Object.freeze`），消除共享引用别名风险。
