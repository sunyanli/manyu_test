# 待办事项新增功能 编码实现报告

> **文档元信息**
>
> | 项目 | 内容 |
> |------|------|
> | 模块 | todo（待办事项） |
> | 系分来源 | `.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/design.md` |
> | 采用技能 | `dtazziboot-java-coding-standards` |
> | 作者 | AiWork |
> | 创建日期 | 2026/10/29 |
> | 实现语言/框架 | Java 8 + Spring Boot 2.7.18 + MyBatis 2.3.2（MyBatis-Spring Boot Starter） |

## 模块进度追踪

| 序号 | 模块 | READ | TEST | IMPL | CHECK | DOCS | 状态 |
|:----:|------|:----:|:----:|:----:|:-----:|:----:|------|
| 1 | todo | ✅ | ✅ | ✅ | ✅ | ✅ | 已完成 |

## 📖 READ: todo

**模块职责**：交付「新增待办事项」最小闭环——入参校验与规范化、身份与租户注入、限流、幂等判定、编号生成、落库、审计留痕与回执组装。

**关键类列表**：
- `TodoController` - 交互层：`POST /api/todo/item/create`
- `TodoExceptionHandler` - 交互层：异常 → 错误码 + HTTP 状态 + 统一响应体
- `TodoService` / `TodoServiceImpl` - 业务层：创建链路编排（S01）、幂等查询（S02）
- `TodoValidator` - 业务组件：入参校验与规范化（S03，R01~R04）
- `TodoNoGenerator` / `TodoNoGeneratorImpl` - 业务组件：编号生成（S04）
- `TodoSequenceProvider` / `SnowflakeSequenceProvider` - 业务组件：取号 SPI 与雪花降级实现
- `TodoRateLimiter` - 业务组件：单用户滑动窗口限流（R11）
- `AuthIntegration` / `GatewayAuthIntegrationImpl` - 集成层：登录态与身份解析（I01）
- `AuditIntegration` / `AuditIntegrationImpl` - 集成层：审计异步投递（I02）
- `TodoItemManager` - Manager 层：唯一键冲突语义转换
- `TodoItemMapper` - DAO 层：MyBatis 数据访问
- `TodoItemDO` - 数据对象（兼作查询条件载体）

**依赖关系**：上游统一网关（身份头透传）→ Controller → Service →（Validator/Generator/RateLimiter/Audit/Auth/Manager）→ MyBatis → MySQL `todo_item`。与既有 `com.antdigital.sort` 模块零依赖。

**已加载规范**：
- [x] naming.md
- [x] exception-logging.md
- [x] constants.md
- [x] mysql.md
- [x] project-structure.md
- [x] frontend-backend.md
- [x] comments.md
- [x] unit-testing.md

## 🧪 TEST: todo

| 测试文件 | 被测类 | 测试方法数 |
|----------|--------|:----------:|
| `src/test/java/com/antdigital/todo/service/impl/TodoServiceImplTest.java` | `TodoServiceImpl` | 17 |
| `src/test/java/com/antdigital/todo/service/component/TodoValidatorTest.java` | `TodoValidator` | 12 |
| `src/test/java/com/antdigital/todo/service/component/TodoNoGeneratorImplTest.java` | `TodoNoGeneratorImpl`、`SnowflakeSequenceProvider` | 5 |
| `src/test/java/com/antdigital/todo/common/util/TodoTextUtilTest.java` | `TodoTextUtil` | 9 |
| `src/test/java/com/antdigital/todo/controller/TodoControllerTest.java` | `TodoController`（含 `GatewayAuthIntegrationImpl`） | 3 |
| `src/test/java/com/antdigital/todo/dao/manager/TodoItemManagerTest.java` | `TodoItemManager` | 4 |

**TodoServiceImplTest 场景清单**：

