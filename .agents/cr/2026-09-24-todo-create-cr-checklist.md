# Code Review Checklist

> **Change** `todo-create`（新增待办事项最小闭环） · **分支/Commit** `AI/task-DEV-9d10e310-…-9b6893e6` / `e6f46f1`（变更范围 `dd91a6a..e6f46f1`） · **日期** `2026-09-24`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。
>
> **自动化预扫（强制前置，已完成）**：`scan-all-rules.sh` 对 12 个 `.java` 文件执行，结果：
> `=== Step 4 Rule Scan (B/M/I + A/S/G) === … === No findings. 52/222 rules scanned ===`（无命中）。以下 Step 3/4 命中项均为 LLM 人工核对补充。

---

## Step 1 — 执行队列（产物 A）

**列语义**：`✅`=已扫无命中；`⚠️`/`❌`=命中风险（见 Step 4 明细）；`N/A`=该节与本文件无关；列值缺省含义同此。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|-----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|-----|--------|
| 1 | src/main/java/com/manyu/todo/TodoApplication.java | 启动类/@MapperScan | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 2 | src/main/java/com/manyu/todo/api/controller/TodoController.java | REST 入口（REQ-1/4） | ⚠️ | ⚠️ | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | ⚠️ | ⚠️ |
| 3 | src/main/java/com/manyu/todo/common/exception/BizException.java | 业务异常载体 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 4 | src/main/java/com/manyu/todo/common/exception/GlobalExceptionHandler.java | 异常→统一响应（REQ-4） | ⚠️ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ⚠️ |
| 5 | src/main/java/com/manyu/todo/common/model/Result.java | 统一响应结构 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 6 | src/main/java/com/manyu/todo/dao/mapper/TodoMapper.java | DAO 接口 | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 7 | src/main/java/com/manyu/todo/model/dto/TodoCreateRequest.java | 请求 DTO（REQ-2/3） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ |
| 8 | src/main/java/com/manyu/todo/model/entity/TodoDO.java | 数据对象 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 9 | src/main/java/com/manyu/todo/model/vo/TodoVO.java | 视图对象（REQ-5） | ⚠️ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | N/A | ⚠️ |
| 10 | src/main/java/com/manyu/todo/service/TodoService.java | 业务接口 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 11 | src/main/java/com/manyu/todo/service/impl/TodoServiceImpl.java | 业务实现（REQ-1/2/3） | ✅ | ✅ | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 12 | src/test/java/com/manyu/todo/service/impl/TodoServiceImplTest.java | 单元测试 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |

非 Java 变更文件（**跳过**，不进入逐文件 Java 审查，仅在相关规则中引用）：`pom.xml`、`src/main/resources/application.yml`、`src/main/resources/db/schema.sql`、`src/main/resources/mapper/TodoMapper.xml`、`docs/ARCHITECTURE.md`、`docs/modules/todo/README.md`、`.agents/todo-create/impl.md`、`.agents/plans/login-feature.md`（前置阶段遗留计划文档，非本 change 产物，仅登记）。

- Java 守卫：变更含 12 个 `.java` 文件，非空，继续。
- 来源：`git diff --name-only dd91a6a..e6f46f1` 展开。

---

## Step 2 — 功能（产物 B）

