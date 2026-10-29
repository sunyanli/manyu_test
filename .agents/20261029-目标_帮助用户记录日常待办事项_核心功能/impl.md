# 待办事项「新增待办」编码实现说明

> 依据：`.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/design.md`（v1.0）
> 实施规范：`dtazziboot-java-coding-standards`

## 1. 落地范围

仅实现最小闭环「新增待办事项」（F01/F02/F03），未引入列表、编辑、删除、提醒、幂等表、分布式锁、MQ、缓存。

## 2. 代码清单

| 分层 | 文件 | 说明 |
|------|------|------|
| 启动 | `src/main/java/com/antdigital/todo/TodoApplication.java` | Spring Boot 启动类 + `@MapperScan` |
| 接口 | `item/controller/TodoItemController.java` | W01 `POST /api/todo/item/create` |
| 服务 | `item/service/TodoItemService.java`<br>`item/service/impl/TodoItemServiceImpl.java` | S01，业务校验/规范化/组装/落库 |
| 持久 | `item/mapper/TodoItemMapper.java`<br>`item/model/TodoItemDO.java` | S02，MyBatis-Plus `BaseMapper.insert` |
| 模型 | `item/model/CreateTodoRequest.java`<br>`item/model/CreateTodoResponse.java` | 出入参 |
| 工具 | `item/util/TodoTextNormalizer.java` | 规范化、非法字符检查、按码点计长 |
| 常量/枚举 | `item/constant/TodoConstants.java`<br>`item/enums/TodoStatus.java` | 长度、Header 名、时区、状态 |
| 公共 | `common/api/ApiResponse.java`、`ResultCode.java` | 统一出参 `{result,msg,data}`、错误码 TODO_001~007 |
| 公共 | `common/exception/BizException.java`、`GlobalExceptionHandler.java` | 业务异常与统一异常映射 |
| 公共 | `common/context/OperatorContext.java`、`OperatorContextHolder.java` | 登录态操作人上下文（ThreadLocal） |
| 公共 | `common/interceptor/LoginInterceptor.java` | R01/R09/R11/R12/R13：来源校验、Header 解析、开关与灰度 |
| 公共 | `common/interceptor/RateLimitInterceptor.java`<br>`common/ratelimit/TokenBucketRateLimiter.java` | 用户级进程内令牌桶限流（默认 10 QPS） |
| 配置 | `common/config/TodoCreateProperties.java`、`WebMvcConfig.java`<br>`common/security/TrustedGatewayProperties.java` | `todo.create.*`、`todo.security.*` 配置绑定与拦截器注册 |
| 资源 | `src/main/resources/application.yml`<br>`src/main/resources/db/todo_item.sql` | 数据源/超时/健康检查、建表语句 |

## 3. 业务规则映射

| 规则 | 实现位置 |
|------|----------|
| R01/R11/R12/R13 登录态与来源校验 | `LoginInterceptor.preHandle` |
| R02/R03 名称必填与长度、描述长度 | `CreateTodoRequest` 注解（列宽防御）+ `TodoItemServiceImpl.checkTitleLength/checkDescriptionLength`（业务字符数） |
| R04 描述缺省按空串 | `TodoTextNormalizer.normalize(null)` → `""` |
| R05 文本规范化 | `TodoTextNormalizer.normalize`（去首尾空白、全角空格归一、连续空白折叠） |
| R06 非法字符 | `TodoTextNormalizer.containsIllegalChar` → TODO_005 |
| R07 归属取值登录态、`creator_name` 兜底 | `TodoItemServiceImpl.buildRecord/resolveCreatorName` |
| R08 状态固定 0 | `TodoStatus.TODO` |
| R09 总开关/灰度 | `LoginInterceptor.checkSwitchAndGray` |
| R10 落库成功才返回 OK、异常统一 TODO_004 | `insertRecord` 捕获 `DataAccessException`；影响行数 != 1 抛 TODO_004 |
| 事务 | 单表单条 insert，自动提交，无显式事务 |

## 4. 依赖变更（pom.xml）

新增 `spring-boot-starter-web`、`spring-boot-starter-validation`、`spring-boot-starter-actuator`、`mybatis-plus-boot-starter:3.5.3.2`、`mysql-connector-j`（runtime）、`spring-boot-starter-test`（test）、`h2`（test，仅用于验证真实 INSERT），并通过 `spring-boot-dependencies:2.7.18` 统一版本。既有 `com.antdigital.sort` 代码与依赖未改动。

## 5. 测试与验证结果

| 项 | 结果 |
|----|------|
| `mvn -B test` | BUILD SUCCESS，Tests run: 64, Failures: 0, Errors: 0（含存量 `SortUtilTest` 21 个） |
| 新增单测 | `TodoItemServiceImplTest`(10)、`TodoTextNormalizerTest`(13)、`LoginInterceptorTest`(8)、`RateLimitInterceptorTest`(2)、`TokenBucketRateLimiterTest`(3)、`TodoItemControllerTest`(5) |
| 端到端 | `TodoCreateIntegrationTest`(2)：真实 Spring 容器 + MyBatis-Plus + H2(MySQL 模式)，提交请求后回查 `todo_item` 校验字段值、主键回填与空描述落库 |
| 真实进程冒烟 | `spring-boot:run` 启动后请求真实 HTTP 入口：无登录态 → `TODO_002`；缺名称 → `TODO_001「事项名称不能为空」`；非 JSON → `TODO_001「请求参数格式不正确」`；名称 150 字符 → `TODO_001「事项名称不能超过100个字符」`；描述 501 字符 → `TODO_001`；含控制字符 → `TODO_005`；`tenantId` 与登录态不一致 → `TODO_003`；合法请求在无 MySQL 环境 → `TODO_004`（持久化失败降级，不泄露 SQL） |

## 6. 已知边界

- 未接入真实 MySQL 实例，成功路径的持久化由 H2(MySQL 模式) 覆盖；生产建表脚本见 `src/main/resources/db/todo_item.sql`。
- `X-Trace-Id` 仅透传记录日志，未做缺失时自动生成与 MDC 贯穿。
- 限流为实例级近似限流（设计已确认取舍），多副本下总量约为副本数×阈值。
