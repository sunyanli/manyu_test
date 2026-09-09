# Code Review Report

> **Change** `todo-app 新增待办事项` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-fcf18725-2647-45ed-948b-cf0e3be16000` / `565e2ca` · **日期** `2026-09-09` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。**须先**运行 `scan-all-rules.sh` 并将要点并入 §5，**再**写 LLM 结论。问题须含 `path:line` 或清单 ID：可读性 `A3.4`，安全 `S1.1`，可靠性 `G16.2`，Bug 模式 `B012` / `M005` 等。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `12` |
| 变更行数 | `+1486 / -0` |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `TodoController` | `src/main/java/com/alipay/todo/controller/TodoController.java` | oneapi 控制器，POST /api/todo/create |
| `TodoService` | `src/main/java/com/alipay/todo/service/TodoService.java` | 业务服务接口 |
| `TodoServiceImpl` | `src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java` | 业务逻辑实现：校验→组装→持久化 |
| `TodoCreateRequest` | `src/main/java/com/alipay/todo/model/dto/TodoCreateRequest.java` | 创建请求 DTO，含 @Valid 校验 |
| `TodoItemDTO` | `src/main/java/com/alipay/todo/model/dto/TodoItemDTO.java` | 响应 DTO |
| `TodoItemDO` | `src/main/java/com/alipay/todo/model/entity/TodoItemDO.java` | 数据对象，对应 todo_item 表 |
| `TodoItemMapper` | `src/main/java/com/alipay/todo/dao/mapper/TodoItemMapper.java` | MyBatis 映射接口 |
| `TodoItemMapper.xml` | `src/main/resources/mapper/TodoItemMapper.xml` | MyBatis SQL 映射（非 Java，跳过 Step 3） |
| `TodoStatusEnum` | `src/main/java/com/alipay/todo/common/enums/TodoStatusEnum.java` | 状态枚举 INIT/DONE |
| `TodoConstants` | `src/main/java/com/alipay/todo/common/constant/TodoConstants.java` | 常量定义 |
| `TodoException` | `src/main/java/com/alipay/todo/common/exception/TodoException.java` | 自定义业务异常 |
| `SecurityContextHolder` | `src/main/java/com/alipay/todo/common/constant/SecurityContextHolder.java` | 登录上下文持有者（ThreadLocal） |
| `TodoServiceImplTest` | `src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java` | 单元测试（10 个测试方法） |
| `pom.xml` | `pom.xml` | Maven 项目配置（非 Java，跳过 Step 3） |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 2 | 4 | 1 |

---

## 3. Step 2 — 功能（REQ）

### REQ-1: 新增待办事项接口 — POST /api/todo/create

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `W01 新增待办事项 POST /api/todo/create` | ✅ | design.md §4.1 W01: POST /api/todo/create | `TodoController.java:42` `@PostMapping("/create")` | 接口路径一致 |
| `响应结构 {code, msg, data}` | ✅ | design.md §1 约束：接口统一返回 {code, msg, data} | `TodoController.java:60-64` 构建 response Map | 结构正确 |
| `成功 code=OK` | ✅ | design.md §5 全局约定：成功 code=OK | `TodoController.java:61` `response.put("code", "OK")` | 一致 |

### REQ-2: 参数校验 — name 必填 ≤64 字符

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `R01 name 去除首尾空白后非空且 ≤64` | ✅ | design.md §5.1.3.1 R01 | `TodoServiceImpl.java:61-67` 校验逻辑 | 校验正确 |
| `R01 失败返回 TODO_001` | ✅ | design.md §5.1.2 错误码表 | `TodoServiceImpl.java:63,66` `throw new TodoException("TODO_001", ...)` | 错误码正确 |
| `@Valid 注解校验` | ✅ | impl.md L1 静态检查 | `TodoCreateRequest.java:17-18` `@NotBlank + @Size(max=64)` | 声明式校验正确 |

### REQ-3: 参数校验 — description 选填 ≤512 字符

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `R02 description ≤512，缺省按空串处理` | ✅ | design.md §5.1.3.1 R02 | `TodoServiceImpl.java:68-70` 校验；`TodoServiceImpl.java:77` 缺省空串 | 校验正确 |
| `R02 失败返回 TODO_001` | ✅ | design.md §5.1.2 错误码表 | `TodoServiceImpl.java:69` `throw new TodoException("TODO_001", ...)` | 错误码正确 |
| `@Size(max=512) 校验` | ✅ | design.md §5.1.1 字段约束 | `TodoCreateRequest.java:22` `@Size(max=512)` | 一致 |

### REQ-4: 登录态校验 — creator 取登录上下文

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `R03 必须处于登录态，creator 取登录人工号` | ✅ | design.md §5.1.3.1 R03 | `TodoServiceImpl.java:86-94` `getCurrentCreator()` | 从 SecurityContextHolder 获取 |
| `R03 失败返回 TODO_002` | ✅ | design.md §5.1.2 错误码表 | `TodoServiceImpl.java:91` `throw new TodoException("TODO_002", "请先登录")` | 错误码正确 |
| `creator 不接受前端传参` | ✅ | design.md §6.4.2.1 | `TodoServiceImpl.java:79` `todoItem.setCreator(getCurrentCreator())` | 强制从上下文获取 |

### REQ-5: 创建后状态固定 INIT

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `R04 创建后 status 固定 INIT` | ✅ | design.md §5.1.3.1 R04 | `TodoServiceImpl.java:78` `todoItem.setStatus(TodoStatusEnum.INIT.getCode())` | 状态正确 |
| `is_deleted=0` | ✅ | design.md §5.1.3.1 R04 | `TodoServiceImpl.java:80` `todoItem.setIsDeleted(0)` | 一致 |

### REQ-6: 数据持久化 — todo_item 表

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `表字段完整` | ✅ | design.md §5.1.1.1 表结构 | `TodoItemDO.java:19-27` 全部字段；`TodoItemMapper.xml:21-22` INSERT 字段 | 字段一一对应 |
| `主键自增回填` | ✅ | design.md §5.1.1.1 id PK 自增 | `TodoItemMapper.xml:20` `useGeneratedKeys="true" keyProperty="id"` | 正确 |
| `INSERT 成功返回 affected=1` | ✅ | design.md §5.1.3.1 时序图 | `TodoServiceImpl.java:45-46` `if (affected != 1)` 校验 | 写入结果校验 |

### REQ-7: 系统异常 — 数据库写入失败返回 TODO_003

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `DB 写入失败返回 TODO_003` | ✅ | design.md §5.1.2 错误码表 | `TodoServiceImpl.java:48` `throw new TodoException("TODO_003", ...)`；`TodoController.java:75` | 错误码正确 |
| `事务回滚 @Transactional` | ✅ | design.md §5.1.3.1 异常场景 | `TodoServiceImpl.java:36` `@Transactional(rollbackFor = Exception.class)` | 事务注解正确 |

### REQ-8: 单元测试覆盖

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `10 个测试方法覆盖正常/异常/边界` | ✅ | impl.md §2 TEST 阶段 | `TodoServiceImplTest.java` 10 个 @Test 方法 | 覆盖正常路径、参数校验、登录态、系统异常 |
| `正常路径断言` | ✅ | design.md §5.1.3.1 | `TodoServiceImplTest.java:82-91` assertThat 断言 | 断言完整 |
| `异常路径断言` | ✅ | design.md §5.1.2 错误码 | `TodoServiceImplTest.java:189-194,207-212,225-230,244-249,270-275,297-302` 断言错误码和消息 | 断言完整 |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明 |
|------|------|
| ✅ | A1.1 文件名 = 顶层类名：所有 12 个 .java 文件名与类名一致 |
| ✅ | A1.2 编码 UTF-8：所有文件均为 UTF-8 |
| ✅ | A1.3 无 Tab：所有文件使用空格缩进 |
| ✅ | A2.1 文件结构：package → import → 类，各部分空行分隔 |
| ✅ | A2.2 无 `import *`：所有 import 均为显式导入 |
| ✅ | A2.3 import 分组：静态 import 与非静态 import 分组正确（测试类） |
| ✅ | A2.4 import 排序：按 ASCII 字典序排列 |
| ✅ | A3.1 K&R 大括号风格：左括号不换行，右括号换行 |
| ✅ | A3.3 缩进 4 空格 |
| ✅ | A3.4 行宽 ≤120：所有行均未超过 120 字符 |
| ✅ | A3.6 类成员间空行 |
| ✅ | A4.1 包名全小写：`com.alipay.todo.*` |
| ✅ | A4.2 类名 UpperCamelCase：TodoController、TodoServiceImpl 等 |
| ✅ | A4.3 方法名 lowerCamelCase：create、insert、selectById 等 |
| ✅ | A4.4 常量 UPPER_SNAKE_CASE：NAME_MAX_LEN、DESC_MAX_LEN、DEFAULT_TENANT_ID |
| ✅ | A4.5 变量 lowerCamelCase |
| ✅ | A4.7 测试类命名：TodoServiceImplTest |
| ✅ | A5.1 @Override：TodoServiceImpl.create() 有 @Override |
| ✅ | A5.2 catch 块非空：Controller 中 catch 均有日志输出 |
| ✅ | A7.1 public 类有 Javadoc：所有类均有 Javadoc 注释 |

**可读性问题：**

- **P2** `A2.4` `TodoServiceImpl.java:14-15` — `import org.springframework.transaction.annotation.Transactional` 重复导入两次。虽不影响编译，但属于冗余代码，建议删除一行。

---

## 5. Step 4 — 可靠性检查

### 5.1 自动化预扫结果（scan-all-rules.sh）

```
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/alipay/todo/controller/TodoController.java:65
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/alipay/todo/controller/TodoController.java:72
[P1] M016 — JavaTimeDefaultTimeZone: src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:81
[P1] M016 — JavaTimeDefaultTimeZone: src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:82
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:73
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:74
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:107
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:108
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:133
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:134
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:161
[P1] M016 — JavaTimeDefaultTimeZone: src/test/java/com/alipay/todo/service/impl/TodoServiceImplTest.java:162
```

**人工复核结论：**

- **G16.2 TodoController.java:65** — 复核后降级为 **P1**。原因：`catch (TodoException e)` 中有 `logger.error(...)` 记录了 errorCode 和 message，但未记录异常堆栈（缺少第三个参数 `e`）。对于业务异常（TODO_001/TODO_002），不记录堆栈可接受；但此处是统一 catch，建议补充堆栈以便排障。
- **G16.2 TodoController.java:72** — 复核后维持 **P0**。原因：`catch (Exception e)` 中有 `logger.error("create todo system error", e)`，堆栈已记录。但此处捕获了所有异常（包括 TodoException 的子类 RuntimeException），虽然 TodoException 已在前面 catch，但 `Exception` 范围过广，建议明确捕获非业务异常。实际上代码逻辑正确（TodoException 在前面先捕获），脚本误报。**降级为 N/A（误报）**。
- **M016 TodoServiceImpl.java:81-82** — 维持 **P1**。`LocalDateTime.now()` 依赖系统默认时区，在跨时区部署或容器时区不一致时可能导致时间偏差。建议使用 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))` 或统一使用 UTC。
- **M016 TodoServiceImplTest.java 多处** — 维持 **P1**。测试代码中 `LocalDateTime.now()` 同样存在时区问题，但测试场景影响较低，建议统一修复。

