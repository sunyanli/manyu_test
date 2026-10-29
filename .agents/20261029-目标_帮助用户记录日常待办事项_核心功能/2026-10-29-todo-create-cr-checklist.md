# Code Review Checklist

> **Change** `todo-create`（新增待办事项最小闭环） · **分支/Commit** `task/DEV-966dcd0a…` / `009d45d` · **日期** `2026-10-29`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
>
> **执行顺序**：已先执行 `scan-all-rules.sh src/main/java src/test/java`（输出见 §3 备注），再由 LLM 完成 Step 2–5 未覆盖项。
>
> **列裁剪说明**：本 change 含 45 个变更文件（39 个 `.java`），逐文件铺开 G1–G17/S1–S10 共 28 列在单文件内不可读。Step 1 采用「文件级 + 检查域聚合」列（Step2 / Step3 / G / S / B-M-I / 总状态），**每个 G*/S*/B*/M*/I* ID 的逐条核销**在 §4.1–§4.3 单独建表完成，两者互相引用、无遗漏核销项。

---

## Step 1 — 执行队列（产物 A）

### 1.0 变更文件清单（非 Java 标注跳过）

| # | 文件 | 归属原因 | 状态 |
|---|------|----------|------|
| - | `pom.xml` | 构建依赖变更 | 跳过(非 Java) |
| - | `docs/ARCHITECTURE.md`、`docs/modules/todo/README.md` | 文档 | 跳过(非 Java) |
| - | `.agents/…/design.md`、`.agents/…/impl.md` | 系分/实现报告 | 跳过(非 Java，作 REQ 证据) |
| - | `src/main/resources/application.yml` | 配置 | 跳过(非 Java，G14/G17 证据) |
| - | `src/main/resources/db/todo_item.sql` | 建表脚本 | 跳过(非 Java，G15.1/S1 证据) |
| - | `src/main/resources/mapper/TodoItemMapper.xml` | SQL 映射 | 跳过(非 Java，S1.1/G4 证据) |

