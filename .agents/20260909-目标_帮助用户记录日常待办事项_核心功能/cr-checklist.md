# Code Review Checklist

**Change** `20260909-帮助用户记录日常待办事项-核心功能` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-45532d90-fff9-4c9c-9c15-ea40bd6b6bc1` · **日期** `2026-09-09`

> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。
> **执行顺序（强制）**：写入本清单并进入逐文件审查前，先在目标仓库对变更路径运行 `references/script/scan-all-rules.sh`，将输出贴入 Step 3 和 Step 4 备注；再用 LLM 完成 Step 2–5 中脚本未覆盖项及复核。

---

## Step 1 — 执行队列（产物 A）

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|-----|----|----|----|----|----|----|----|----|----|-----|-----|-----|-----|-----|----|----|-----|--------|
| 1 | `src/main/java/com/dtazziboot/todoapp/TodoApplication.java` | 启动类 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 2 | `src/main/java/com/dtazziboot/todoapp/common/constant/TodoConstants.java` | 常量 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 3 | `src/main/java/com/dtazziboot/todoapp/common/context/LoginContext.java` | 登录上下文 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ G8.5 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 4 | `src/main/java/com/dtazziboot/todoapp/common/enums/ErrorCodeEnum.java` | 错误码枚举 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 5 | `src/main/java/com/dtazziboot/todoapp/common/exception/BusinessException.java` | 业务异常 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 6 | `src/main/java/com/dtazziboot/todoapp/common/exception/GlobalExceptionHandler.java` | 全局异常处理器 | ✅ | ⚠️ A3.4 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ 已审有问题 |
| 7 | `src/main/java/com/dtazziboot/todoapp/common/model/ApiResult.java` | 统一出参 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 8 | `src/main/java/com/dtazziboot/todoapp/config/WebMvcConfig.java` | Web MVC 配置 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 9 | `src/main/java/com/dtazziboot/todoapp/controller/TodoController.java` | REST 控制器 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ S8.1 | N/A | N/A | ✅ 已审 |
| 10 | `src/main/java/com/dtazziboot/todoapp/dao/mapper/TodoItemMapper.java` | MyBatis Mapper | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ S1.1 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 11 | `src/main/java/com/dtazziboot/todoapp/model/dto/TodoCreateRequest.java` | 请求 DTO | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ G11.3 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 12 | `src/main/java/com/dtazziboot/todoapp/model/dto/TodoCreateResult.java` | 创建结果 DTO | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 13 | `src/main/java/com/dtazziboot/todoapp/model/dto/TodoVO.java` | 视图对象 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 14 | `src/main/java/com/dtazziboot/todoapp/model/entity/TodoItemDO.java` | 数据对象 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 15 | `src/main/java/com/dtazziboot/todoapp/service/TodoService.java` | 服务接口 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 16 | `src/main/java/com/dtazziboot/todoapp/service/impl/TodoServiceImpl.java` | 服务实现 | ✅ | ⚠️ A3.4 | N/A | ⚠️ G2.1 | ✅ G3.2 | N/A | N/A | N/A | N/A | ✅ G8.1,G8.5 | N/A | ⚠️ G14.3 | N/A | N/A | ⚠️ G16.1 | ⚠️ G14.3 | N/A | ⚠️ G16.1 | ⚠️ G17.1 | ✅ S1.1 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ 已审有问题 |
| 17 | `src/main/resources/application.yml` | 应用配置 | ✅ | N/A(非 Java) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 18 | `src/main/resources/mapper/TodoItemMapper.xml` | MyBatis 映射 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ S1.1 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 19 | `src/main/resources/sql/schema.sql` | 建表 SQL | ✅ | ✅ | N/A | N/A | N/A | ✅ G4.4 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 20 | `src/test/java/com/dtazziboot/todoapp/service/impl/TodoServiceImplTest.java` | 单元测试 | ✅ | ⚠️ A3.4×3 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ G11.1 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ 已审有问题 |
| 21 | `pom.xml` | Maven 配置 | ✅ | N/A(非 Java) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| F01 | Given 提交 title+description, When 调用 POST /api/todo/create, Then 返回创建结果 | design.md §1 / impl.md §1 | TodoController.java, TodoServiceImpl.java, TodoItemMapper.java | ✅ | Controller 接收 TodoCreateRequest，Service 调用 Mapper 插入并返回 TodoCreateResult |
| R01 | title 不能为空且长度 ≤128 | design.md §4.1 / impl.md §1 | TodoCreateRequest.java:17-18 | ✅ | @NotBlank + @Size(max=128) 校验 |
| R02 | description 长度 ≤1024 | design.md §4.1 / impl.md §1 | TodoCreateRequest.java:24 | ✅ | @Size(max=1024) 校验 |
| R03 | creator_id 从登录上下文获取，不可为空 | design.md §4.1 / impl.md §1 | TodoServiceImpl.java:54-60 | ✅ | LoginContext.getCreatorId() 获取，为空时抛 BusinessException(TODO_004) |
| R04 | tenant_id 从登录上下文获取，不可为空 | design.md §4.1 / impl.md §1 | TodoServiceImpl.java:55-64 | ✅ | LoginContext.getTenantId() 获取，为空时抛 BusinessException(TODO_004) |
| E01 | 错误码 TODO_001-005 覆盖所有错误场景 | design.md §4.1 / impl.md §5 | ErrorCodeEnum.java | ✅ | 枚举包含 TODO_001~TODO_005 |
| E02 | 统一出参结构 {result, msg, data} | design.md §5 / impl.md §1 | ApiResult.java | ✅ | ApiResult<T> 包含 result/msg/data 三个字段 |
| E03 | MyBatis 使用 #{} 参数化，禁止 ${} | design.md §6.4 / impl.md §1 | TodoItemMapper.java:23-24, TodoItemMapper.xml | ✅ | Mapper 接口使用 #{}，XML 中无 ${} |
| E04 | 事务控制：@Transactional(rollbackFor = Exception.class) | design.md §6.3 / impl.md §1 | TodoServiceImpl.java:51 | ✅ | 方法标注 @Transactional(rollbackFor = Exception.class) |
| E05 | 单元测试覆盖正常/异常路径 | impl.md §2 | TodoServiceImplTest.java | ✅ | 5 个测试方法覆盖：正常路径、无描述创建、creatorId 为空、tenantId 为空、插入失败 |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注（命中写 path:line） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | |
| A2 | 源文件结构/import 顺序 | ✅ | |
| A3 | 代码样式 | ⚠️ | A3.4 行宽超 120 字符：GlobalExceptionHandler.java:55, TodoServiceImplTest.java:133/147/167 |
| A4 | 命名规范 | ✅ | |
| A5 | 编码实践 | ✅ | |
| A6 | 特定元素样式 | ✅ | |
| A7 | Javadoc 规范 | ✅ | |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

> 预扫结果（scan-all-rules.sh）：P0 S1.1 误报（pom.xml Maven 属性引用），P1 M016 ×2，P2 A3.4 ×4。

| ID | 状态 | 备注（命中写 path:line；预扫可粘贴脚本摘要） |
|----|------|--------------------------------------------------|
| B001 | N/A | |
| B002 | N/A | |
| B003 | N/A | |
| B004 | N/A | |
| B005 | N/A | |
| B006 | N/A | |
| B007 | N/A | |
| B008 | N/A | |
| B009 | N/A | |
| B010 | N/A | |
| B011 | N/A | |
| B012 | N/A | |
| B013 | N/A | |
| B014 | N/A | |
| B015 | N/A | |
| B016 | ⚠️ | M016 TodoServiceImpl.java:72,73 — LocalDateTime.now() 未指定时区 |
| B017 | N/A | |
| B018 | N/A | |
| B019 | N/A | |
| B020 | N/A | |
| B021 | N/A | |
| B022 | N/A | |
| B023 | N/A | |
| B024 | N/A | |
| B025 | N/A | |
| B026 | N/A | |
| B027 | N/A | |
| B028 | N/A | |
| B029 | N/A | |
| B030 | N/A | |
| B031 | N/A | |
| B032 | N/A | |
| B033 | N/A | |
| B034 | N/A | |
| B035 | N/A | |
| B036 | N/A | |
| B037 | N/A | |
| B038 | N/A | |
| B039 | N/A | |
| B040 | N/A | |
| B041 | N/A | |
| B042 | N/A | |
| B043 | N/A | |
| B044 | N/A | |
| B045 | N/A | |
| B046 | N/A | |
| B047 | N/A | |
| B048 | N/A | |
| B049 | N/A | |
| B050 | N/A | |
| B051 | N/A | |
| B052 | N/A | |
| B053 | N/A | |
| B054 | N/A | |
| B055 | N/A | |
| B056 | N/A | |
| B057 | N/A | |
| B058 | N/A | |
| B059 | N/A | |
| B060 | N/A | |
| B061 | N/A | |
| B062 | N/A | |
| B063 | N/A | |
| B064 | N/A | |
| B065 | N/A | |
| B066 | N/A | |
| B067 | N/A | |
| B068 | N/A | |
| B069 | N/A | |
| B070 | N/A | |
| B071 | N/A | |
| B072 | N/A | |
| B073 | N/A | |
| B074 | N/A | |
| B075 | N/A | |
| B076 | N/A | @Transactional 在 public 方法上，符合要求 |
| B077 | N/A | |
| B078 | N/A | |
| B079 | N/A | |
| B080 | N/A | |
| B081 | N/A | |
| M001 | N/A | |
| M002 | N/A | |
| M003 | N/A | |
| M004 | N/A | |
| M005 | N/A | |
| M006 | N/A | |
| M007 | N/A | |
| M008 | N/A | |
| M009 | N/A | |
| M010 | N/A | |
| M011 | N/A | |
| M012 | N/A | |
| M013 | N/A | |
| M014 | N/A | |
| M015 | N/A | |
| M016 | ⚠️ | TodoServiceImpl.java:72,73 — LocalDateTime.now() 未指定时区 |
| M017 | N/A | |
| M018 | N/A | |
| M019 | N/A | |
| M020 | N/A | |
| M021 | N/A | |
| M022 | N/A | |
| M023 | N/A | |
| M024 | N/A | |
| M025 | N/A | |
| M026 | N/A | |
| M027 | N/A | |
| I001 | N/A | |
| I002 | N/A | |
| I003 | N/A | |
| I004 | N/A | |
| I005 | N/A | |
| I006 | N/A | |
| I007 | N/A | |
| I008 | N/A | |
| I009 | N/A | |
| I010 | N/A | |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | N/A | 无并发读写冲突场景 |
| G1.2 | N/A | |
| G1.3 | N/A | |
| G1.4 | N/A | |
| G2.1 | ⚠️ | 写接口无幂等键，同一请求可重复创建 |
| G2.2 | N/A | |
| G2.3 | N/A | |
| G3.1 | N/A | 无跨库操作 |
| G3.2 | ✅ | @Transactional 范围仅含 DB 插入，无外部 I/O |
| G4.1 | N/A | |
| G4.2 | N/A | |
| G4.3 | N/A | |
| G4.4 | ✅ | MyBatis 使用 #{} 预编译 |
| G5.1 | N/A | 无 MQ 调用 |
| G6.1 | N/A | 无缓存 |
| G6.2 | N/A | |
| G7.1 | N/A | |
| G7.2 | N/A | |
| G8.1 | ✅ | 异常路径均有日志输出 |
| G8.2 | N/A | |
| G8.3 | N/A | 无 I/O 流需手动释放 |
| G8.4 | N/A | 无线程池 |
| G8.5 | ✅ | LoginContext.clear() 在 @AfterEach 中调用 |
| G8.6 | N/A | 无 Executors 创建线程池 |
| G9.1 | N/A | 无外部调用 |
| G9.2 | N/A | |
| G9.3 | N/A | |
| G10.1 | N/A | |
| G10.2 | N/A | |
| G11.1 | ✅ | 有单元测试，5 个测试方法均有断言 |
| G11.2 | ⚠️ | 未覆盖边界值测试（title 恰好 128 字符、description 恰好 1024 字符） |
| G11.3 | ✅ | @NotBlank 和 @Size 注解提供空值防御 |
| G11.4 | N/A | 无金额运算 |
| G12.1 | N/A | 无资金相关场景 |
| G12.2 | N/A | |
| G13.1 | ✅ | 日志级别正确 |
| G14.1 | N/A | 无金额计算 |
| G14.2 | ✅ | tenantId 来自登录上下文，非用户可控参数 |
| G14.3 | ⚠️ | LocalDateTime.now() 未显式指定时区 |
| G14.4 | N/A | |
| G15.1 | N/A | 无数据库表结构变更 |
| G15.2 | N/A | |
| G15.3 | N/A | |
| G16.1 | ⚠️ | 缺少接口入口埋点（设计文档 §6.5 要求） |
| G16.2 | ✅ | 异常路径有日志输出且包含上下文 |
| G16.3 | ✅ | 日志级别正确 |
| G16.4 | N/A | |
| G17.1 | ⚠️ | 设计文档 §7.3 要求功能开关，代码未实现 |
| G17.2 | N/A | |
| G17.3 | N/A | |
| G18.1 | N/A | |
| G18.2 | N/A | |
| G18.3 | N/A | |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | MyBatis 使用 #{} 预编译，XML 中无 ${} |
| S1.2 | N/A | |
| S1.3 | N/A | |
| S2.1 | N/A | 无 HTML/JSON 输出编码场景 |
| S2.2 | N/A | |
| S2.3 | N/A | |
| S3.1 | N/A | |
| S3.2 | N/A | |
| S3.3 | N/A | |
| S4.1 | N/A | |
| S4.2 | N/A | |
| S5.1 | N/A | |
| S5.2 | N/A | |
| S6.1 | N/A | |
| S6.2 | N/A | |
| S6.3 | N/A | |
| S7.1 | N/A | |
| S7.2 | N/A | |
| S7.3 | N/A | |
| S8.1 | ✅ | 依赖内部统一登录态拦截器校验登录态 |
| S8.2 | N/A | |
| S8.3 | N/A | |
| S8.4 | N/A | |
| S9.1 | ✅ | 密码字段为空，未硬编码凭证 |
| S9.2 | ✅ | creator_id/tenant_id 不在日志中明文输出 |
| S9.3 | N/A | |
| S9.4 | N/A | |
| S10.1 | N/A | |
| S10.2 | N/A | |
| S10.3 | N/A | |

---

## Step 5 — 自定义扩展检查（产物 E）

**N/A(未启用自定义规则)** — `customized-checklist.md` 中 U1.1 条目（Controller 入参必须使用 `@Valid`）已满足：`TodoController.createTodo` 使用了 `@Valid` 注解。

### 5.1 自定义扩展（`customized-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | ✅ | Controller 已使用 @Valid 注解 |
| U1.2 | N/A | |
| U1.3 | N/A | |
| U2.1 | N/A | |
| U2.2 | N/A | |
| U2.3 | N/A | |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 Step2、Step3、S1–S10/G1–G17 各列均非 ⬜（跳过文件除外）
- [x] Step 2 的每个 REQ/Scenario 均非 ⬜
- [x] Step 3 的 A1–A7 均非 ⬜
- [x] Step 4 全部 G/S 与 B001–B081 / M001–M027 / I001–I010 ID 均非 ⬜（允许 N/A，但有原因）
- [x] Step 5 全部 U* ID 均非 ⬜（允许 N/A(未启用自定义规则)）
- [x] 所有 ❌/⚠️ 已写入 report，且包含 ID + path:line