| 方法 | 测试场景 |
|------|----------|
| should_createTodo_when_validRequest | 正常路径：落库 + 回执 + 审计 |
| should_forcePendingStatusAndDefaultSource_when_create | R08 固定值收口 |
| should_returnFirstResult_when_sameRequestIdSubmittedTwice | R07 幂等命中 |
| should_returnFirstResult_when_duplicateRequestIdRace | R07 并发唯一键冲突重查 |
| should_retryAndSucceed_when_todoNoConflictOnce | R09 编号冲突重试成功 |
| should_throwTodoNoConflict_when_todoNoAlwaysConflicts | R09 重试耗尽 → TODO_0007 |
| should_throwStorageError_when_insertFails | 存储异常 → TODO_0008 |
| should_throwNoPermission_when_rolesLackPermission | R06 无权限 → TODO_0006 |
| should_allowCreate_when_roleIsWildcard | R06 全量权限放行 |
| should_throwNotLogin_when_operatorIsNull / _when_userIdBlank | R05 → TODO_0005 |
| should_throwItemNameRequired_when_nameIsBlank | R02 → TODO_0002 |
| should_throwRateLimited_when_exceedsLimit | R11 → TODO_0009 |
| should_stillSucceed_when_auditThrows | R10 审计降级 |
| should_returnExistingResult_when_requestIdExists / _should_returnNull_when_requestIdNotExists / _should_throwParamInvalid_when_requestIdIsBlank | S02 幂等查询 |

**测试覆盖摘要**：
- 被测类：`TodoServiceImpl`、`TodoValidator`、`TodoNoGeneratorImpl`、`TodoItemManager`、`TodoController`、`TodoTextUtil`
- 测试方法数：50（参数化展开后 84 条用例，全部通过）
- 覆盖场景：正常路径 ✓、参数校验 ✓、异常处理 ✓、边界值（128/129 字符、512/513 字符、emoji、全角空格、控制字符）✓、并发幂等 ✓、降级 ✓
- Mock 选型：不引入新 Mock 依赖，使用手写替身（`FakeTodoItemMapper` / `FakeTodoNoGenerator` / `FakeAuditIntegration` / `FakeTodoService`），与项目现有 JUnit5 + AssertJ 技术栈一致

## 🔧 IMPL: todo

**已实现主代码（33 个文件）**：

- `src/main/java/com/antdigital/todo/TodoApplication.java`
- `controller/TodoController.java`、`controller/TodoExceptionHandler.java`
- `service/TodoService.java`、`service/impl/TodoServiceImpl.java`
- `service/component/TodoValidator.java`、`TodoNoGenerator.java`、`TodoNoGeneratorImpl.java`、`TodoSequenceProvider.java`、`SnowflakeSequenceProvider.java`、`TodoRateLimiter.java`
- `integration/AuthIntegration.java`、`integration/impl/GatewayAuthIntegrationImpl.java`
- `integration/AuditIntegration.java`、`integration/impl/AuditIntegrationImpl.java`
- `dao/entity/TodoItemDO.java`、`dao/mapper/TodoItemMapper.java`、`dao/manager/TodoItemManager.java`
- `model/dto/TodoCreateRequest.java`、`OperatorContext.java`、`ValidatedTodo.java`、`TodoCreateResult.java`
- `model/vo/TodoCreateVO.java`
- `common/constant/TodoConstants.java`
- `common/enums/TodoErrorCodeEnum.java`、`TodoStatusEnum.java`、`TodoSourceEnum.java`、`DeleteFlagEnum.java`
- `common/exception/TodoBizException.java`、`TodoDuplicateKeyException.java`
- `common/util/TodoTextUtil.java`、`DesensitizeUtil.java`
- `common/model/ApiResult.java`

**已实现资源与构建**：

- `src/main/resources/mapper/TodoItemMapper.xml`（resultMap + 显式字段列表 + `#{}` 全参数化）
- `src/main/resources/application.yml`（数据源/MyBatis/登录开关/限流配置）
- `src/main/resources/db/todo_item.sql`（建表脚本，含唯一索引与查询索引）
- `pom.xml`（新增 Spring Boot BOM、web/validation、MyBatis starter、MySQL 驱动）

**关键实现决策（对系分的落地取舍，均不改变对外契约）**：