### 1.1 Java 执行队列（39 个）

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1–G17 | S1–S10 | B/M/I | 总状态 |
|---|----------------------|----------|-------|-------|--------|--------|-------|--------|
| 1 | `src/main/java/com/antdigital/todo/TodoApplication.java` | REQ-1 启动入口 | ✅ | ✅ | N/A(无并发/SQL/外部调用) | N/A(无入参面) | ✅ | ✅ 已审 |
| 2 | `src/main/java/com/antdigital/todo/common/constant/TodoConstants.java` | REQ-1/2/5 常量 | ✅ | ✅ | N/A(无逻辑) | N/A | ✅ | ✅ 已审 |
| 3 | `src/main/java/com/antdigital/todo/common/enums/DeleteFlagEnum.java` | REQ-1 状态固定 | ✅ | ✅ | N/A(无逻辑) | N/A | ✅ | ✅ 已审 |
| 4 | `src/main/java/com/antdigital/todo/common/enums/TodoErrorCodeEnum.java` | REQ-7 错误码契约 | ✅ | ✅ | N/A(无逻辑) | N/A | ✅ | ✅ 已审 |
| 5 | `src/main/java/com/antdigital/todo/common/enums/TodoSourceEnum.java` | REQ-2 来源校验 | ✅ | ✅ | N/A | N/A | ✅ | ✅ 已审 |
| 6 | `src/main/java/com/antdigital/todo/common/enums/TodoStatusEnum.java` | REQ-1 状态固定 | ✅ | ✅ | N/A | N/A | ✅ | ✅ 已审 |
| 7 | `src/main/java/com/antdigital/todo/common/exception/TodoBizException.java` | REQ-7 异常契约 | ✅ | ✅ | N/A | N/A | ✅ | ✅ 已审 |
| 8 | `src/main/java/com/antdigital/todo/common/exception/TodoDuplicateKeyException.java` | REQ-3 幂等冲突语义 | ✅ | ✅ | G2 见§4.2 | N/A | ✅ | ✅ 已审 |
| 9 | `src/main/java/com/antdigital/todo/common/model/ApiResult.java` | REQ-7 响应体 | ✅ | ✅ | N/A | N/A | ✅ | ✅ 已审 |
| 10 | `src/main/java/com/antdigital/todo/common/util/DesensitizeUtil.java` | REQ-4/R10 日志脱敏 | ✅ | ✅ | G16.2 ✅ | S9.2 ✅ | ✅ | ✅ 已审 |
| 11 | `src/main/java/com/antdigital/todo/common/util/TodoTextUtil.java` | REQ-2 R02/R04 | ✅ | ✅ | G11.2 ✅ | S6 ✅ | ✅ | ✅ 已审 |
| 12 | `src/main/java/com/antdigital/todo/controller/TodoController.java` | REQ-6 W01 入口 | ✅ | ⚠️(P2-10/14) | G10.1 ⚠️ | S8.1 ⚠️ | ✅ | ⚠️ 已审有问题 |
| 13 | `src/main/java/com/antdigital/todo/controller/TodoExceptionHandler.java` | REQ-7 异常映射 | ⚠️(P2-11) | ✅ | G10.1 ⚠️ | S2.1 ✅ | ✅ | ⚠️ 已审有问题 |
| 14 | `src/main/java/com/antdigital/todo/dao/entity/TodoItemDO.java` | REQ-1 数据对象 | ✅ | ✅ | N/A(数据载体) | S9.2 ✅ | ✅ | ✅ 已审 |
| 15 | `src/main/java/com/antdigital/todo/dao/manager/TodoItemManager.java` | REQ-3 唯一键语义 | ✅ | ✅ | G2 ⚠️ / G4 ✅ | S1 ✅ | ✅ | ⚠️ 已审有问题 |
| 16 | `src/main/java/com/antdigital/todo/dao/mapper/TodoItemMapper.java` | REQ-1/3 DAO 接口 | ✅ | ✅ | G4 ✅ | S1 ✅ | ✅ | ✅ 已审 |
| 17 | `src/main/java/com/antdigital/todo/integration/AuditIntegration.java` | REQ-4 审计接口 | ✅ | ✅ | G8.2 ✅ | N/A | ✅ | ✅ 已审 |
| 18 | `src/main/java/com/antdigital/todo/integration/AuthIntegration.java` | REQ-5 鉴权接口 | ✅ | ✅ | G9 ✅ | S8.1 ✅ | ✅ | ✅ 已审 |
| 19 | `src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java` | REQ-4 审计实现 | ✅ | ✅ | G8.4 ✅ / G8.1 ⚠️(P2-12) | N/A | ✅ | ⚠️ 已审有问题 |
| 20 | `src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java` | REQ-5 身份解析 | ⚠️(P1-1) | ⚠️(P2-14) | G10.1 ✅ | S8.1 ⚠️ | ✅ | ⚠️ 已审有问题 |
| 21 | `src/main/java/com/antdigital/todo/model/dto/OperatorContext.java` | REQ-5 可信上下文 | ✅ | ✅ | G11.3 ✅ | S9.2 ✅ | ✅ | ✅ 已审 |
| 22 | `src/main/java/com/antdigital/todo/model/dto/TodoCreateRequest.java` | REQ-2 入参 | ✅ | ✅ | G11.3 ✅ | S9.2 ✅ | ✅ | ✅ 已审 |
| 23 | `src/main/java/com/antdigital/todo/model/dto/TodoCreateResult.java` | REQ-4 回执 | ✅ | ✅ | N/A(数据载体) | S9.2 ✅ | ✅ | ✅ 已审 |
| 24 | `src/main/java/com/antdigital/todo/model/dto/ValidatedTodo.java` | REQ-2 校验出参 | ✅ | ✅ | N/A(数据载体) | S9.2 ✅ | ✅ | ✅ 已审 |
| 25 | `src/main/java/com/antdigital/todo/model/vo/TodoCreateVO.java` | REQ-4 W01 出参 | ⚠️(P2-10) | ✅ | G10.1 ✅ | N/A | ✅ | ⚠️ 已审有问题 |
| 26 | `src/main/java/com/antdigital/todo/service/TodoService.java` | REQ-1 S01/S02 | ✅ | ✅ | N/A(接口) | N/A | ✅ | ✅ 已审 |
| 27 | `src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java` | REQ-1/R09 取号 | ⚠️(P1-4) | ✅ | G1.1 ✅ / G9.3 ⚠️ | N/A | ✅ | ⚠️ 已审有问题 |
| 28 | `src/main/java/com/antdigital/todo/service/component/TodoNoGenerator.java` | REQ-1 S04 接口 | ✅ | ✅ | N/A(接口) | N/A | ✅ | ✅ 已审 |
| 29 | `src/main/java/com/antdigital/todo/service/component/TodoNoGeneratorImpl.java` | REQ-1 S04 实现 | ⚠️(P1-4/P2-7) | ✅ | G14.3 ⚠️ | N/A | ✅ | ⚠️ 已审有问题 |
| 30 | `src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java` | REQ-6 R11 限流 | ⚠️(P2-8/9) | ✅ | G1.1 ✅ / G6.1 ⚠️ | N/A | ✅ | ⚠️ 已审有问题 |
| 31 | `src/main/java/com/antdigital/todo/service/component/TodoSequenceProvider.java` | REQ-1 取号 SPI | ✅ | ✅ | N/A(接口) | N/A | ✅ | ✅ 已审 |
| 32 | `src/main/java/com/antdigital/todo/service/component/TodoValidator.java` | REQ-2 S03 校验 | ✅ | ✅ | G11.3 ✅ | S6 ✅ | ✅ | ✅ 已审 |
| 33 | `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java` | REQ-1~6 主链路 | ⚠️(P1-2/3/5/6) | ✅ | G3.2 ⚠️ / G16.1 ⚠️ / G17.1 ⚠️ | S8.1 ⚠️ | ✅ | ⚠️ 已审有问题 |
| 34 | `src/test/java/com/antdigital/todo/common/util/TodoTextUtilTest.java` | REQ-2 测试 | ✅ | ✅ | G11.1 ✅ | N/A | ✅ | ✅ 已审 |
| 35 | `src/test/java/com/antdigital/todo/controller/TodoControllerTest.java` | REQ-5/6 测试 | ✅ | ✅ | G11.2 ⚠️(P2-15) | N/A | ✅ | ⚠️ 已审有问题 |
| 36 | `src/test/java/com/antdigital/todo/dao/manager/TodoItemManagerTest.java` | REQ-3 测试 | ✅ | ✅ | G11.1 ✅ | N/A | ✅ | ✅ 已审 |
| 37 | `src/test/java/com/antdigital/todo/service/component/TodoNoGeneratorImplTest.java` | REQ-1 测试 | ✅ | ✅ | G11.1 ✅ | N/A | ✅ | ✅ 已审 |
| 38 | `src/test/java/com/antdigital/todo/service/component/TodoValidatorTest.java` | REQ-2 测试 | ✅ | ⚠️(P2-14) | G11.2 ✅ | N/A | ✅ | ⚠️ 已审有问题 |
| 39 | `src/test/java/com/antdigital/todo/service/impl/TodoServiceImplTest.java` | REQ-1~6 测试 | ✅ | ✅ | G11.2 ⚠️(P2-15) | N/A | ✅ | ⚠️ 已审有问题 |

