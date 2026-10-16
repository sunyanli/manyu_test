# Code Review Checklist

> **Change** 俄罗斯方块单文件 HTML 小游戏（`file://` 双击即玩） · **分支/Commit** `AI/task-DEV-9d10e310…` / `070af4a`（编码实现 round 1） · **日期** 2026-10-16
>
> **AI 进度源**：状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`（本文件为唯一驱动源与进度跟踪源）。
>
> **执行顺序（强制）**：先对变更路径运行 `references/script/scan-all-rules.sh`（已执行，输出见 §3/§4 备注），再由 LLM 完成 Step 2–5 中脚本未覆盖项与误报复核。

---

## 预检 — 审查范围

| 项 | 内容 |
|---|---|
| 变更来源 | `git show --stat HEAD`（`070af4a [auto-dev] 编码实现 (stage: 编码实现, round: 1)`） |
| 变更文件 | `README.md`（+71）、`index.html`（+1283）、`tools/selftest.mjs`（+549），共 3 文件 / 1903 行新增 |
| 变更外文件 | `hello.py`、`cred-helper-test.txt`、`.superpowers/`、`.agents/plans/login.md` 等为历史任务遗留，**不在本次审查范围** |
| spec 输入 | `.agents/20261016-实现一个俄罗斯方块小游戏_直接在html/design.md`（系分，2479 行）、`.agents/plans/tetris.md`（实施计划，2339 行） |
| **Java 守卫结论** | 本次变更**不含任何 `.java` 文件**。按技能约定「无 Java 文件 → 终止」；但本节点产物验收条件强制要求 `cr_report.md` + `blocker_count`，故**不终止**：Java 专属规则（Alibaba B/M/I、Java 中间件类 G/S）整节标 `N/A(非 Java)`，改按**同等四维**（功能核对 → 可读性 → 可靠性/安全/Bug 模式 → 自定义扩展）对 HTML/JS 变更做语言等价审查，规则 ID 缺失处一律给出 `path:line` 证据。 |
| 二进制文件 | 无 |

---

## Step 1 — 执行队列（产物 A）

> 队列由 `git diff --name-only` 展开（禁 glob），一行一个仓库相对路径。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | Step4（G/S/B/M/I） | 总状态 |
|---|----------------------|----------|-------|-------|--------------------|--------|
| 1 | `index.html` | 游戏本体（内核 + 外壳 + 样式 + DOM），承载 F01–F27 全部功能点 | ✅ | ⚠️ | ⚠️ | ⚠️ 已审有问题 |
| 2 | `tools/selftest.mjs` | 开发期自测脚本（非运行交付物），承载 F28 与附录 B 断言 | ✅ | ⚠️ | ⚠️ | ⚠️ 已审有问题 |
| 3 | `README.md` | 文档交付物（F03；页面不引用） | ✅ | ✅ | ✅ | ✅ 已审 |
| — | `.agents/**`、`docs/**` | 过程文档（非代码），仅在 Step 2 作为 spec 依据引用 | N/A | N/A | N/A | 跳过（非交付代码） |

**Java 专属规则适用性**：`bug-pattern-checklist.md` 的 B/M/I（120 条阿里 Java 缺陷规则）与 G/S 中 Java/中间件专属条目（锁、事务、SQL 索引、ConnectTimeout、鉴权、密钥、上传等）在本变更**无对应实现面**，整节 `N/A(非 Java)`；脚本可程序化命中的通用条目（A3.4 行宽、G16.2 异常吞没）已实际执行并逐条复核（见 §3、§4）。

**收口核销**：队列 `⬜ 待审` = 0；跳过项均为非代码过程文档。

---

## Step 2 — 功能性检查（产物 B）

> REQ 全部来自 spec 原文（附录 A 验收 1–20 / 功能清单 F01–F28 / §5 业务规则），每条绑定至少一个变更文件。功能不符统一按技能约定标 P0 —— 本次**无 P0**；下表 `⚠️` 项为「规格契约未完全落地但默认路径行为等价」，按 §4/§5 复核后降级为 P1/P2，降级理由随条给出。

| REQ | 来源锚点（spec 证据） | 关联文件 | 核对证据（代码/执行） | 结论 |
|-----|----------------------|----------|----------------------|------|
| REQ-01 `file://` 双击即玩，无服务器/无网络请求 | design.md:2404（A-1）；design.md:39-41（约束）；F01/F02 | `index.html` | 无 `type="module"`、无 `<script src=`、无 `<link href=`、无 `http(s)://`（selftest.mjs:385-388 全部通过；人工复核一致） | ✅ |
| REQ-02 无控制台报错、零第三方依赖 | design.md:2405（A-2） | `index.html` | Node + DOM 桩冒烟跑通 `tetris-core` + `tetris-shell` 两区块，`frames.errors.length === 0`（selftest.mjs:511-541）；**真实浏览器未执行**（环境无浏览器，见 §1 限制） | ✅（含环境限制说明） |
| REQ-03 README 独立存在、页面不引用它 | design.md:2406（A-3）；F03 | `README.md` | README 覆盖打开方式/键位/三模式/参数表；`!/README\.md/.test(html)`、`!/tools\//.test(html)`（selftest.mjs:400-407） | ✅ |
| REQ-04 7 种方块、7-bag 每袋各一次、配色可区分 | design.md:2410（A-4）；F05/F18 | `index.html` | 70 次抽取 ×10 段每段 7 种无重复；同种子序列一致（selftest.mjs:154-181）；`CONST.COLORS` 7 键（index.html:148-151） | ✅ |
| REQ-05 五键操作即时反馈（← → ↑ Z ↓ Space） | design.md:2411（A-5）；F06–F09 | `index.html` | 键位映射 index.html:173-183；`keydown` → `Input.handleDown` → `GameState.applyIntent` index.html:1226-1227；冒烟断言「硬降经输入管线加分」selftest.mjs:532-535 | ✅ |
| REQ-06 贴墙/堆叠可借 SRS 踢墙旋转 | design.md:2412（A-6）；F07 | `index.html` | JLSTZ/I 两张表 index.html:227-246 与 SRS 官方表逐项比对（8 组转移 ×2 类型），`dy` 取反规则一致；「竖 I 贴左墙借 (2,0) 踢墙转平」selftest.mjs:256-265 | ✅ |
| REQ-07 幽灵块与硬降落点一致 | design.md:2413（A-7）；F13 | `index.html` | `ghostY` index.html:553-559 与 `hardDrop` index.html:683-692 同源；断言 `hardCells === ghost - spawnY` selftest.mjs:291-294 | ✅ |
| REQ-08 满行消除 / 多行 / 上方下移 | design.md:2414（A-8）；F12 | `index.html` | 单行/四行/未满行三组断言（selftest.mjs:125-150）；`clearRows` 补齐顶部空行 index.html:320-329 | ✅ |
| REQ-09 计分（100/300/500/800×等级，软降 1、硬降 2） | design.md:2415（A-9）；F17 | `index.html` | `SCORE_TABLE` index.html:154 与断言 selftest.mjs:184-197；硬降计分按 `ghostY - y` index.html:686-689 | ✅ |
| REQ-10 等级加速重力（每 10 行 1 级 + 下限） | design.md:2416（A-10）；F10 | `index.html` | `gravityMs` index.html:405-409；断言等级 1 = 1000ms、单调变快、≥16ms（selftest.mjs:192-194） | ✅ |
| REQ-11 Hold 可用，同一方块落地前仅一次 | design.md:2417（A-11）；F15 | `index.html` | `canHold` 语义 index.html:568-582/625-646；三断言（首次/重复拒绝/落地后可再用）selftest.mjs:319-329 | ✅ |
| REQ-12 READY 面板可选模式与起始等级且生效 | design.md:2418（A-12）；F22 | `index.html` | 面板按钮 3/15 个（selftest.mjs:521-522）；`START_LEVEL_MIN/MAX = 1/15` index.html:158-159；键盘导航 index.html:1186-1200 | ✅ |
| REQ-13 三模式目标与结算文案（马拉松/40 行/120s） | design.md:2419-2421（A-13/14/15）；F19–F21 | `index.html` | 模式数据化配置 index.html:440-474；独立探针：sprint 消满 40 行 → `status=GAME_OVER, lines=40, topOut=false`；Ultra 到点结算（selftest.mjs:358-368） | ✅ |
| REQ-14 结算后 Enter 换模式/等级重开 | design.md:2422（A-16）；F23 | `index.html` | `onEnter` index.html:1177-1183 + `toReady` index.html:775-779（READY 保留模式/等级选择） | ✅ |
| REQ-15 暂停/切后台冻结、计时不累加、无瞬间掉落 | design.md:2426-2427（A-17/18）；F23 | `index.html` | 暂停断言「方块冻结 + 计时不累加」selftest.mjs:331-341；`dt` 钳制 `MAX_DT_MS=250` index.html:696 + 断言 selftest.mjs:370-375；`autoPause`/`lastTs=0` index.html:1236-1243 | ✅（残留 P2-1，见 §4） |
| REQ-16 无持久化，刷新回选择面板 | design.md:2428（A-19）；F24 | `index.html` | 无 `localStorage/sessionStorage/document.cookie`（selftest.mjs:390）；`bestScore` 仅内存 index.html:1108 | ✅ |
| REQ-17 全键盘可完成、方向键/空格不滚页、无触控路径 | design.md:2429（A-20）；F26 | `index.html` | 统一 `preventDefault` index.html:1204；无 `touch*/pointer*`（selftest.mjs:389）；面板键鼠双通道 index.html:1123-1140 | ✅ |
| REQ-18 手感/规则参数集中于 `CONST`，外壳不硬编码 | design.md:47（可维护性）；F27；R-CONST-01/05（design.md:543/577） | `index.html` | 外壳数值全部取自 `CONST`（如 index.html:875/877/1204/921-923）；`CONST` 定义 index.html:141-184 | ✅ |
| REQ-19 引导期自检：`CONST` 合法性 + DOM 齐全，不通过输出 `CONST_001`/终止引导 | **R-CONST-03/04（design.md:545-546）**；**R-BOOT-09（design.md:2031）**；错误码表 design.md:425/537 | `index.html` | `index.html` 全文无 `CONST_001`、无任何常量/DOM 预校验；`grep -c 'CONST_001' index.html` = 0（本次实测） | ❌ **P1** |
| REQ-20 渲染失败降级：canvas/2D 缺失时跳过该层绘制，逻辑不受影响 | **RENDER_001（design.md:433）**；渲染与逻辑解耦 design.md:433/585 | `index.html` | `Renderer.setup` index.html:909-918 直接 `canvas.getContext('2d')`，元素缺失/取上下文失败即抛异常，冒泡出 `bootstrap` IIFE → 主循环不启动（与「跳过该层绘制、逻辑推进不受影响」不符） | ❌ **P1** |
| REQ-21 模式结束判定统一走 `Modes.isOver` | R-MODE-05（design.md:1341）；R-GS-13（design.md:1533） | `index.html` | `update` 直接调用 `state.mode.isFinished(state)` index.html:699；`Modes.isOver`（index.html:480-482）无任何调用点（实测 `Modes.isOver` 在 index.html 出现 0 次） | ⚠️ **P2**（行为等价，≤1 帧延迟） |
| REQ-22 堆顶结束文案 `message` = 「方块堆顶」 | R-GS-12（design.md:1532）；design.md:1452 | `index.html` | `topOut(state, '堆顶')` index.html:579/641；结算标题渲染为「本局结束 · 堆顶」（index.html:1167） | ⚠️ **P2**（文案偏差） |
| REQ-23 交付体积建议 `index.html` 约 700–1000 行 / < 60KB | design.md:46（体积约束） | `index.html` | 实测 1283 行 / 43817 B：**字节数达标**，**行数超建议上限约 28%** | ⚠️ **P2** |

**Step 2 章节级结论**：23 条 REQ 中 18 条 `✅`、2 条 `❌`（同一「引导自检 + 渲染降级」错误处理簇）、3 条 `⚠️`（文案/实现路径/体积，均不影响默认路径行为）。

---

## Step 3 — 可读性检查（产物 C，`readability-checklist.md` A1–A7）

| ID | 状态 | 备注 |
|----|------|------|
| A1.1 文件名 = 顶层类名 | N/A | 非 Java（交付形态为 `index.html` + `README.md`，见 design.md:2446-2447） |
| A1.2 编码 UTF-8 | ✅ | `index.html:4 <meta charset="utf-8">`；`README.md` 中文正常；`node tools/selftest.mjs` 输出中文正常 |
| A1.3 空白仅 ASCII 空格/换行，禁 Tab | ✅ | 三个文件均为空格缩进（2 空格，JS/HTML 惯例），无 Tab |
| A2.1–A2.5 源文件结构 / import 规范 | ⚠️ | 非 Java，但等价检查：`tools/selftest.mjs:8-11` 4 条 `node:` 内建 import 按 ASCII 序排列、无通配符 import；`index.html` 两区块分节注释清晰（index.html:135-138、811-814） |
| A3.1 K&R 大括号 / A3.7 关键字空格 / A3.8 运算符空格 | ✅ | 全文件 K&R 一致（如 index.html:547-551、870-889），未出现 `if(` / `}else` 变体 |
| A3.2 空 catch 不可简写 | ⚠️ | `tools/selftest.mjs:33`、`:483`、`:508` 三处 `catch` 非空但仅赋值不打印（见 §4 G16.2 复核结论：故意捕获后由断言输出，判定可接受） |
| A3.3 缩进 4 空格（Java）/ 禁 Tab | N/A | 非 Java；实际采用 2 空格 JS 惯例，页内一致 |
| A3.4 行宽 ≤ 120 字符 | ❌ | **11 处命中**（全部由 `scan-all-rules.sh` 命中并经人工复核确认）：`index.html:520`；`tools/selftest.mjs:66,121,123,193,195,196,197,215,216,296` |
| A3.5 换行规则 | ⚠️ | 少量长表达式未按运算符前换行（如 index.html:520），与 A3.4 同源 |
| A3.6 成员间空行 | ✅ | 模块间以 `/* ===== */` 分节 + 空行（index.html:140/186/268/350/390/427/487） |
| A4.1–A4.7 命名规范 | ✅ | 等价检查通过：常量 `UPPER_SNAKE_CASE`（index.html:141-184）、函数/字段 `lowerCamelCase`、命名空间 `PascalCase`（`Board`/`Tetromino`/`GameState`），无 `mXxx`/`xxx_` 前后缀 |
| A5.x 编码实践（`@Override`、日志等） | N/A(非 Java) | 无注解体系；对应等价面为「错误码/console 输出」，见 §4 |
| A6.x / A7.x 注释与文档 | ✅ | 分节注释 + 关键不变量注释齐备（如 index.html:225-226 踢墙坐标系、index.html:696 dt 钳制、index.html:1028 隐藏缓冲、index.html:1006 READY 首帧保护） |

---

## Step 4 — 可靠性 / 安全 / Bug 模式（产物 D）

### 4.0 自动化预扫（强制，已执行）

```text
$ bash /root/.agentix/skills/managed/dtazziboot-java-code-review/references/script/scan-all-rules.sh index.html tools/selftest.mjs README.md .
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: index.html tools/selftest.mjs README.md .
Engine:  ripgrep