1. **不新建 Maven 子模块**：系分约束中已明确「待办模块以新增包 `com.antdigital.todo` 独立引入，不改动既有排序模块行为」，且多模块拆分会使既有 `sort-core` 构建与测试路径变更、风险不可控，故采用单模块新增包方案。
2. **取号默认走本地雪花**：仓库无 Redis 依赖，为保持可构建、可验证，`TodoSequenceProvider` 抽象取号能力，默认 `SnowflakeSequenceProvider`；接入 Redis INCR 只需新增一个实现并注入，无需改业务代码（系分推荐的降级路径即为雪花）。
3. **限流为单实例口径**：`TodoRateLimiter` 采用本地滑动窗口（默认 30 次/分钟）；集群级限流由网关层承担（系分 6.3 亦要求双层限流）。
4. **唯一键冲突语义转换**：MyBatis-Spring 会把冲突翻译为 Spring `DuplicateKeyException`，新增 `TodoItemManager` 统一转换为 `TodoDuplicateKeyException` 并解析索引名（`uk_todo_item_req` / `uk_todo_item_no`），使业务层区分幂等冲突与编号冲突，同时避免业务层耦合持久化框架异常。
5. **校验分工**：注解（`@NotBlank`）承担非空快速失败；长度按字符（code point）计，因 `@Size` 按 UTF-16 计数会误杀 emoji，故长度与字符集统一由 `TodoValidator` 校验，避免错误码口径分裂。
6. **幂等不单独建表**：沿用系分结论，`uk_todo_item_req(tenant_id, request_id)` 即幂等记录。

**编译验证**：见下方 CHECK 的 L2 动态验证。

## ✅ CHECK: todo

### L1 静态检查

| 检查项 | 规范要求 | 符合情况 |
|--------|----------|:--------:|
| 命名规范 | 类名大驼峰、方法名/变量名小驼峰、常量全大写、枚举 `Enum` 后缀 | ✅ |
| 分层与包结构 | controller/service/dao/model/common，接口与实现分离（`impl` 包） | ✅ |
| 前后端契约 | JSON key 为 lowerCamelCase；主键 Long 以 String 返回；时间 `yyyy-MM-dd HH:mm:ss` | ✅ |
| 错误响应 | HTTP 状态 + errorCode + 提示信息 + data，错误码不直接作为用户提示 | ✅ |
| 异常处理 | 自定义 `TodoBizException`（unchecked）；不 catch NPE/IndexOutOfBounds；非稳定代码分类捕获；`finally` 未用 return | ✅ |
| 日志规范 | SLF4J + 占位符；无 `System.out` / `printStackTrace`；异常日志含现场与堆栈；名称/描述不落原文（脱敏/隐藏） | ✅ |
| 常量与枚举 | 无魔法值（长度、默认值、请求头、权限点均常量/枚举化）；`long` 字面量用大写 `L` | ✅ |
| 安全 | MyBatis 全 `#{ }` 参数化、无 `${}`、无 `SELECT *`；身份只取网关注入；无权限/登录态快速失败；入参长度与字符集校验 | ✅ |
| MySQL | 表名/字段名小写下划线且 <26；含 `id`/`gmt_create`/`gmt_modified`；`pk_`/`uk_`/`idx_` 命名；索引列 NOT NULL 有默认值；无外键；`tinyint` 状态、`datetime` 时间 | ✅ |
| 事务 | 单表写入 `@Transactional(rollbackFor = Exception.class)`；审计/限流在事务外语义，避免长事务 | ✅ |
| 注释 | 类/方法级 javadoc，含 `@author`/`@date`；枚举值均有注释 | ✅ |
| 单元测试 | 6 个测试类、50 个测试方法，AAA 结构，断言具体值；测试包路径与被测类一致 | ✅ |

### L2 动态验证

| 验证项 | 状态 | 说明 |
|--------|:----:|------|
| 编译验证 | ✅ | `javac 17.0.2 -source 8 -target 8`：待办模块 33 个主代码 + 6 个测试类（连同既有排序模块 8 个文件，共 47 个源文件）全部编译通过，0 error |
| 单测验证 | ✅ | JUnit Platform 1.10.2 执行：**84 个测试用例全部通过，0 失败**（17 个容器），含既有 `SortUtilTest` 回归通过 |
| MyBatis 映射校验 | ✅ | 用 MyBatis `XMLMapperBuilder` 真实解析 `TodoItemMapper.xml`：2 条 statement 正确绑定到 Mapper 方法、1 个 resultMap 注册成功 |
| Spring 装配校验 | ✅ | `AnnotationConfigApplicationContext` 真实启动：13 个 Bean 全部装配成功（含 `@Autowired` 取号 SPI 注入、`@Value` 默认值、`@PreDestroy` 正常关闭），并成功生成编号 `TD20261029xxxxxx` |
| 端到端 HTTP 冒烟 | ⚠️ | 沙箱无 MySQL 实例，未启动真实 Web 服务；控制器层已通过 `TodoControllerTest` 覆盖协议适配与身份解析 |