> REQ 仅提取自 change 原文（requirement 描述 + `docs/modules/todo/README.md` + `docs/ARCHITECTURE.md`）。功能性不符统一标 **P0**。

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | Given 合法 name+description，When POST /api/todos/create，Then 落库并返回含自增 id 的 TodoVO | requirement：「核心功能：新增待办事项。任务信息：事项名称和描述。最小闭环：仅创建」；ARCHITECTURE.md §应用概览 | TodoController.java / TodoServiceImpl.java / TodoMapper.java | ✅ | TodoServiceImpl.java:31-43（insert+affected==1 校验+toVO）；TodoServiceImplTest:34-57（id=100 回填、name trim、ArgumentCaptor 验证写库参数） |
| REQ-2 | name 空白/为空 → A0001；超 64 字符 → A0002 | README.md §API「name 必填 ≤64」；错误码表 A0001/A0002 | TodoServiceImpl.java / TodoCreateRequest.java | ✅ | TodoServiceImpl.java:49-55；测试 should_throwException_when_nameIsBlank（:69-79）、when_nameExceedsMaxLength（:82-92） |
| REQ-3 | description 选填、超 512 字符 → A0003 | README.md §API「description 选填 ≤512」，错误码表 A0003 | TodoServiceImpl.java / TodoCreateRequest.java | ✅ | TodoServiceImpl.java:56-59；测试 should_throwException_when_descriptionExceedsMaxLength（:95-106） |
| REQ-4 | 请求体缺失/非法 JSON → A0400（请求体校验失败）或 A0001（请求为空） | README.md 错误码表：「A0400 请求体校验失败（@Valid）」「A0001 名称空白 / 请求为空」 | GlobalExceptionHandler.java / TodoController.java | ❌ **P0** | TodoController.java:36 `@Valid @RequestBody`（required=true）在 body 缺失/JSON 非法时抛 `HttpMessageNotReadableException`；GlobalExceptionHandler.java:36-39 仅兜底 `Exception.class` → 返回 **B0001「系统繁忙」**，README 声明的 A0400/A0001 行为不可达。spec 证据：README.md:52-53；代码证据：GlobalExceptionHandler.java:36-39 |
| REQ-5 | 成功响应 `gmtCreate` 为 ISO-8601 字符串（如 `2026-09-24T12:00:00.000+08:00`） | README.md §成功响应示例（README.md:44） | TodoVO.java / application.yml | ❌ **P0** | TodoVO.java:22 使用 `java.util.Date`，工程无任何 Jackson 日期格式配置（application.yml 无 spring.jackson 配置）；Spring Boot 2.7 默认将 `Date` 序列化为 epoch 毫秒数，与文档示例字符串格式不符，前端按文档对接会解析失败。spec 证据：README.md:44；代码证据：TodoVO.java:3,22 |

---

## Step 3 — 可读性检查（产物 C）

> 对照 `references/readability-checklist.md` A1–A7；预扫脚本 A* 无命中，以下为 LLM 全文核对。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 12 个文件均 UTF-8、文件名=顶层类名、无 Tab（空格缩进） |
| A2 | 源文件结构/import 顺序 | ⚠️ | **A2.4 (P2)** `TodoController.java:7-12`：import 组内非 ASCII 字典序——`javax.validation.Valid` 排在 `org.springframework.*` 之后（j < o 应在前）。其余文件 import 顺序合规、无 `import *` |
| A3 | 代码样式 | ✅ | K&R 大括号、4 空格缩进、行宽 ≤120、成员间空行均合规 |
| A4 | 命名规范 | ✅ | 类名/方法名/常量（`NAME_MAX_LENGTH`、`LOGGER`）合规；DO/VO/DTO 后缀、Test 后缀符合 A4.7 |
| A5 | 编码实践 | ✅ | 重写接口方法均有 `@Override`（TodoServiceImpl.java:31）；无空 catch、无 finalize |
| A6 | 特定元素样式 | ✅ | 无 switch/数组 C 风格/long 字面量问题；注解每行一个 |
| A7 | Javadoc 规范 | ✅ | public 类与 public 方法均有 Javadoc；简单 getter 依 A7.3 豁免 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

> 预扫：`scan-all-rules.sh` 52/222 条，No findings。其余由 LLM 逐条核对。Blocker→P0、Major→P1、Info→P2。