> `⬜ 待审` 计数：**0**（39 个 `.java` 全部核销）。

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据 |
|-----|----------|----------------------|----------|------|----------|
| REQ-1 | Given 合法入参与可信身份，When 调用 POST /api/todo/item/create，Then 落库一条 `todo_item` 并返回 `todoNo`/`status=0`/`gmtCreate` | design §5.1.2 W01、§5.1.3.1 时序图、R08 | `TodoController.java`、`TodoServiceImpl.java`、`TodoItemMapper.xml` | ✅ | `TodoServiceImpl.java:119-147`；`TodoItemMapper.xml:42-51`；`TodoServiceImplTest#should_createTodo_when_validRequest` |
| REQ-2 | Given 名称/描述，When 校验，Then 名称 trim 后 1~128 字符、描述 0~512 字符、NFC 规范化并剔除控制字符，超长不截断直接报错 | design R02/R03/R04、A05、S03 | `TodoValidator.java`、`TodoTextUtil.java` | ✅ | `TodoValidator.java:69-92`；`TodoTextUtil.java:46-103`；`TodoValidatorTest`（11 用例含 128/129、512/513、emoji） |
| REQ-3 | Given 同一 `tenantId+requestId` 重复提交，When 创建，Then 返回首次结果且 `idempotent=true`，不产生重复记录 | design R07、§5.1.3.1 异常场景「唯一键冲突」、§5.1.1.1 UK `uk_todo_item_req` | `TodoServiceImpl.java`、`TodoItemManager.java`、`TodoItemMapper.xml` | ⚠️ | 主路径 `TodoServiceImpl.java:111-116`、`123-135` 正确；**冲突索引名解析依赖 DB 错误文案**（P1-3）`TodoItemManager.java:76-89` |
| REQ-4 | Given 创建成功，When 组装回执并留痕，Then 返回 id/todoNo/itemName/itemDesc/status/creatorId/gmtCreate/idempotent 且写审计 | design W01 出参表、R10、I02 | `TodoController.java`、`TodoCreateVO.java`、`TodoServiceImpl.java`、`AuditIntegrationImpl.java` | ⚠️ | 字段齐全；`data.id` 类型与系分（Long）不一致（P2-10）；审计在事务内发出（P1-2） |
| REQ-5 | Given 网关注入身份头，When 解析，Then `creatorId/creatorName/tenantId` 只取上下文、Body 身份字段被忽略；上下文缺失返回 TODO_0005；无权限返回 TODO_0006 | design R05/R06、§6.4.2.1/6.4.2.2 | `GatewayAuthIntegrationImpl.java`、`TodoServiceImpl.java`、`OperatorContext.java` | ⚠️ | Body 无身份字段 ✅；`userId` 缺失 → TODO_0005 ✅；**tenantId 缺失静默回落 "0"**（与 R05「上下文缺失→TODO_0005」不符）；**权限取自 `X-User-Roles` 头**（P1-1）`GatewayAuthIntegrationImpl.java:70-74`、`TodoServiceImpl.java:201-210` |
| REQ-6 | Given 同一用户高频创建，When 超过 30 次/分钟，Then 返回 TODO_0009 | design R11、§6.3 | `TodoRateLimiter.java`、`application.yml` | ⚠️ | 滑动窗口实现正确 `TodoRateLimiter.java:66-94`；配置项 `todo.create.rate-limit-per-minute` 未接线（P2-9） |
| REQ-7 | Given 各类失败，When 返回，Then 错误码与 HTTP 状态符合 W01 错误码表（TODO_0001~0009）且不泄露堆栈 | design §5.1.2 错误码表、§5.1.3.1 异常场景表 | `TodoExceptionHandler.java`、`TodoErrorCodeEnum.java`、`ApiResult.java` | ⚠️ | 映射表 `TodoExceptionHandler.java:169-187` 基本一致；未知异常统一映射 TODO_0008（P2-11） |
| REQ-8 | Given 发布/故障，When 运营操作，Then 可通过配置中心关闭 `todo.create.enabled` 并按租户尾号灰度 | design §7.2、§7.3 | `TodoServiceImpl.java`、`application.yml` | ❌ | 代码无开关/灰度判定，配置无该项（P1-6） |
| REQ-9 | Given 线上运行，When 观测，Then 有 `todo_create_total`/`todo_create_cost_ms` 等指标支撑 §6.5 的 5 条告警 | design §6.5、§7.1 | `TodoServiceImpl.java` | ❌ | 仅有日志，无指标埋点（P1-5） |
| REQ-10 | Given 编号冲突，When 插入失败，Then 重试 1 次取新号，仍冲突返回 TODO_0007 | design R09、S04 | `TodoServiceImpl.java`、`TodoConstants.java` | ✅ | `TodoServiceImpl.java:119-149`；`should_throwTodoNoConflict_when_todoNoAlwaysConflicts` 断言 insert 恰好 2 次 |