[P0] G16.2 — CatchWithoutLogging: tools/selftest.mjs:33
[P0] G16.2 — CatchWithoutLogging: tools/selftest.mjs:483
[P0] G16.2 — CatchWithoutLogging: tools/selftest.mjs:508
[P2] A3.4 — LineWidthExceeded: index.html:520
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:121
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:123
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:193
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:195
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:196
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:197
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:215
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:216
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:296
[P2] A3.4 — LineWidthExceeded: tools/selftest.mjs:66

=== Summary: 14 findings (P0=3, P1=0, P2=11) | 52/222 rules scanned ===
SCAN_EXIT=1
```

**脚本命中复核（技能要求：正则误报需人工确认）**

| 脚本命中 | 复核结论 | 依据 |
|---|---|---|
| `[P0] G16.2` ×3（`tools/selftest.mjs:33/483/508`） | **判定为误报（不计阻塞）** | 三处均为「捕获后交断言输出」：`selftest.mjs:30-35` 存 `coreError` → `selftest.mjs:64` 断言并用 `coreError.message` 输出；`:480-485` 存 `frames.errors` → `:518` 打印 `stack`；`:503-511` 存 `shellLoadError` → `:511` 打印 `stack`。异常未被静默吞没，且该脚本非运行交付物（design.md:2448）。规则原意（Java 生产代码 catch 不记日志导致不可观测）在此不成立。 |
| `[P2] A3.4` ×11 | **确认成立** | 见 §3 A3.4 行；`index.html:520` 为 121 字符，其余为断言行超宽。 |

### 4.1 Bug 模式（`bug-pattern-checklist.md` B/M/I，120 条）

**整节 `N/A(非 Java)`**：变更文件为 `index.html`（HTML + 内联 JS）、`tools/selftest.mjs`（Node ESM）、`README.md`（Markdown），无 `.java` 源文件，阿里 Java 缺陷规则无适用对象。为避免以「不适用」代替审查，已按等价面（JS 未定义变量、类型不一致、状态机非法迁移、数组越界、异步/事件边界、深拷贝别名）人工补扫，命中项写入 §4.3。

### 4.2 可靠性（G）与安全（S）逐节核销

| 节 | 状态 | 备注 |
|----|------|------|
| G1 并发控制 / G2 幂等 / G3 事务 / G4 SQL 与索引 / G5 缓存 | N/A(非 Java) | 无服务端、无数据库、无中间件（design.md:38-42/50/54）。等价面：单线程事件循环，唯一写入者为 `GameState`（design.md:2378 方案 A），审查未发现竞态 |
| G6–G7 超时/重试、限流 | N/A | 无网络调用（design.md:41） |
| G8 资源释放 / 内存 | ⚠️ | 主循环每帧无大对象累积（绘制为定长循环，index.html:1015-1026/1030-1057）；`Board.merge/clone` 每锁块一次 O(22×10) 拷贝（index.html:281-306），无泄漏路径。残留建议见 §4.3 P2-10 |
| G9 边界条件 | ✅ | 越左/越右/越底/隐藏缓冲上方（`y<0` 合法）四向边界断言 selftest.mjs:108-117；`x`/`y` 越界判定 index.html:290-292 |
| G10 空值与默认值 | ✅ | `state.board === null`（READY）首帧保护 index.html:1006；`state.piece === null`（消行动画期）在各入口判空 index.html:549/557/596/685/714/1041 |
| G11 线程池/异步 | N/A | 无异步任务，仅 `requestAnimationFrame` 单循环（design.md:2386 方案 A） |
| G12 时间与时钟 | ⚠️ | 桌面运行、无服务器时钟依赖；`dt` 在内核被钳制（index.html:696），但输入层未钳制（**P2-1**） |
| G13 日志/可观测 | ❌ | 设计要求的本地观测通道（`CONST_001`/`RENDER_001`，design.md:2312）在 `index.html` 中缺失 → 与 REQ-19/REQ-20 同一问题（**P1**，见 §7.1） |
| G14 灰度/开关 | N/A | 单文件本地运行，无灰度面（design.md:2371） |
| G15 兼容性 | ✅ | 仅用 `const/let`、函数表达式、模板/字符串拼接、`Math.imul`、`String#padStart`（index.html:429）、`classList.toggle`，均为设计允许集（design.md:44）；未用触控/实验 API |
| G16 异常处理 | ⚠️ | 内核「返回安全值」策略基本落地（`kicksFor` 回退 `[[0,0]]` index.html:253、`Modes.byId` 回退首模式 index.html:477-479、`applyIntent` 非法状态幂等 index.html:751）；缺口为渲染/引导层（**P1**）+ 脚本三处 catch（复核误报，§4.0） |
| G17 变更可应急 | ✅ | 参数集中 `CONST`，改完刷新即生效（design.md:2372） |
| S1 SQL 注入 / S2 XSS / S3 命令注入 / S4 SSRF / S5 反序列化 / S6 认证授权 / S7 文件上传 / S8 访问控制 / S9 数据安全 / S10 CSRF-CORS | N/A | 无服务端、无输入输出接口、无第三方脚本、无用户数据处理（design.md:41-42/49、2358）。**XSS 等价检查已做**：全部 DOM 文案走 `textContent`（index.html:1122/1135/1062-1066/1161-1168），无 `innerHTML`/`insertAdjacentHTML`/`eval`/`new Function`（人工全文核验），无注入面 |