### 5.2 可靠性（军规 G1–G17）

| ID | 检查项 | 结果 | 等级 | 说明 |
|----|--------|------|------|------|
| G1.1 | 事务内先读后写无锁 | ✅ N/A | — | 纯 INSERT 操作，无读改写 |
| G1.2 | 已加锁未二次校验 | ✅ N/A | — | 无锁场景 |
| G1.3 | 高冲突乐观锁无限重试 | ✅ N/A | — | 无乐观锁 |
| G1.4 | 多资源加锁顺序不一致 | ✅ N/A | — | 无多资源加锁 |
| G2.1 | 写接口无幂等键 | ⚠️ | P2 | 纯 INSERT 允许同名待办，设计上不做幂等去重（design.md §5.1.3.1 已说明），但建议在接口文档中明确 |
| G2.2 | 重试未防重复落库 | ✅ N/A | — | 无重试机制 |
| G3.1 | 分布式事务当本地强一致 | ✅ N/A | — | 单库单表 |
| G3.2 | @Transactional 范围过大 | ✅ | — | 事务范围仅含 INSERT，无外部 I/O |
| G4.1 | 复杂业务分支堆在 SQL | ✅ | — | SQL 简单 INSERT |
| G4.2 | WHERE 对索引列函数转换 | ✅ N/A | — | 无 WHERE 查询（本期仅 INSERT） |
| G4.3 | 无分页大列表查询 | ✅ N/A | — | 本期无查询 |
| G5.1 | MQ 消费未幂等 | ✅ N/A | — | 无 MQ |
| G6.1 | 缓存无超时 | ✅ N/A | — | 无缓存 |
| G6.2 | 缓存与 DB 双写无策略 | ✅ N/A | — | 无缓存 |
| G7.1 | 调度任务多实例重复 | ✅ N/A | — | 无调度任务 |
| G8.1 | catch 吞异常或只打 log | ❌ | P1 | `TodoController.java:65` catch TodoException 未记录堆栈 |
| G8.2 | 核心链路强依赖非核心 | ✅ N/A | — | 仅依赖 MySQL |
| G8.3 | I/O 流未释放 | ✅ N/A | — | 无 I/O 流操作 |
| G8.4 | 线程池未 shutdown | ✅ N/A | — | 无线程池 |
| G8.5 | ThreadLocal 未 remove | ❌ | P0 | `SecurityContextHolder.java:11` ThreadLocal 无 `remove()` 在请求结束时调用；虽然提供了 `clear()` 方法，但未见在 Filter/Interceptor 中调用 |
| G8.6 | 默认无界队列线程池 | ✅ N/A | — | 无线程池 |
| G9.1 | 外部调用未区分三态 | ✅ N/A | — | 无外部调用 |
| G9.2 | 外部调用未设置超时 | ✅ N/A | — | 无外部调用 |
| G9.3 | 重试前未查询最新状态 | ✅ N/A | — | 无重试 |
| G10.1 | 同一字段 null 表无数据和异常 | ✅ | — | 响应结构 data 字段在异常时为 null，code/msg 已区分 |
| G11.1 | 新逻辑无单测 | ✅ | — | 10 个测试方法覆盖 |
| G11.2 | 未覆盖边界 | ✅ | — | 覆盖空、最大值 |
| G11.3 | 入参空值无防御 | ✅ | — | `TodoServiceImpl.java:58-59` null 校验 |
| G11.4 | 数值运算溢出/精度丢失 | ✅ N/A | — | 无数值运算 |
| G12.1 | 资金场景无幂等 | ✅ N/A | — | 非资金场景 |
| G13.1 | 错误打 info、成功打 error | ✅ | — | 日志级别正确：成功 INFO，失败 ERROR |
| G14.1 | 金额用 double | ✅ N/A | — | 无金额 |
| G14.2 | 多租户查询无租户条件 | ✅ N/A | — | 本期无查询；INSERT 已含 tenant_id |
| G14.3 | 存本地时区字符串 | ❌ | P1 | `TodoServiceImpl.java:81-82` `LocalDateTime.now()` 依赖系统时区 |
| G15.1 | 表结构变更向前兼容 | ✅ N/A | — | 全新表 |
| G15.2 | 新旧接口共存 | ✅ N/A | — | 全新接口 |
| G15.3 | 不兼容逻辑无开关 | ✅ | — | `TodoConstants.java:13` 预留 `TODO_CREATE_ENABLED` |
| G16.1 | 核心链路无关键指标埋点 | ⚠️ | P2 | 仅有日志，无 metrics 埋点（QPS、耗时）；design.md §6.5 提到监控点但未实现 |
| G16.2 | 异常路径无可追溯上下文 | ⚠️ | P2 | 日志缺少 traceId/requestId |
| G16.3 | 日志级别正确 | ✅ | — | 业务异常 WARN/ERROR，系统异常 ERROR |
| G16.4 | 空 catch | ✅ | — | 无空 catch |
| G17.1 | 功能开关支持紧急关闭 | ✅ | — | `TodoConstants.java:13` 预留开关常量 |
| G17.2 | 降级预案 | ✅ N/A | — | 无降级需求（单表 INSERT） |
| G17.3 | 数据变更有回滚脚本 | ✅ N/A | — | 全新表，回滚保留表即可 |

