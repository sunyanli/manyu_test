# Code Review Report

> **Change** 分别写三个接口helloworld、哈希算法以及冒泡排序 · **分支/Commit** `AI/task-DEV-9d10e310-7901-11f1-8a9f-59ecae612580-97503bfa-3cbb-44e3-95f8-0ee1face2899` / `ca44a90` · **日期** 2026-08-24 · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。**已运行** `scan-all-rules.sh` 并将要点并入 §5。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | 21 |
| 其他文件数 | 8 (pom.xml, schema.sql, application.yml, CallRecordMapper.xml, index.html, design.md, cr_report.md, run_context.json) |
| 变更行数 | `+1521 / -0` |

| 类/接口 | 路径 | 角色 |
|---------|------|--------------|
| AlgorithmDemoApplication | `src/main/java/.../AlgorithmDemoApplication.java` | 启动类 |
| ApiNameConstant | `src/main/java/.../common/constant/ApiNameConstant.java` | 常量定义 |
| ErrorCodeConstant | `src/main/java/.../common/constant/ErrorCodeConstant.java` | 错误码常量 |
| BusinessException | `src/main/java/.../common/exception/BusinessException.java` | 业务异常 |
| GlobalExceptionHandler | `src/main/java/.../common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| AsyncConfig | `src/main/java/.../config/AsyncConfig.java` | 异步线程池配置 |
| WebConfig | `src/main/java/.../config/WebConfig.java` | CORS 配置 |
| AlgorithmController | `src/main/java/.../controller/AlgorithmController.java` | 算法接口控制器 |
| ExportController | `src/main/java/.../controller/ExportController.java` | 导出接口控制器 |
| CallRecordMapper | `src/main/java/.../dao/mapper/CallRecordMapper.java` | 数据访问层 |
| HashRequest | `src/main/java/.../model/dto/HashRequest.java` | 哈希请求 DTO |
| BubbleSortRequest | `src/main/java/.../model/dto/BubbleSortRequest.java` | 冒泡排序请求 DTO |
| CallRecord | `src/main/java/.../model/entity/CallRecord.java` | 调用记录实体 |
| Department | `src/main/java/.../model/entity/Department.java` | 部门实体 |
| HelloWorldVO | `src/main/java/.../model/vo/HelloWorldVO.java` | HelloWorld 响应 VO |
| HashVO | `src/main/java/.../model/vo/HashVO.java` | 哈希响应 VO |
| BubbleSortVO | `src/main/java/.../model/vo/BubbleSortVO.java` | 冒泡排序响应 VO |
| AlgorithmService | `src/main/java/.../service/AlgorithmService.java` | 算法服务接口 |
| ExportService | `src/main/java/.../service/ExportService.java` | 导出服务接口 |
| AlgorithmServiceImpl | `src/main/java/.../service/impl/AlgorithmServiceImpl.java` | 算法服务实现 |
| ExportServiceImpl | `src/main/java/.../service/impl/ExportServiceImpl.java` | 导出服务实现 |
| TrackingAspect | `src/main/java/.../tracking/aop/TrackingAspect.java` | 埋点切面 |
| TrackingController | `src/main/java/.../tracking/controller/TrackingController.java` | 统计控制器 |
| TrackingService | `src/main/java/.../tracking/service/TrackingService.java` | 埋点服务接口 |
| TrackingServiceImpl | `src/main/java/.../tracking/service/impl/TrackingServiceImpl.java` | 埋点服务实现 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 5 | 4 | 5 |

---

## 3. Step 2 — 功能（REQ）

### REQ-F01: HelloWorld 接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/hello?name=Alice 返回问候语 | ✅ | Design §5.1.2 W01 | `AlgorithmController.java:32-36` | 正确实现，返回 message + timestamp |
| name 为空时默认 "World" | ✅ | Design §5.1.2 W01 入参表 | `AlgorithmServiceImpl.java:29-31` | 正确实现 |
| name 超过100字符返回 PARAM_001 | ✅ | Design §5.1.3.1 R01 | `AlgorithmServiceImpl.java:32-33` | 正确校验 |

### REQ-F02: 哈希算法接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/hash 计算哈希值 | ✅ | Design §5.1.2 W02 | `AlgorithmController.java:42-45` | 正确实现 |
| 支持 MD5/SHA-256/SHA-512 | ✅ | Design §5.1.2 W02 入参表 | `AlgorithmServiceImpl.java:46-58` | 三种算法正确映射 |
| 不支持的算法返回 ALGO_001 | ✅ | Design §5.1.2 W02 错误码 | `AlgorithmServiceImpl.java:57` | 正确抛出异常 |
| input 为空返回 PARAM_001 | ✅ | Design §5.1.3.2 R02 | `HashRequest.java:11` | `@NotBlank` 校验 |
| input 超过10KB 限制 | ⚠️ | Design §5.1.3.2 异常场景 | `HashRequest.java:12` | 使用 `@Size(max=10240)` 校验，符合设计预期 |
| input.getBytes() 使用默认字符集 | ❌ | Design §6.3 稳定性 | `AlgorithmServiceImpl.java:61` | 使用平台默认字符集，跨环境不一致 → **P0** |

### REQ-F03: 冒泡排序接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/bubble-sort 排序 | ✅ | Design §5.1.2 W03 | `AlgorithmController.java:51-54` | 正确实现 |
| 空数组返回 PARAM_001 | ✅ | Design §5.1.3.3 R04 | `BubbleSortRequest.java:11` | `@NotEmpty` 校验 |
| 数组长度超过1000返回 PARAM_001 | ✅ | Design §5.1.3.3 R05 | `BubbleSortRequest.java:12` | `@Size(max=1000)` 校验 |
| order 默认 asc | ✅ | Design §5.1.3.3 R06 | `AlgorithmServiceImpl.java:78-80` | 正确默认值 |
| 支持 asc/desc | ✅ | Design §5.1.3.3 R06 | `AlgorithmServiceImpl.java:81,90` | 正确支持两种排序 |

### REQ-F04: 前端三 Tab 页面

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 三个 Tab 分别展示 | ✅ | Design §5.4.1 | `manyu_test1/index.html:60-63` | HelloWorld/哈希/冒泡排序三个 Tab |
| 切换 Tab 保留结果 | ✅ | Design §5.4.2.1 R11 | `index.html:152-159` | Tab 切换仅切换显示状态，不销毁内容 |
| 默认选中第一个 Tab | ✅ | Design §5.4.2.1 R12 | `index.html:60` | 首个 Tab 带 `active` 类 |

### REQ-F05: 导出功能

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/export 导出接口 | ✅ | Design §5.3.2 W04 | `ExportController.java:29-47` | 正确实现导出端点 |
| 支持 xlsx/csv 格式 | ✅ | Design §5.3.2 W04 入参 | `ExportServiceImpl.java:41-49` | 支持两种格式 |
| 导出内容为实际用户结果 | ❌ | Design §5.3.3.1 描述"导出各页面展示结果" | `ExportServiceImpl.java:76-101` | **使用硬编码测试数据，非用户实际输入/结果 → P0** |
| apiName=all 导出所有接口结果 | ✅ | Design §5.3.3.1 R09 | `ExportServiceImpl.java:75,84,93` | 正确实现多条件判断 |

### REQ-F06: 后端埋点

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 记录调用次数和调用人 | ✅ | Design §5.2.3.1 | `TrackingAspect.java:30-63` | AOP 切面自动记录 |
| 异步写入不影响主流程 | ✅ | Design §5.2.3.1 R07 | `TrackingServiceImpl.java:29` | `@Async("trackingExecutor")` 异步执行 |
| 埋点覆盖所有接口 | ❌ | Design §5.2.3.1 方案A"AOP拦截器" | `TrackingAspect.java:30` | **切面仅拦截 AlgorithmController，未覆盖 ExportController → P0** |
| 调用人信息从请求头获取 | ✅ | Design §5.2.3.1 R08 | `TrackingAspect.java:45-56` | 从请求头解析用户信息 |

### REQ-F07: 前端报表可视化

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 折线图/饼图/柱状图 | ✅ | Design §5.4.2.2 | `index.html:246-308` | ECharts 实现三种图表 |
| 按人员类型/层级/部门维度 | ✅ | Design §5.4.2.2 | `index.html:130-134` | 三个维度选择 |
| 无数据时空状态提示 | ✅ | Design §5.4.2.2 R13 | `index.html:310-317` | `showError` 展示提示 |
| 部门维度显示部门名称 | ❌ | Design §5.2.1.2 部门表有 dept_name | `CallRecordMapper.xml:36` | **部门维度显示数字ID而非部门名称 → P0** |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ⚠️ **A2.2** — 通配符导入 | `AlgorithmController.java:12` `ExportController.java:8` `ExportServiceImpl.java:9` `TrackingController.java:6,8`：多个文件使用 `import ...*` 通配符导入，违反阿里巴巴 Java 规范。建议改为显式单类导入。 |
| ⚠️ **A3.4** — 行宽超限 | `ExportServiceImpl.java:131,136` `TrackingAspect.java:30`：部分行超过 120 字符。建议拆分或换行。 |
| ✅ A1.1 源文件声明 | 所有文件包声明顺序正确，无多余空行 |
| ✅ A1.2 类注释 | 所有类均有 Javadoc 注释 |
| ✅ A2.1 方法注释 | 接口方法均有 Javadoc 注释 |
| ✅ A4.1 命名规范 | 类名 UpperCamelCase，方法名 lowerCamelCase，常量 UPPER_SNAKE_CASE，均符合规范 |
| ✅ A5.1 代码块 | 使用标准 4 空格缩进，花括号风格一致 |

---

## 5. Step 4 — 可靠性检查

### 自动化预扫结果（`scan-all-rules.sh`）

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 可靠性 | `reliability-checklist.md` | ⚠️ | P0 | **G16.2** CatchWithoutLogging: `AlgorithmServiceImpl.java:64`, `ExportServiceImpl.java:115,117`, `TrackingAspect.java:53,61`, `TrackingServiceImpl.java:47` |
| 安全 | `security-checklist.md` | ⚠️ | P1 | **S10.2** CorsWildcard: `WebConfig.java:16` |
| Bug 模式 | `bug-pattern-checklist.md` | ⚠️ | P1 | **M016** JavaTimeDefaultTimeZone: `AlgorithmServiceImpl.java:107`, `ExportController.java:36`, `ExportServiceImpl.java:73,127`, `TrackingServiceImpl.java:43` |

### LLM 补充审查

| 域 | 参考 | 结果 | 等级 | 说明（补扫项） |
|----|------|------|------|-----------------|
| 可靠性 | G1 并发控制 | ✅ | N/A | 异步线程池使用 `@Async`，无共享可变状态，无需显式锁 |
| 可靠性 | G2 超时/重试/限流 | ⚠️ | P0 | **G2.1** 无超时配置：`ExportServiceImpl.java` 导出大文件时无超时控制，`AlgorithmServiceImpl.java` 哈希计算无超时 |
| 可靠性 | G3 资源释放 | ✅ | N/A | 使用 try-with-resources 管理 Workbook/ByteArrayOutputStream |
| 可靠性 | G4 事务边界 | ⚠️ | P1 | `TrackingServiceImpl.java:45` 异步方法中无事务注解，若写入失败仅记录日志，数据可能丢失 |
| 可靠性 | G5 幂等 | ✅ | N/A | 当前为纯查询+INSERT，无幂等要求 |
| 可靠性 | G6 边界条件 | ❌ | P0 | `AlgorithmServiceImpl.java:61` `input.getBytes()` 使用默认字符集，跨平台不一致 → 需指定 UTF-8 |
| 可靠性 | G7 日志/监控 | ⚠️ | P1 | `AsyncConfig.java:24` 线程池拒绝处理器使用 `System.err.println` 而非 Logger，不符合生产要求 |
| 可靠性 | G8 灰度/开关 | ✅ | N/A | `application.yml` 中 `tracking.enabled` 配置可做开关 |
| 可靠性 | G9 降级/熔断 | ✅ | N/A | 异步线程池满时降级写入日志，符合设计 §6.3 |
| 安全 | S1 SQL 注入 | ✅ | N/A | MyBatis 使用 `#{}` 参数化查询，`countByDimension` 中 CASE WHEN 也是安全的 |
| 安全 | S2 认证/授权 | ⚠️ | P1 | 假设已有统一登录系统，但未实现任何登录态校验拦截器 |
| 安全 | S3 输入校验 | ✅ | N/A | 使用 `@Valid` + `@NotBlank`/`@NotEmpty`/`@Size` 进行参数校验 |
| 安全 | S4 密钥泄露 | ✅ | N/A | 无硬编码密钥 |
| 安全 | S10 CORS | ❌ | P0 | `WebConfig.java:16-19` — `allowedOriginPatterns("*")` 配合 `allowCredentials(true)` 时，浏览器会拒绝此配置（`*` 不能与 credentials 共存） |
| Bug 模式 | B009 数组越界 | ✅ | N/A | 冒泡排序使用 `array.clone()` 保护原始数组，循环边界正确 |
| Bug 模式 | B012 空指针 | ✅ | N/A | 所有参数使用前均有 null/空值检查 |
| Bug 模式 | M005 异常处理 | ⚠️ | P1 | `ExportServiceImpl.java:119` 捕获 `Exception` 后抛出 `RuntimeException`，丢失了原始异常的类型信息，建议抛出更具体的业务异常 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` | N/A | - | 未启用自定义规则 |

---

## 7. 结论

- **合并建议**：修复后合并
- **P0**：5 个阻塞性问题
  1. 导出使用硬编码测试数据而非用户实际结果
  2. 部门维度统计显示数字ID而非部门名称
  3. CORS 配置 `*` + `allowCredentials(true)` 浏览器拒绝
  4. 埋点AOP切面仅拦截 AlgorithmController，漏掉 ExportController
  5. 哈希算法 `input.getBytes()` 使用平台默认字符集
- **P1/P2**：9 个中低级别问题
- **一句话**：核心功能实现完整，但存在 5 个功能性/安全阻塞问题需修复后方可合并。

---

## 7.1 问题片段（必填）

### P0 — 导出使用硬编码测试数据

- **P0** `REQ-F05` `ExportServiceImpl.java:76-80` — 导出使用硬编码测试数据，非用户实际结果

```java
L76| if ("hello".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
L77|     HelloWorldVO hello = algorithmService.helloWorld("World");  // 硬编码
L78|     Row row = sheet.createRow(rowNum++);
L79|     row.createCell(0).setCellValue("helloworld");
L80|     row.createCell(1).setCellValue("name=World");  // 硬编码
```

### P0 — 部门维度显示数字ID

- **P0** `REQ-F07` `CallRecordMapper.xml:36` — 部门维度统计显示 user_dept_id 数字而非部门名称

```xml
L31| <select id="countByDimension" resultType="java.util.Map">
L32|     SELECT
L33|         CASE
L34|             WHEN #{dimension} = 'user_type' THEN user_type
L35|             WHEN #{dimension} = 'user_level' THEN user_level
L36|             WHEN #{dimension} = 'user_dept' THEN CAST(user_dept_id AS CHAR)  -- 显示数字ID
L37|         END AS label,
L38|         COUNT(*) AS count
L39|     FROM call_record
L40|     GROUP BY label
L41|     ORDER BY count DESC
L42| </select>
```

### P0 — CORS 配置错误

- **P0** `S10.2` `WebConfig.java:16-19` — `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 冲突，浏览器会拒绝

```java
L14| public void addCorsMappings(CorsRegistry registry) {
L15|     registry.addMapping("/api/**")
L16|             .allowedOriginPatterns("*")     // 通配符 origin
L17|             .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
L18|             .allowedHeaders("*")
L19|             .allowCredentials(true);        // 与 * 冲突
L20| }
```

### P0 — 埋点未覆盖导出接口

- **P0** `REQ-F06` `TrackingAspect.java:30` — AOP 切面仅拦截 AlgorithmController，未覆盖 ExportController

```java
L30| @AfterReturning(pointcut = "execution(* com.example.algorithmdemo.controller.AlgorithmController.*(..))", returning = "result")
L31| public void trackAlgorithmCall(JoinPoint joinPoint, Object result) {
```
应补充 `ExportController` 的拦截。

### P0 — 哈希算法默认字符集

- **P0** `G6.1` `AlgorithmServiceImpl.java:61` — `input.getBytes()` 使用平台默认字符集，跨环境不一致

```java
L59| try {
L60|     MessageDigest md = MessageDigest.getInstance(javaAlgo);
L61|     byte[] digest = md.digest(input.getBytes());  // 默认字符集，跨平台不一致
L62|     String hashValue = HexFormat.of().formatHex(digest);
```

### P1 — JavaTimeDefaultTimeZone

- **P1** `M016` `AlgorithmServiceImpl.java:107` — `LocalDateTime.now()` 使用默认时区，建议显式指定

```java
L106| private String now() {
L107|     return LocalDateTime.now().format(FORMATTER);  // 默认时区
L108| }
```

同模式也出现在 `ExportController.java:36`, `ExportServiceImpl.java:73,127`, `TrackingServiceImpl.java:43`。

### P1 — 线程池拒绝处理器使用 System.err

- **P1** `G7.1` `AsyncConfig.java:22-25` — 拒绝处理器使用 `System.err.println` 而非 Logger

```java
L22| executor.setRejectedExecutionHandler((r, e) -> {
L23|     // 线程池满时降级，不阻塞主线程
L24|     System.err.println("埋点线程池已满，任务被拒绝");  // 建议使用 Logger
L25| });
```

### P1 — 导出异常使用 RuntimeException

- **P1** `M005` `ExportServiceImpl.java:119` — 捕获通用异常后抛出 `RuntimeException`，应抛更具体的业务异常

```java
L117| } catch (Exception e) {
L118|     log.error("导出Excel失败", e);
L119|     throw new RuntimeException("导出Excel失败", e);  // 建议使用 BusinessException
L120| }
```

### P2 — 通配符导入

- **P2** `A2.2` `TrackingController.java:8` — 通配符导入

```java
L6| import java.util.*;
L8| import java.util.*;  // 重复导入
```

### P2 — 行宽超限

- **P2** `A3.4` `TrackingAspect.java:30` — 行宽超 120 字符

```java
L30| @AfterReturning(pointcut = "execution(* com.example.algorithmdemo.controller.AlgorithmController.*(..))", returning = "result")
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `ExportServiceImpl.java:76-101` — 将导出功能改为从数据库/缓存读取用户实际执行结果，而非硬编码测试数据
- [ ] **P0** `CallRecordMapper.xml:36` — 部门维度统计改为 LEFT JOIN department 表，显示 dept_name 而非 user_dept_id
- [ ] **P0** `WebConfig.java:16-19` — 修复 CORS 配置：移除 `allowCredentials(true)` 或列出具体允许的域名而非 `*`
- [ ] **P0** `TrackingAspect.java:30` — 扩展 AOP 切面点，增加对 `ExportController` 的拦截或使用更通用的包级切面
- [ ] **P0** `AlgorithmServiceImpl.java:61` — 将 `input.getBytes()` 改为 `input.getBytes(StandardCharsets.UTF_8)`

### P1

- [ ] **P1** `AlgorithmServiceImpl.java:107`, `ExportController.java:36`, `ExportServiceImpl.java:73,127`, `TrackingServiceImpl.java:43` — 指定时区，如 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))`
- [ ] **P1** `AsyncConfig.java:24` — 将拒绝处理器中的 `System.err.println` 替换为 Logger 输出
- [ ] **P1** `ExportServiceImpl.java:119` — 将 `throw new RuntimeException(...)` 改为 `throw new BusinessException(ErrorCodeConstant.SYS_001, ...)`
- [ ] **P1** `TrackingServiceImpl.java:45` — 考虑在异步方法中添加 `@Transactional` 注解保证数据一致性（需评估性能影响）

### P2

- [ ] **P2** `AlgorithmController.java:12`, `ExportController.java:8`, `ExportServiceImpl.java:9`, `TrackingController.java:6,8` — 将通配符导入替换为显式单类导入
- [ ] **P2** `ExportServiceImpl.java:131,136`, `TrackingAspect.java:30` — 拆分超长行，控制在 120 字符以内