### 4.3 等价面人工补扫发现（JS/HTML 专项）

| 编号 | 等级 | 定位 | 说明 |
|------|------|------|------|
| P2-1 | P2 | `index.html:1246-1249` | `Input.poll(dt)` 使用**未钳制**的原始 `dt`，而内核用 `MAX_DT_MS` 钳制（`index.html:696`）。主线程长卡顿（>170ms）后单帧可批量连发 `while (leftTimer >= ARR_MS)`（`index.html:877/885`），方块瞬间贴墙；与设计「dt 双保险钳制」（design.md:2462）不完全一致 |
| P2-2 | P2 | `index.html:659` | `lockPiece` 在消行时置 `softDropping = false`，而消行动画期间重力本已被 `update` 提前返回挡住（`index.html:707-711`）。**实测复现**（本次独立探针）：按住 ↓ 语义下软降消掉 1 行后 `line=1, softDropping=false` → 玩家须松开再按 ↓ 才恢复软降，与 F06/F08「完整手感」不符 |
| P2-3 | P2 | `index.html:699`、`index.html:480-482` | R-GS-13 要求「`update` 末尾执行 `Modes.isOver`」，实现改为开头调用 `mode.isFinished`，`Modes.isOver` 成为**无调用点**的导出（`index.html` 中出现 0 次）。行为等价（≤1 帧延迟，堆顶由 `spawn` 直接结算），但契约未落地 |
| P2-4 | P2 | `index.html:579/641` vs design.md:1532 | 堆顶 `message` 取 `'堆顶'`，设计为 `'方块堆顶'`；结算标题渲染「本局结束 · 堆顶」 |
| P2-5 | P2 | `index.html:677` | `state.message = 'LEVEL ' + state.level` 为**死写入**：`message` 唯一消费者是 GAME_OVER 分支且仅在 `topOut` 为真时拼进标题（`index.html:1167`），升级提示永不可见 |
| P2-6 | P2 | `index.html:1006`、`index.html:775-779` | ①首帧 READY 时 `drawBoard` 在绘制背景/网格**之前**提前返回（`1006 < 1010-1026`），画布为透明空白，R-BOOT-08 要求的「画布空白网格已绘制」未达成；②`toReady` 不重置 `board`，GAME_OVER→READY 后旧堆叠仍被绘制在遮罩之下（实测 `board != null` 仍为真） |
| P2-7 | P2 | `index.html`（整文件） | 1283 行，超设计建议 700–1000 行约 28%（字节 43817 B < 60KB 达标，design.md:46） |
| P2-8 | P2 | `index.html:520`；`tools/selftest.mjs:66,121,123,193,195,196,197,215,216,296` | A3.4 行宽 >120（脚本命中已复核确认） |
| P2-9 | P2 | `index.html:392-394`、`index.html:411-415` | `SCORING_001`（design.md:430）要求 `cleared ∉ 0..4` 时「计 0 分且**不推进等级**」；实现为 `SCORE_TABLE[cleared] || 0`（分数为 0）但 `lines += cleared` 仍推进等级。当前不可达（`fullRows` 最多 4 行），属防御性缺口 |
| P2-10 | P2 | `index.html:209-212` | `buildRotations` 对 `size === 2`（O）返回 `[flat, flat, flat, flat]`，四个旋转态**共享同一数组引用**；当前无写入点，属潜在别名风险（一旦未来就地修改 `cells` 即污染 4 个姿态） |