### 5.3 安全（S1–S10）

| ID | 检查项 | 结果 | 等级 | 说明 |
|----|--------|------|------|------|
| S1.1 | SQL 参数化 #{} | ✅ | — | `TodoItemMapper.xml:22` 全部使用 `#{}` |
| S1.2 | order by/group by 白名单 | ✅ N/A | — | 无动态排序 |
| S1.3 | like/in 参数化 | ✅ N/A | — | 无 like/in 查询 |
| S2.1 | XSS 输出编码 | ✅ N/A | — | 纯后端 API，无页面渲染 |
| S2.2 | 富文本过滤 | ✅ N/A | — | 无富文本 |
| S2.3 | 模板引擎自动转义 | ✅ N/A | — | 无模板引擎 |
| S3.1 | SSRF 域名白名单 | ✅ N/A | — | 无外部 URL 请求 |
| S3.2 | 302 跳转校验 | ✅ N/A | — | 无跳转 |
| S3.3 | 超时设置 | ✅ N/A | — | 无外部调用 |
| S4.1 | 外部参数拼接系统命令 | ✅ N/A | — | 无系统命令调用 |
| S4.2 | 文件操作调用外部命令 | ✅ N/A | — | 无文件操作 |
| S5.1 | XML 解析器禁用外部实体 | ✅ N/A | — | MyBatis XML 为配置文件，非用户输入解析 |
| S6.1 | 反序列化数据来源可信 | ✅ | — | 仅内部接口，Spring Boot 默认 Jackson 配置 |
| S6.2 | JSON 反序列化禁用多态 | ✅ N/A | — | 无多态反序列化 |
| S6.3 | 敏感字段 transient | ✅ N/A | — | 无敏感字段 |
| S7.1 | 文件上传白名单 | ✅ N/A | — | 无文件上传 |
| S7.2 | 路径过滤 ../ | ✅ N/A | — | 无文件路径操作 |
| S7.3 | 文件重命名存储 | ✅ N/A | — | 无文件存储 |
| S8.1 | 接口已接入鉴权 | ✅ | — | `SecurityContextHolder` 强制登录态校验；`TodoServiceImpl.java:89-91` 未登录抛 TODO_002 |
| S8.2 | 禁止 GET 执行增删改 | ✅ | — | `TodoController.java:42` 使用 POST |
| S8.3 | 数据 ID 不可预测 | ✅ N/A | — | 使用数据库自增 ID，内部工具可接受 |
| S8.4 | Cookie HttpOnly + Secure | ✅ N/A | — | 不涉及 Cookie 操作 |
| S9.1 | 密钥不硬编码 | ✅ | — | 无密钥 |
| S9.2 | 日志不记录敏感信息 | ✅ | — | 日志仅记录 id、creator、errorCode，不打印 description 全文 |
| S9.3 | 传输加密 HTTPS | ✅ N/A | — | 部署层 Nginx/SLB 负责 |
| S9.4 | 随机数 SecureRandom | ✅ N/A | — | 无随机数使用 |
| S10.1 | CSRF Token 防护 | ✅ N/A | — | 纯 API 接口，无表单提交 |
| S10.2 | CORS 白名单 | ✅ N/A | — | 未配置 CORS，默认同源 |
| S10.3 | URL 跳转白名单 | ✅ N/A | — | 无 URL 跳转 |