**[环境说明]**：沙箱初始无 JDK 与 Maven，`apt` 通道因 `setgroups ... Operation not permitted` 不可用。为完成真实验证，改为直连发行包与 Maven Central 下载 JDK 17 及运行/测试所需 jar（全部落在 `/tmp`，未写入仓库），执行等价于 `mvn compile` + `mvn test` 的编译与测试。因此本节点的编译与单测结论为**实测结果**，而非静态推断；仓库内仍保留标准 Maven 命令供 CI 复跑。

静态审查同步完成（与实测相互印证）：
- **正常路径**：身份 → 权限 → 限流 → 校验 → 幂等查询 → 取号 → 插入 → 审计 → 回执，与系分时序图一致。
- **幂等分支**：先查 `uk_todo_item_req`，命中直接返回 `idempotent=true`；并发场景由唯一索引兜底后重查，仍缺失则按存储异常处理（避免把未知冲突误判为成功）。
- **编号冲突分支**：失败重试次数等于 `TODO_NO_CONFLICT_RETRY_TIMES`（1），循环上限 `<=` 保证总尝试 2 次，耗尽抛 TODO_0007（已由 `should_throwTodoNoConflict_when_todoNoAlwaysConflicts` 验证 `insert` 恰好 2 次）。
- **边界值**：名称 128/129 字符、描述 512/513 字符、emoji 按 1 字符计、全角空格 trim、控制字符剔除保留 `\n`/`\t`，均有对应用例。
- **类型一致性**：`TodoCreateRequest` → `ValidatedTodo` → `TodoItemDO` → `TodoCreateResult` → `TodoCreateVO` 字段逐层对应，`status`/`deleted` 为 `Integer`，`id` 在 VO 层转 `String`。
- **异常语义**：`TodoDuplicateKeyException`（业务可识别）与其它 `RuntimeException`（存储异常）分类处理；审计 `catch (Throwable)` 保证弱依赖不阻断（`should_stillSucceed_when_auditThrows` 验证）。
- **事务**：MyBatis-Spring 唯一键冲突不置事务 rollback-only（异常在方法内被消化），同事务内重试取号为安全操作。

#### 📋 待人工验证

本节点已在沙箱内以等价方式完成编译与单测；仓库 CI 环境请复跑标准命令，并补充数据库与 HTTP 冒烟：

```bash
mvn -q compile -DskipTests
mvn -q test
# 初始化数据库后启动并冒烟
mysql -u root -p < src/main/resources/db/todo_item.sql
mvn spring-boot:run
curl -X POST http://localhost:8080/api/todo/item/create \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: U10086' -H 'X-User-Name: 张三' -H 'X-Tenant-Id: 0' -H 'X-User-Roles: TODO_CREATE' \
  -d '{"requestId":"8f2b1c34-5d6e-4a7b-9c8d-0e1f2a3b4c5d","itemName":"整理季度经营分析材料","itemDesc":"汇总 Q3 数据"}'
```

**发现问题**：编码过程中发现并修复 1 处设计落地缺口——MyBatis-Spring 唯一键冲突异常类型与业务侧约定不一致，新增 `TodoItemManager` 做语义转换（见「关键实现决策」第 4 条）；修复后由 `TodoItemManagerTest`（3 个转换用例）与 `TodoServiceImplTest` 的并发幂等/重试用例共同覆盖。

## 📝 DOCS: todo

**文档操作**：
- 架构文档：新建 `docs/ARCHITECTURE.md`（仓库形态、待办服务分层、关键约束、模块索引）
- 模块文档：新建 `docs/modules/todo/README.md`（职责、关键类、依赖、API、错误码、配置、建表脚本）
- 编码报告：已写入 `.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/impl.md`

## ✅ 模块 todo 完成

| 阶段 | 状态 |
|------|:----:|
| READ | ✅ |
| TEST | ✅ |
| IMPL | ✅ |
| CHECK | ✅ |
| DOCS | ✅ |

**下一步**：仓库 CI 复跑 `mvn test`，并在有 MySQL 的环境完成建表与 HTTP 冒烟；随后进入列表/完成/删除等后续阶段的接口扩展。