---

## Step 3 — 可读性检查（产物 C）

> **预扫输出**（`scan-all-rules.sh src/main/java src/test/java`，摘要）：
> ```
> [P0] G16.2 — CatchWithoutLogging: TodoItemManager.java:62
> [P0] G16.2 — CatchWithoutLogging: AuditIntegrationImpl.java:91 / :107 / :111
> [P0] G16.2 — CatchWithoutLogging: TodoServiceImpl.java:123 / :138 / :259
> [P1] M016 — JavaTimeDefaultTimeZone: TodoServiceImpl.java:220
> [P2] A3.4 — LineWidthExceeded: GatewayAuthIntegrationImpl.java:74
> [P2] A3.4 — LineWidthExceeded: TodoValidatorTest.java:133
> === Summary: 10 findings (P0=7, P1=1, P2=2) | 52/222 rules scanned ===
> ```
> **复核结论**：7 条 `G16.2 CatchWithoutLogging` **均为误报**——脚本仅识别 `log.*` 变量名，本仓库统一使用 `LOGGER`，逐条人工复核均已在 catch 内输出日志（`TodoItemManager.java:64`、`AuditIntegrationImpl.java:93/109/113`、`TodoServiceImpl.java:128/136/139/261`）。不据此计 P0。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 文件名=顶层类名；UTF-8；无 Tab |
| A2 | 源文件结构/import 顺序 | ✅ | 无 `import *`；static import 独立分组（`TodoServiceImplTest.java:29-30`）；组内字典序 |
| A3 | 代码样式 | ⚠️ | A3.4 行宽 >120：`GatewayAuthIntegrationImpl.java:74`、`TodoValidatorTest.java:133`（P2-14）；其余 K&R、4 空格、成员间空行均符合 |
| A4 | 命名规范 | ✅ | 包名全小写；`TodoItemDO`/`TodoCreateVO` 后缀符合规约；常量 UPPER_SNAKE |
| A5 | 编码实践 | ✅ | 重写方法均有 `@Override`（`TodoServiceImpl.java:102/160`）；无空 catch；无 `finalize` |
| A6 | 特定元素样式 | ✅ | 无 C 风格数组；`switch` 含 `default`（`TodoExceptionHandler.java:184`）；`long` 字面量大写 `L`（`TodoConstants.java:76/136`） |
| A7 | Javadoc 规范 | ✅ | public 类/方法均有 Javadoc，`@param/@return/@throws` 顺序正确；getter 免注释 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`，120 条逐条核销）

| ID | 状态 | 备注（命中写 `path:line`） |
|----|------|---------------------------|
| B001 | ✅ | 无字面量调用必抛异常 parse/of |
| B002 | N/A | 无数组内容比较 |
| B003 | N/A | 无 `Arrays.fill` |
| B004 | N/A | 无数组 `toString` |
| B005 | N/A | 无 `Arrays.asList(基本类型数组)` |
| B006 | ✅ | 测试使用 AssertJ，无 `assertEquals` 参数序问题 |
| B007 | ✅ | 无 `catch(Throwable)` 吞断言（`TodoServiceImpl.java:259` 为非测试代码且记录 ERROR） |
| B008 | ✅ | 未使用 `Executors`；审计消费线程为裸 `Thread`（`AuditIntegrationImpl.java:60`） |
| B009 | N/A | 无移位运算 |
| B010 | N/A | 无 `BigDecimal(double)` |
| B011 | ✅ | 无包装类型 `==` 比较 |
| B012 | N/A | 无 Calendar |
| B013 | N/A | 无 Calendar |
| B014 | N/A | 无泛型不兼容集合查询 |
| B015 | N/A | 无 `toArray` |
| B016 | N/A | 无 `Comparable` 实现 |
| B017 | N/A | 无 `this == null` |
| B018 | N/A | 无三目数值类型提升 |
| B019 | N/A | 无 Money API |
| B020 | N/A | 无编译期常量乘法溢出 |
| B021 | N/A | 未使用 Jedis |
| B022 | ✅ | 使用 `DateTimeFormatter`（`TodoController.java:37`、`TodoNoGeneratorImpl.java:27`） |
| B023 | ✅ | 无「创建异常未抛出」；`TodoSourceEnum.java:59` 正确 throw |
| B024 | N/A | 审计线程已 `start()`（`AuditIntegrationImpl.java:62`） |
| B025 | N/A | 无双括号初始化 |
| B026 | ✅ | 判空使用 `== null` |
| B027 | N/A | 无自定义 `equals` |
| B028 | N/A | 未使用 commons `DateUtil` |
| B029 | ✅ | setter 赋值字段正确（`TodoItemDO`、`TodoCreateVO` 逐字段核对） |
| B030 | N/A | 无浮点比较 |
| B031 | N/A | 无 `String.format` |
| B032 | N/A | 无注解 `getClass()` |
| B033 | N/A | 无 Unsafe |
| B034 | N/A | 无 Hashtable |
| B035 | N/A | 无同一对象双侧二元运算 |
| B036 | N/A | 无 IdentityHashMap |
| B037 | N/A | 无可变参数条件表达式 |
| B038 | ✅ | 无无条件递归（`SnowflakeSequenceProvider.waitUntil` 为有界自旋） |
| B039 | N/A | 无 `indexOf` 双参误用 |
| B040 | N/A | 无 `isInstance` |
| B041 | N/A | 无裸 JDBC 连接 |
| B042 | N/A | 统一 JUnit5 |
| B043 | N/A | 无内部类 `@Test` |
| B044 | N/A | 未混用 JUnit3/4 |
| B045 | N/A | 无对包装类型加锁 |
| B046 | ✅ | 两处循环条件均会更新（`TodoServiceImpl.java:119`、`TodoRateLimiter.java:87`） |
| B047 | N/A | 无 `Float.compare` |
| B048 | N/A | 无 `Math.round` |
| B049 | ✅ | 日期格式 `yyyyMMdd`/`yyyy-MM-dd`（`TodoConstants.java:51/121`） |
| B050 | ✅ | 24 小时制 `HH:mm:ss`（`TodoConstants.java:121`） |
| B051 | N/A | 无 `Boolean.getBoolean` |
| B052 | ✅ | 使用 `yyyy` 而非 `YYYY` |
| B053 | ✅ | 期望异常用例使用 `assertThatThrownBy`，无需 `fail()` |
| B054 | N/A | 无 EqualsTester |
| B055 | N/A | 未使用 Mockito |
| B056 | N/A | 无 `Arrays.asList` 结构修改 |
| B057 | ✅ | 增强 for 内删除使用 `Iterator.remove`（`TodoRateLimiter.java:87-92`） |
| B058 | N/A | 无集合自传参 |
| B059 | N/A | 无 `Collections.nCopies` |
| B060 | N/A | 无含 null 分支的拆箱三目 |
| B061 | N/A | 无 `sun.misc` Base64 |
| B062 | N/A | 无 ClassLoader 强转 |
| B063 | N/A | 无 `javax.xml.bind` |
| B064 | N/A | 无 Optional 比较 |
| B065 | N/A | 无 Pojo 自赋值 |
| B066 | N/A | 无 `(int)Math.random()` |
| B067 | N/A | 无 `Random.nextInt()%` |
| B068 | N/A | 无自赋值（`OperatorContext`/`TodoCreateVO` setter 均带 `this.`） |
| B069 | N/A | 无 `compareTo` 自比较 |
| B070 | N/A | 无自定义 `equals` |
| B071 | ✅ | 使用 `isEmpty()` 判断空 |
| B072 | N/A | 无 Stream `toString` |
| B073 | N/A | `new StringBuilder(int)` 传入 int 容量（`TodoNoGeneratorImpl.java:84`），非 char |
| B074 | N/A | 无 `substring(0)` |
| B075 | ✅ | for 循环方向与条件一致（`TodoNoGeneratorImpl.java:85`） |
| B076 | ✅ | `@Transactional` 位于 public 方法（`TodoServiceImpl.java:104`） |
| B077 | ✅ | 测试未捕获 `Throwable` |
| B078 | N/A | 无 Truth 自比较 |
| B079 | N/A | 无 `@Mock` |
| B080 | ✅ | 全部测试含断言（`assertThat`/`assertThatThrownBy`） |
| B081 | N/A | 无排序集合丢弃 |
| M001 | ✅ | 无重复条件分支（`TodoExceptionHandler.java:170-186` 分支互斥） |
| M002 | N/A | 无父子类 `instanceof` |
| M003 | ✅ | 无包装类构造器 |
| M004 | ✅ | 无 `printStackTrace` |
| M005 | ✅ | 内部类均为 `static final`（`TodoServiceImplTest.java:388/451/469`、`TodoControllerTest.java:116`） |
| M006 | N/A | 无恒真布尔表达式 |
| M007 | ✅ | 无空 catch |
| M008 | N/A | 无 `equals` 重写 |
| M009 | N/A | 无不兼容类型 `equals` |
| M010 | N/A | 无位运算（雪花掩码为运行期变量拼接，`SnowflakeSequenceProvider.java:31`） |
| M011 | ✅ | `switch` 各 case 均 `return`，无 fall-through |
| M012 | ✅ | finally 中无 return/throw |
| M013 | N/A | 无浮点强转 |
| M014 | N/A | 无枚举 `getClass()` |
| M015 | N/A | 无继承字段隐藏 |
| M016 | ⚠️ | `LocalDateTime.now()`（`TodoServiceImpl.java:220`）、`LocalDate.now()`（`TodoNoGeneratorImpl.java:47`）依赖 JVM 默认时区（P2-7） |
| M017 | ✅ | 测试方法均有 `@Test` |
| M018 | N/A | 无显式 Lock |
| M019 | ✅ | 枚举 switch 带 `default` |
| M020 | ✅ | 重写方法均有 `@Override` |
| M021 | N/A | 无自定义 `equals` |
| M022 | N/A | 无 `Optional.of(null)` |
| M023 | N/A | 无未重写 `toString` 的输出（DO/DTO 均已重写并脱敏） |
| M024 | N/A | 无 Optional `get()` |
| M025 | N/A | 无 final 类 protected 成员 |
| M026 | N/A | 无 `@Mock` |
| M027 | N/A | 无 ThreadLocal |
| I001 | ✅ | 异常用例断言错误码（`TodoServiceImplTest.java:186-189` 等） |
| I002 | N/A | 无 `@DoNotMock` |
| I003 | N/A | 无 `@AutoValue` |
| I004 | N/A | 未使用 `java.util.Date` |
| I005 | N/A | 无 JUnit3 |
| I006 | N/A | 无 `setUp()` 命名依赖（使用 `@BeforeEach`） |
| I007 | N/A | 无 `tearDown()` |
| I008 | N/A | 无 dataProvider |
| I009 | N/A | 统计类规则，不适用 |
| I010 | N/A | 无 Spring 容器测试（沿用现有纯单测风格） |

### 4.2 可靠性（`reliability-checklist.md`，G1–G18 逐条核销）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | ✅ | 创建为独立行插入，无「先读后写」共享行竞争（design §5.1.3.1 并发说明）；幂等由唯一索引兜底 |
| G1.2 | ⚠️ | 编号冲突重试未基于最新状态二次校验，但重试即重新取号（`TodoServiceImpl.java:120,137`），不构成丢更新 |
| G1.3 | N/A | 无乐观锁 |
| G1.4 | N/A | 单资源写入，无多锁顺序 |
| G2.1 | ✅ | 幂等键 `requestId` + `uk_todo_item_req`（`todo_item.sql:22`） |
| G2.2 | ✅ | 重试沿用同一 `requestId`（design U01/异常场景表） |
| G2.3 | ✅ | 幂等键字段名/粒度与系分 W01 一致 |
| G3.1 | N/A | 单库本地事务 |
| G3.2 | ⚠️ | 审计投递位于 `@Transactional` 方法内（`TodoServiceImpl.java:143`），事务回滚时仍会发出成功审计（P1-2） |
| G4.1 | ✅ | SQL 无业务分支（`TodoItemMapper.xml:32-51`） |
| G4.2 | ✅ | 查询条件 `tenant_id`/`request_id` 均等值、无函数包裹，命中 `uk_todo_item_req` |
| G4.3 | ✅ | 单条 `LIMIT 1`，无大列表查询 |
| G5.1 | N/A | 无 MQ |
| G6.1 | ⚠️ | 无数据缓存（本期不引入，design §3）；但限流器本地 Map 无过期淘汰（P2-8） |
| G6.2 | N/A | 无缓存双写 |
| G7.1 | N/A | 无定时任务 |
| G7.2 | N/A | 无定时任务 |
| G8.1 | ⚠️ | 审计 `catch(Throwable)` 有 ERROR 日志 ✅；关闭时队列丢弃未告警（P2-12） |
| G8.2 | ✅ | 审计为弱依赖、鉴权为强依赖，边界与 design §6.1 一致 |
| G8.3 | N/A | 无 I/O 流/连接/锁资源 |
| G8.4 | ✅ | `@PreDestroy shutdown()`（`AuditIntegrationImpl.java:85-95`） |
| G8.5 | N/A | 无 ThreadLocal |
| G8.6 | N/A | 未使用 `Executors` 线程池 |
| G9.1 | ✅ | 本服务无出站 RPC/HTTP；鉴权仅读网关头 |
| G9.2 | ✅ | DB 超时：Hikari `connection-timeout=1000`（`application.yml:15`）、`default-statement-timeout: 1`（`:22`） |
| G9.3 | ✅ | 失败重试沿用同一 `requestId`，天然幂等 |
| G10.1 | ⚠️ | 未知异常统一映射为 TODO_0008「创建失败」，与「存储异常」语义混用（P2-11） |
| G10.2 | ✅ | 全新增接口，无存量契约变更 |
| G11.1 | ✅ | 6 个测试类、48 个 `@Test`（含参数化展开 84 用例） |
| G11.2 | ⚠️ | 缺跨租户隔离、`queryByRequestId` 租户隔离、`/create` 缺 `requestId` 的错误码用例（P2-15） |
| G11.3 | ✅ | 入参 null/空白/超长均有防御（`TodoValidator.java:36-92`） |
| G11.4 | N/A | 无金额/数值精度运算 |
| G12.1 | N/A | 无资金场景 |
| G12.2 | ❌ | 无功能开关止血手段（P1-6） |
| G13.1 | ⚠️ | 日志级别基本正确；参数校验失败走 `@Valid` 返回 400 且 WARN ✅ |
| G14.1 | N/A | 无金额 |
| G14.2 | ✅ | 查询强制带 `tenant_id`（`TodoServiceImpl.java:175-180`、`TodoItemMapper.xml:36`）；租户取自网关头 |
| G14.3 | ⚠️ | 使用 `LocalDateTime.now()` 且未约定时区（P2-7） |
| G14.4 | N/A | 无格式化解析外部时间串 |
| G15.1 | ✅ | 全新增表，字段均有默认值（`todo_item.sql`），向前兼容 |
| G15.2 | N/A | 无旧接口 |
| G15.3 | ❌ | 无灰度开关/白名单（P1-6） |
| G16.1 | ❌ | 无成功率/耗时/调用量指标埋点，design §6.5 告警无法落地（P1-5） |
| G16.2 | ✅ | 日志含 `requestId`/`todoNo`/`creatorId`；预扫 7 条 CatchWithoutLogging 复核为误报 |
| G16.3 | ✅ | 业务异常 WARN（`TodoExceptionHandler.java:58`）、系统异常 ERROR（`:125`）、成功 INFO |
| G16.4 | ✅ | 无空 catch、无 `printStackTrace` |
| G17.1 | ❌ | 无紧急关闭开关（P1-6） |
| G17.2 | ⚠️ | 审计/取号有降级；限流与创建主链路无兜底开关 |
| G17.3 | ✅ | 全新增表，回滚无需数据脚本（design §7.3） |
| G18.1 | N/A | 模板未定义该项，按未启用处理 |
| G18.2 | N/A | 同上 |
| G18.3 | N/A | 同上 |

### 4.3 安全（`security-checklist.md`，S1–S10 逐条核销）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | 全部 `#{}` 占位符，无 `${}`（`TodoItemMapper.xml:32-51`） |
| S1.2 | ✅ | 无动态 order by/表名 |
| S1.3 | N/A | 无 like/in 查询 |
| S2.1 | ✅ | 纯 JSON 响应，无 HTML 渲染；描述按纯文本存储，展示端转义（design §5.1.2 业务规则） |
| S2.2 | N/A | 无富文本 |
| S2.3 | N/A | 无模板引擎 |
| S3.1 | N/A | 无出站 URL 请求 |
| S3.2 | N/A | 无跳转 |
| S3.3 | N/A | 无出站请求 |
| S4.1 | N/A | 无命令拼接 |
| S4.2 | N/A | 无文件操作 |
| S5.1 | N/A | 无 XML 入参解析 |
| S5.2 | N/A | 无 XPath |
| S6.1 | N/A | 无 Java 原生反序列化 |
| S6.2 | ✅ | Jackson 默认绑定，未启用多态 `@JsonTypeInfo` |
| S6.3 | N/A | 无敏感凭证字段 |
| S7.1 | N/A | 无文件上传 |
| S7.2 | N/A | 无路径拼接 |
| S7.3 | N/A | 无文件存储 |
| S8.1 | ⚠️ | 接口以网关注入头作身份与权限判定，直接可达时可伪造 `X-User-Id`/`X-User-Roles`；R06 的权限平台校验未落地（P1-1） |
| S8.2 | ✅ | 仅 POST 创建，无 GET 改数据 |
| S8.3 | ✅ | 主键自增但对外以 `todoNo` 展示；`requestId` 由前端 UUID 提供 |
| S8.4 | N/A | 无 Cookie 操作 |
| S9.1 | ✅ | 数据库口令占位 `${TODO_DB_PASSWORD:}`，无硬编码（`application.yml:11`） |
| S9.2 | ✅ | 名称/描述不落日志原文，DO/Result/Request `toString` 隐藏或脱敏（`DesensitizeUtil.java:58-66`） |
| S9.3 | ✅ | JDBC `useSSL=true`（`application.yml:9`）；无自研加密 |
| S9.4 | N/A | 无随机数生成 |
| S10.1 | N/A | 内部 oneapi 网关统一鉴权，非浏览器表单直连；记 N/A 并保留为后续评审项 |
| S10.2 | N/A | 无 CORS 配置 |
| S10.3 | N/A | 无跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | N/A | 示例项；实际已满足——Controller 使用 `@Valid`（`TodoController.java:69`） |
| U1.2 | N/A(未启用自定义规则) | — |
| U1.3 | N/A(未启用自定义规则) | — |
| U2.1 | N/A(未启用自定义规则) | — |
| U2.2 | N/A(未启用自定义规则) | — |
| U2.3 | N/A(未启用自定义规则) | — |

---

## 终检（防漏检）

- [x] 执行队列 39 个 `.java` 文件 Step2 / Step3 / G / S / B-M-I 列均非 `⬜`；
- [x] Step 2 的 REQ-1~REQ-10 均非 `⬜`；
- [x] Step 3 的 A1–A7 均非 `⬜`；
- [x] Step 4 的 B001–B081 / M001–M027 / I001–I010 / G1.1–G18.3 / S1.1–S10.3 均非 `⬜`（`N/A` 已写原因）；
- [x] Step 5 的 U* 均非 `⬜`；
- [x] 所有 `❌/⚠️` 已写入 `cr_report.md`，包含 `ID + path:line`。