---

## Step 5 — 自定义扩展检查（产物 E）

**`N/A(未启用自定义规则)`** —— `references/customized-checklist.md` 仅含示例项（U1.1），无团队/项目私有规则；仓库内亦未发现 `REVIEW.md`/团队规范文件（项目指令检测：无）。

---

## 终检（防漏检）

- [x] 执行队列每个文件 `Step2`/`Step3`/Step4 各列均非 `⬜`（跳过项为过程文档，已标 `N/A`）
- [x] Step 2 的 23 条 REQ 均非 `⬜`
- [x] Step 3 的 A1–A7 均非 `⬜`
- [x] Step 4 的 G/S 各节均非 `⬜`；B/M/I 整节 `N/A(非 Java)` 并给出等价补扫（§4.3）
- [x] Step 5 全部 `N/A(未启用自定义规则)`
- [x] 所有 `❌/⚠️` 均已写入 `cr_report.md`，且包含 `规则ID + path:line`

---

## 独立验证证据（本次实测，非推断）

| 项 | 命令 | 结果 |
|---|---|---|
| 项目自带检查（唯一既有 harness） | `node tools/selftest.mjs` | `selftest: 410 passed, 0 failed`（exit 0）（Node v22.20.0） |
| 规则预扫 | `scan-all-rules.sh index.html tools/selftest.mjs README.md .` | 14 findings（P0=3 复核为误报 / P2=11 确认），exit 1 |
| 独立复算探针（自构造输入，非 sample） | 抽取 `tetris-core` 于 `node:vm` 沙箱执行 | ①按住软降消 1 行后 `softDropping=false`（P2-2）；②连续 20 次移动后 `lockResets=15` 封顶、方块未锁定（R-GS-10 合规）；③sprint 消满 40 行 → `GAME_OVER/lines=40/topOut=false` 且新方块仍在场（P2-6②）；④`toReady` 后 `board != null` |
| 静态合规红线 | 正则全文核验 | 无 `type="module"`、无外链、无 `http(s)://`、无触控、无本地存储、无 `innerHTML`/`eval` |