### 5.4 Bug 模式（B/M/I）

| ID | 规则名 | 结果 | 等级 | 说明 |
|----|--------|------|------|------|
| B006 | AssertEqualsArgumentOrderChecker | ✅ | — | 测试使用 AssertJ `assertThat(actual).isEqualTo(expected)`，参数顺序正确 |
| B011 | BoxedPrimitiveEquality | ✅ N/A | — | 无包装类型 == 比较 |
| B025 | DoubleBraceInitialization | ✅ N/A | — | 无双括号初始化 |
| B026 | EqualsNull | ✅ N/A | — | 无 equals(null) |
| B030 | FloatValueEquality | ✅ N/A | — | 无浮点比较 |
| B046 | LoopConditionChecker | ✅ N/A | — | 无循环 |
| B055 | MockitoUsage | ✅ | — | `when(...).thenReturn(...)` 使用正确；`verify(mock, times(1)).method(...)` 正确 |
| B060 | NullTernary | ✅ N/A | — | 无自动拆箱三目 |
| B068 | SelfAssignment | ✅ N/A | — | 无自赋值 |
| B076 | TransactionalNonPublicMethod | ✅ | — | `TodoServiceImpl.java:36` @Transactional 在 public 方法上 |
| B077 | TryFailThrowable | ✅ N/A | — | 测试未捕获 Throwable |
| B080 | UnitCaseNoAssertionsCheck | ✅ | — | 所有测试方法均有断言 |
| M004 | CatchAndPrintStackTrace | ✅ | — | 无 printStackTrace() |
| M007 | EmptyCatch | ✅ | — | 无空 catch |
| M012 | Finally | ✅ N/A | — | 无 finally 块 |
| M016 | JavaTimeDefaultTimeZone | ❌ | P1 | `TodoServiceImpl.java:81-82` `LocalDateTime.now()` 依赖系统默认时区 |
| M020 | MissingOverride | ✅ | — | `TodoServiceImpl.java:35` 有 @Override |
| M026 | StaticMockMember | ✅ | — | @Mock 未声明 static |
| M027 | ThreadLocalUsage | ✅ | — | `SecurityContextHolder.java:11` ThreadLocal 声明为 static final |
| I001 | AssertExceptionDetailInfoPreferred | ✅ | — | 测试断言了错误码和消息内容 |
| I004 | JavaUtilDate | ✅ | — | 使用 `LocalDateTime` 而非 `Date` |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | N/A | — | 未启用自定义规则 |