| ID | 状态 | 备注（命中写 `path:line`） |
|----|------|----------------------------|
| B001 | N/A | 无字面量 parse/of 调用 |
| B002 | N/A | 无数组比较 |
| B003 | N/A | 无 Arrays.fill |
| B004 | N/A | 无数组 toString |
| B005 | N/A | 无 Arrays.asList 基本类型数组 |
| B006 | N/A | 无 JUnit assertEquals（使用 AssertJ） |
| B007 | N/A | 无 catch Throwable/Error |
| B008 | N/A | 无 Executors 创建线程池 |
| B009 | N/A | 无 int 移位 |
| B010 | N/A | 无 BigDecimal(double) |
| B011 | N/A | 无包装类型 == 比较 |
| B012 | N/A | 无 Calendar 加天 |
| B013 | N/A | 无 Calendar.HOUR |
| B014 | N/A | 无集合泛型不兼容查询 |
| B015 | N/A | 无 toArray(T[]) |
| B016 | N/A | 无 Comparable 实现 |
| B017 | N/A | 无 this == null |
| B018 | N/A | 无数值三目提升 |
| B019 | N/A | 无 Money API |
| B020 | N/A | 无常量乘法溢出 |
| B021 | N/A | 无 Jedis |
| B022 | N/A | 无 SimpleDateFormat |
| B023 | N/A | 无创建未抛出异常（BizException 均throw） |
| B024 | N/A | 无裸 Thread |
| B025 | N/A | 无双括号初始化 |
| B026 | N/A | 无 equals(null) |
| B027 | N/A | 未重写 equals |
| B028 | N/A | 无 commons DateUtil |
| B029 | N/A | setter 赋值正确（TodoDO/TodoVO/Result 逐一核对） |
| B030 | N/A | 无浮点 == |
| B031 | N/A | 无 String.format |
| B032 | N/A | 无注解 getClass |
| B033 | N/A | 无 Unsafe |
| B034 | N/A | 无 Hashtable |
| B035 | N/A | 无同对象二元运算 |
| B036 | N/A | 无 IdentityHashMap |
| B037 | N/A | 无混用 varargs 条件表达式 |
| B038 | N/A | 无无条件递归 |
| B039 | N/A | 无 indexOf 参数颠倒 |
| B040 | N/A | 无 isInstance 误用 |
| B041 | N/A | 无手工 JDBC 连接（MyBatis 管理） |
| B042 | N/A | 非 JUnit3 |
| B043 | N/A | 无内部类 @Test |
| B044 | N/A | 无 JUnit3/4 混用（JUnit5 + MockitoExtension） |
| B045 | N/A | 无包装类型锁 |
| B046 | N/A | 无 while 循环 |
| B047 | N/A | 无数值 compare 精度损失 |
| B048 | N/A | 无 Math.round 整型入参 |
| B049 | N/A | 无日期格式化（时间由 DB NOW() 生成） |
| B050 | N/A | 无日期格式化 |
| B051 | N/A | 无 Boolean.getBoolean |
| B052 | N/A | 无日期格式化 |
| B053 | ✅ | 异常断言使用 assertThatThrownBy，无需 fail() 兜底（Test:62,75,88,102,116） |
| B054 | N/A | 无 EqualsTester |
| B055 | ✅ | `when().thenAnswer/thenReturn`（Test:39,113）、`verify(mock).insert(...)`（Test:54,65,78,91,104）用法正确 |
| B056 | N/A | 无 Arrays.asList 修改 |
| B057 | N/A | 无增强 for 中改集合 |
| B058 | N/A | 无集合自传参 |
| B059 | N/A | 无 nCopies |
| B060 | N/A | 无拆箱三目 |
| B061 | N/A | 无 sun.misc BASE64 |
| B062 | N/A | 无 ClassLoader 强转 |
| B063 | N/A | 无 javax.xml.bind 引用（javax.validation 不受影响，JDK8） |
| B064 | N/A | 无 Optional == |
| B065 | N/A | 无 Pojo 自赋值 |
| B066 | N/A | 无 Math.random 强转 |
| B067 | N/A | 无 Random 取余 |
| B068 | N/A | 无变量自赋值 |
| B069 | N/A | 无 compareTo |
| B070 | N/A | 无自身 equals |
| B071 | N/A | 无 size() >= 0 |
| B072 | N/A | 无 Stream.toString |
| B073 | N/A | StringBuilder 以 String 构造（Test:122） |
| B074 | N/A | 无 substring(0) |
| B075 | N/A | for 循环条件正确（Test:123-125） |
| B076 | N/A | 无 @Transactional（单条 insert 无需事务，无非 public 事务方法问题） |
| B077 | N/A | 无 catch Throwable |
| B078 | N/A | 无 assertThat(x).isEqualTo(x) |
| B079 | ✅ | @Mock 字段无显式赋值（Test:25-29） |
| B080 | ✅ | 6 个测试均含 assertThat/verify 断言 |
| B081 | N/A | 无新建集合排序丢弃 |
| M001 | N/A | 无重复条件判断 |
| M002 | N/A | 无 instanceof |
| M003 | N/A | 无包装类构造器 |
| M004 | N/A | 无 printStackTrace（日志用 SLF4J） |
| M005 | N/A | 无非静态内部类 |
| M006 | N/A | 无编译期确定布尔表达式 |
| M007 | N/A | 无空 catch |
| M008 | N/A | 未重写 equals/hashCode |
| M009 | N/A | 无不兼容类型 equals |
| M010 | N/A | 无位运算 |
| M011 | N/A | 无 switch |
| M012 | N/A | 无 finally return/throw |
| M013 | N/A | 无浮点强转 |
| M014 | N/A | 无枚举 getClass |
| M015 | N/A | 无子类隐藏父类字段 |
| M016 | ✅ | Java 代码未使用依赖默认时区的 now()（时间统一由 SQL `NOW()` 生成，JDBC URL 显式 serverTimezone=Asia/Shanghai） |
| M017 | N/A | 测试方法均有 @Test |
| M018 | N/A | 无显式 Lock |
| M019 | N/A | 无枚举 switch |
| M020 | ✅ | TodoServiceImpl.createTodo 有 @Override（:31） |
| M021 | N/A | 未重写 equals |
| M022 | N/A | 无 Optional.of(null) |
| M023 | N/A | 打印对象均有自定义 toString（Result/TodoDO/TodoVO/TodoCreateRequest） |
| M024 | N/A | 无 Optional.get |
| M025 | N/A | 无 final 类 protected 成员 |
| M026 | ✅ | @Mock 非 static（Test:25-26） |
| M027 | N/A | 无 ThreadLocal |
| I001 | ✅ | 异常断言含消息校验 hasMessageContaining（优于仅判类型） |
| I002 | N/A | 无 @DoNotMock 类 |
| I003 | N/A | 无 @AutoValue 类 |
| I004 | ❌ **P2** | `TodoDO.java:3,21,24`、`TodoVO.java:3,22` 使用 `java.util.Date`；建议改用 `java.time.LocalDateTime/Instant`（亦一并解决 REQ-5 序列化问题） |
| I005 | N/A | 非 JUnit3 |
| I006 | N/A | 无 setUp 方法 |
| I007 | N/A | 无 tearDown 方法 |
| I008 | N/A | 无 dataProvider |
| I009 | N/A | 仅统计用途 |
| I010 | ✅ | 纯 Mockito 单测，未启动容器，符合单测定位 |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | N/A | 无并发共享状态读写/先读后写场景（单条 insert） |
| G1.2 | N/A | 无加锁更新流程 |
| G1.3 | N/A | 无乐观锁重试 |
| G1.4 | N/A | 无多资源加锁 |
| G2.1 | ❌ **P0** | 写接口无幂等键：`POST /api/todos/create`（TodoController.java:35-38）无请求幂等 ID/去重表/唯一约束，双击/重发将重复落库重复待办。建议：客户端生成请求 ID + DB 唯一索引或 Redis 去重 |
| G2.2 | N/A | 无重试/定时任务/MQ 重投场景 |
| G2.3 | N/A | 无上游幂等键约定 |
| G3.1 | N/A | 无跨库/分布式事务 |
| G3.2 | N/A | 未使用 @Transactional（单条 insert 无需，无事务范围过大问题） |
| G4.1 | ✅ | SQL 仅单条 INSERT，业务逻辑在 Java 层 |
| G4.2 | ✅ | 无 WHERE 条件，无索引列函数/隐式转换 |
| G4.3 | N/A | 无列表查询/分页 |
| G4.4 | N/A | 模板遗留条目，`reliability-checklist.md` 无此 ID |
| G5.1 | N/A | 无 MQ 消费 |
| G6.1 | N/A | 无缓存 |
| G6.2 | N/A | 无缓存双写 |
| G7.1 | N/A | 无调度任务 |
| G7.2 | N/A | 无调度任务 |
| G8.1 | ✅ | 异常均经全局处理器转化，无吞异常/只打日志不响应 |
| G8.2 | ✅ | 依赖仅 MySQL 单点，无强弱依赖混合调用链 |
| G8.3 | N/A | 无手工 I/O 流/连接/锁（MyBatis 连接池管理） |
| G8.4 | N/A | 无自建线程池/定时任务 |
| G8.5 | N/A | 无 ThreadLocal |
| G8.6 | N/A | 无 Executors 默认无界线程池 |
| G8.7 | N/A | 模板遗留条目，清单无此 ID |
| G9.1 | ⚠️ **P0** | 外部调用（DB）未区分三态：所有异常统一兜底 B0001（GlobalExceptionHandler.java:36-39），超时/连接失败不可区分，与 G9.2 同源修复 |
| G9.2 | ❌ **P0** | DB 调用未设置超时：`application.yml:8` JDBC URL 无 `connectTimeout/socketTimeout`，HikariCP 亦未显式配置（仅默认获取连接超时，无 socket 读超时），慢查询可无限挂起 Tomcat 线程 |
| G9.3 | N/A | 无重试逻辑 |
| G10.1 | ✅ | Result 以 code 字段区分成功/失败，data=null 语义由 code 消歧 |
| G10.2 | N/A | 无上下游契约变更（新服务） |
| G10.3 | N/A | 模板遗留条目，清单无此 ID |
| G11.1 | ✅ | 6 个单测，断言完整（含 ArgumentCaptor 写库参数 verify） |
| G11.2 | ⚠️ **P2** | 边界未覆盖：service 层 `name == null` 分支（仅测空白，TodoServiceImpl.java:50）；`gmtCreate` 回填依赖 DB，未验证 NOW() 写入 |
| G11.3 | ✅ | request null 有防御（TodoServiceImpl.java:46-48）且有测试（Test:60-66） |
| G11.4 | N/A | 无数值/金额运算 |
| G12.1 | N/A | 非资金/库存/积分场景 |
| G12.2 | ✅ | 低危 CRUD，止血手段=发布回滚+按 id 删数据，无资损 |
| G13.1 | ✅ | 日志级别正确：业务异常 WARN、系统异常 ERROR（GlobalExceptionHandler.java:22,32,38） |
| G14.1 | N/A | 无金额/币种 |
| G14.2 | N/A | 内部单租户，无租户隔离诉求 |
| G14.3 | ✅ | 时间由 DB `NOW()` 生成，JDBC 显式 `serverTimezone=Asia/Shanghai`，无本地时区字符串存储 |
| G14.4 | N/A | 无 Java 侧日期格式化 |
| G15.1 | ✅ | 新建表 `CREATE TABLE IF NOT EXISTS`（schema.sql:2），无破坏性 DDL |
| G15.2 | ✅ | 全新接口，无旧接口共存问题 |
| G15.3 | N/A | 无不兼容逻辑切换 |
| G16.1 | ❌ **P0** | 核心链路（创建入口）无任何指标埋点（调用量/成功率/耗时），仅日志，无法监控告警。建议接入 Micrometer/内部监控 |
| G16.2 | ✅ | 异常日志含 code/message/堆栈（GlobalExceptionHandler.java:38） |
| G16.3 | ✅ | WARN/ERROR 分级正确（见 G13.1） |
| G16.4 | ✅ | 无空 catch、无 printStackTrace |
| G17.1 | ❌ **P0** | 无功能开关：创建入口无法不重启紧急关闭（如 DB 故障时关闭写入入口） |
| G17.2 | ❌ **P0** | 无降级预案：DB 不可用时仅返回「系统繁忙」，无兜底/限流策略 |
| G17.3 | ✅ | 仅 INSERT 新增数据，可按 id/时间删除回滚，无需补偿脚本 |
| G18.1 | N/A | `reliability-checklist.md` 未提供 G18 条目（模板遗留） |
| G18.2 | N/A | 同上 |
| G18.3 | N/A | 同上 |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | TodoMapper.xml 全量 `#{}` 预编译参数，无 `${}` 拼接 |
| S1.2 | N/A | 无 order by/group by/动态表名 |
| S1.3 | N/A | 无 like/in 查询 |
| S2.1 | N/A | 纯 JSON REST API，无 HTML/JS 渲染输出 |
| S2.2 | N/A | 无富文本 |
| S2.3 | N/A | 无模板引擎 |
| S3.1 | N/A | 无外部 URL 请求 |
| S3.2 | N/A | 无 302 跳转 |
| S3.3 | N/A | 无外部 HTTP 调用 |
| S4.1 | N/A | 无命令拼接 |
| S4.2 | N/A | 无外部命令/文件操作 |
| S5.1 | N/A | 无 XML 解析用户输入（MyBatis DTD 为固定声明） |
| S5.2 | N/A | 无 XPath |
| S6.1 | N/A | 无 Java 反序列化外部数据 |
| S6.2 | N/A | 无多态反序列化 |
| S6.3 | N/A | 无敏感字段 |
| S7.1 | N/A | 无文件上传下载 |
| S7.2 | N/A | 无路径处理 |
| S7.3 | N/A | 无文件存储 |
| S8.1 | ❌ **P0** | 接口无鉴权：无 Spring Security/登录态/网关鉴权任何一层（pom.xml 依赖清单、TodoController.java:19-21），内部用户场景下仍需至少统一登录/网关鉴权，否则可匿名写库刷数据 |
| S8.2 | ✅ | 创建操作使用 POST |
| S8.3 | ⚠️ **P1** | 数据 ID 为自增 Long 直接对外（TodoVO.java:13 + schema.sql:3），可预测可遍历；内部低危场景可接受，若后续开放查询接口建议 UUID/加密 ID |
| S8.4 | N/A | 无 Cookie 会话 |
| S9.1 | ❌ **P0** | 数据库凭证硬编码并提交仓库：`application.yml:9-10`（root/root），应改环境变量/配置中心并轮换已泄露凭证 |
| S9.2 | ✅ | 日志无敏感信息；TodoCreateRequest.toString 仅含 name，不含 description |
| S9.3 | ⚠️ **P1** | 未配置 HTTPS（HTTP 明文 8080，application.yml:1-2）；若由网关/SLB 终结 TLS 需在部署层面确认，应用层建议开启 redirect 或前置网关 |
| S9.4 | N/A | 无随机数使用 |
| S10.1 | ⚠️ **P1** | 增改操作无 CSRF 防护（TodoController.java:35）；若采用 Header Token 鉴权（非 Cookie）可豁免，需架构确认 |
| S10.2 | ✅ | 未配置 CORS（Spring Boot 默认同源，未放开 `*`） |
| S10.3 | N/A | 无 URL 跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

### 5.1 自定义扩展（`customized-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | ✅ | 示例项：Controller 入参已使用 `@Valid`（TodoController.java:36），且 Service 层有二次防御校验 |
| U1.2 | N/A(未启用自定义规则) | 清单仅含示例项 |
| U1.3 | N/A(未启用自定义规则) | 同上 |
| U2.1 | N/A(未启用自定义规则) | 业务红线节为空 |
| U2.2 | N/A(未启用自定义规则) | 同上 |
| U2.3 | N/A(未启用自定义规则) | 同上 |

---

## 终检（防漏检）

- [x] 执行队列 12 个 Java 文件 Step2/Step3/G/S 各列均非 `⬜`
- [x] Step 2 的 5 个 REQ 均非 `⬜`（REQ-4、REQ-5 为 ❌）
- [x] Step 3 的 A1–A7 均非 `⬜`（A2 ⚠️）
- [x] Step 4 全部 B/M/I（120 条）与 G/S ID 均非 `⬜`（N/A 均附原因）
- [x] Step 5 全部 U* ID 均非 `⬜`（N/A(未启用自定义规则)）
- [x] 所有 `❌/⚠️` 已写入 report 并附 `ID + path:line` 与问题片段