---

## 7. 结论

- **合并建议**：修复后合并
- **P0**：
  1. **G8.5** `SecurityContextHolder.java:11` — ThreadLocal 未在请求结束时 `remove()`，存在内存泄漏风险。虽然提供了 `clear()` 方法，但未见在 Filter/Interceptor/Controller 中调用。建议在登录拦截器或 `TodoController` 的 `finally` 块中调用 `SecurityContextHolder.clear()`。
  2. **G14.3** `TodoServiceImpl.java:81-82` — `LocalDateTime.now()` 依赖系统默认时区，跨时区部署或容器时区不一致时导致时间偏差。建议使用 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))` 或统一使用 UTC。
- **P1**：
  1. **G8.1** `TodoController.java:65` — catch TodoException 未记录异常堆栈（缺少第三个参数 `e`），建议补充为 `logger.error("...", e.getErrorCode(), e.getMessage(), e)`。
  2. **M016** `TodoServiceImpl.java:81-82` — 同 G14.3，`LocalDateTime.now()` 时区问题。
  3. **A2.4** `TodoServiceImpl.java:14-15` — `import org.springframework.transaction.annotation.Transactional` 重复导入两次。
- **P2**：
  1. **G16.1/G16.2** — 缺少 metrics 埋点和 traceId，建议后续接入监控体系。
  2. **G2.1** — 设计上允许同名待办（无幂等去重），建议在接口文档中明确说明。

- **一句话**：整体代码质量良好，功能实现与设计文档完全一致，分层清晰，测试覆盖充分；存在 ThreadLocal 内存泄漏风险和时区依赖问题，修复后可合并。

---

## 7.1 问题片段

### P0 — G8.5 — ThreadLocal 未 remove

**`SecurityContextHolder.java:11`** — ThreadLocal 声明正确，但缺少请求结束时的清理机制。

片段范围：`src/main/java/com/alipay/todo/common/constant/SecurityContextHolder.java:9-27`

```java
L9 |public final class SecurityContextHolder {
L10|
L11|    private static final ThreadLocal<String> CREATOR_HOLDER = new ThreadLocal<>();
L12|
L13|    public static void setCreator(String creator) {
L14|        CREATOR_HOLDER.set(creator);
L15|    }
L16|
L17|    public static String getCreator() {
L18|        return CREATOR_HOLDER.get();
L19|    }
L20|
L21|    public static void clear() {
L22|        CREATOR_HOLDER.remove();
L23|    }
L24|
L25|    private SecurityContextHolder() {
L26|        throw new IllegalStateException("Utility class");
L27|    }
L28|}
```

**问题**：`clear()` 方法存在但无人调用。在 Tomcat 线程池场景下，ThreadLocal 不 remove 会导致线程复用时数据残留，造成内存泄漏和脏数据。建议在登录拦截器的 `afterCompletion` 或 Controller 的 `finally` 块中调用 `SecurityContextHolder.clear()`。

### P0/P1 — G14.3/M016 — LocalDateTime.now() 时区依赖

**`TodoServiceImpl.java:81-82`** — 使用系统默认时区获取当前时间。

片段范围：`src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:73-84`

```java
L73|    private TodoItemDO buildEntity(TodoCreateRequest request) {
L74|        TodoItemDO todoItem = new TodoItemDO();
L75|        todoItem.setTenantId(TodoConstants.DEFAULT_TENANT_ID);
L76|        todoItem.setName(request.getName().trim());
L77|        todoItem.setDescription(request.getDescription() != null ? request.getDescription() : "");
L78|        todoItem.setStatus(TodoStatusEnum.INIT.getCode());
L79|        todoItem.setCreator(getCurrentCreator());
L80|        todoItem.setIsDeleted(0);
L81|        todoItem.setGmtCreate(LocalDateTime.now());
L82|        todoItem.setGmtModified(LocalDateTime.now());
L83|        return todoItem;
L84|    }
```

**问题**：`LocalDateTime.now()` 依赖 JVM 系统默认时区（`ZoneId.systemDefault()`）。在容器化部署或跨时区场景下，容器时区可能与业务时区不一致，导致 gmtCreate/gmtModified 时间偏差。建议显式指定时区：`LocalDateTime.now(ZoneId.of("Asia/Shanghai"))`，或统一使用 UTC 并在展示层转换。

### P1 — G8.1 — catch 未记录异常堆栈

**`TodoController.java:65-71`** — catch TodoException 未记录堆栈。

片段范围：`src/main/java/com/alipay/todo/controller/TodoController.java:65-71`

```java
L65|        } catch (TodoException e) {
L66|            logger.error("create todo error, errorCode: {}, message: {}", e.getErrorCode(), e.getMessage());
L67|            Map<String, Object> response = new HashMap<>();
L68|            response.put("code", e.getErrorCode());
L69|            response.put("msg", e.getMessage());
L70|            response.put("data", null);
L71|            return ResponseEntity.badRequest().body(response);
```

**问题**：`logger.error` 未传入异常对象 `e` 作为最后一个参数，导致异常堆栈不会输出到日志。对于业务异常（TODO_001/TODO_002）影响较小，但对于 TODO_003（系统异常）或意外 RuntimeException，缺少堆栈将增加排障难度。建议改为 `logger.error("create todo error, errorCode: {}, message: {}", e.getErrorCode(), e.getMessage(), e)`。

### P1 — A2.4 — 重复 import

**`TodoServiceImpl.java:14-15`** — 同一类重复导入。

片段范围：`src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:14-15`

```java
L14|import org.springframework.transaction.annotation.Transactional;
L15|import org.springframework.transaction.annotation.Transactional;
```

**问题**：`Transactional` 重复导入两次，虽不影响编译，但属于冗余代码，建议删除一行。

---

## 8. 修复任务列表

### P0

- [ ] **P0** `src/main/java/com/alipay/todo/common/constant/SecurityContextHolder.java:11` — 在登录拦截器或 TodoController 的 finally 块中调用 `SecurityContextHolder.clear()`，防止 ThreadLocal 内存泄漏
- [ ] **P0** `src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:81-82` — `LocalDateTime.now()` 改为 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))` 或统一使用 UTC

### P1

- [ ] **P1** `src/main/java/com/alipay/todo/controller/TodoController.java:66` — logger.error 补充异常堆栈参数 `e`
- [ ] **P1** `src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java:14-15` — 删除重复的 `import org.springframework.transaction.annotation.Transactional`

### P2

- [ ] **P2** `src/main/java/com/alipay/todo/controller/TodoController.java` — 接入 metrics 埋点（QPS、耗时、成功率）
- [ ] **P2** `src/main/java/com/alipay/todo/service/impl/TodoServiceImpl.java` — 日志中补充 traceId 便于链路追踪
