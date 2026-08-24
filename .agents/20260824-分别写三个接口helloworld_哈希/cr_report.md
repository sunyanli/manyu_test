# Code Review Report

> **Change** 分别写三个接口helloworld、哈希算法以及冒泡排序 · **分支/Commit** `AI/task-DEV-9d10e310-7901-11f1-8a9f-59ecae612580-97503bfa-3cbb-44e3-95f8-0ee1face2899` / `HEAD` · **日期** 2026-08-24 · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准。已运行 `scan-all-rules.sh` 并将要点并入 §5。问题含 `path:line` 或清单 ID。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | 18 |
| 变更行数 | `+~800 / -0` |

| 类/接口 | 路径 | 角色 |
|---------|------|--------------|
| AlgorithmDemoApplication | `src/main/java/.../AlgorithmDemoApplication.java` | 启动类 |
| ApiNameConstant | `src/main/java/.../common/constant/ApiNameConstant.java` | 接口名称常量 |
| ErrorCodeConstant | `src/main/java/.../common/constant/ErrorCodeConstant.java` | 错误码常量 |
| BusinessException | `src/main/java/.../common/exception/BusinessException.java` | 业务异常 |
| GlobalExceptionHandler | `src/main/java/.../common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| AsyncConfig | `src/main/java/.../config/AsyncConfig.java` | 异步线程池配置 |
| WebConfig | `src/main/java/.../config/WebConfig.java` | CORS 配置 |
| AlgorithmController | `src/main/java/.../controller/AlgorithmController.java` | 算法接口控制器 |
| ExportController | `src/main/java/.../controller/ExportController.java` | 导出接口控制器 |
| CallRecordMapper | `src/main/java/.../dao/mapper/CallRecordMapper.java` | MyBatis Mapper |
| BubbleSortRequest | `src/main/java/.../model/dto/BubbleSortRequest.java` | 冒泡排序请求 DTO |
| HashRequest | `src/main/java/.../model/dto/HashRequest.java` | 哈希算法请求 DTO |
| CallRecord | `src/main/java/.../model/entity/CallRecord.java` | 调用记录实体 |
| Department | `src/main/java/.../model/entity/Department.java` | 部门实体 |
| BubbleSortVO | `src/main/java/.../model/vo/BubbleSortVO.java` | 冒泡排序返回 VO |
| HashVO | `src/main/java/.../model/vo/HashVO.java` | 哈希算法返回 VO |
| HelloWorldVO | `src/main/java/.../model/vo/HelloWorldVO.java` | HelloWorld 返回 VO |
| AlgorithmService | `src/main/java/.../service/AlgorithmService.java` | 算法服务接口 |
| AlgorithmServiceImpl | `src/main/java/.../service/impl/AlgorithmServiceImpl.java` | 算法服务实现 |
| ExportService | `src/main/java/.../service/ExportService.java` | 导出服务接口 |
| ExportServiceImpl | `src/main/java/.../service/impl/ExportServiceImpl.java` | 导出服务实现 |
| TrackingAspect | `src/main/java/.../tracking/aop/TrackingAspect.java` | 埋点 AOP 切面 |
| TrackingController | `src/main/java/.../tracking/controller/TrackingController.java` | 埋点统计控制器 |
| TrackingService | `src/main/java/.../tracking/service/TrackingService.java` | 埋点追踪服务接口 |
| TrackingServiceImpl | `src/main/java/.../tracking/service/impl/TrackingServiceImpl.java` | 埋点追踪服务实现 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 5 | 5 | 5 |

---

## 3. Step 2 — 功能（REQ）

### REQ-F01: HelloWorld 接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/hello 返回问候语 | ✅ | §5.1.2 W01 | `AlgorithmController.java:32-35` | 接口路径、入参出参匹配设计 |
| 空 name 参数默认 "World" | ✅ | §5.1.2 W01 | `AlgorithmServiceImpl.java:29-31` | 默认值处理正确 |
| name 长度校验 > 100 返回 PARAM_001 | ✅ | §5.1.3.1 R01 | `AlgorithmServiceImpl.java:32-33` | 参数校验实现 |
| 返回格式含 message + timestamp | ✅ | §5.1.2 W01 data 结构 | `AlgorithmServiceImpl.java:36` | 返回结构匹配 |

### REQ-F02: 哈希算法接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/hash 计算哈希值 | ✅ | §5.1.2 W02 | `AlgorithmController.java:41-44` | 接口路径、入参出参匹配 |
| 支持 MD5/SHA-256/SHA-512 | ✅ | §5.1.2 W02 业务规则 | `AlgorithmServiceImpl.java:44-58` | 三种算法支持 |
| 默认算法 SHA-256 | ✅ | §5.1.2 W02 入参 | `AlgorithmServiceImpl.java:41-43` | 默认值处理 |
| 不支持的算法返回 ALGO_001 | ✅ | §5.1.2 W02 错误码 | `AlgorithmServiceImpl.java:57,66` | 异常处理 |
| input 为空校验 | ✅ | §5.1.3.2 R02 | `HashRequest.java:11` | `@NotBlank` 注解 |
| input 长度不超过 10KB | ✅ | §5.1.3.2 异常场景 | `HashRequest.java:12` | `@Size(max=10240)` 注解 |

### REQ-F03: 冒泡排序接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/bubble-sort 排序 | ✅ | §5.1.2 W03 | `AlgorithmController.java:50-53` | 接口路径匹配 |
| 升序/降序支持 | ✅ | §5.1.2 W03 入参 | `AlgorithmServiceImpl.java:78-81` | order 参数处理 |
| 默认升序 | ✅ | §5.1.2 W03 入参 | `AlgorithmServiceImpl.java:79` | 默认值处理 |
| 数组为空校验 | ✅ | §5.1.3.3 R04 | `BubbleSortRequest.java:11` | `@NotEmpty` 注解 |
| 数组长度 ≤ 1000 | ✅ | §5.1.3.3 R05 | `BubbleSortRequest.java:12` | `@Size(max=1000)` 注解 |
| 返回含 originalArray + sortedArray + sortTime | ✅ | §5.1.2 W03 data 结构 | `AlgorithmServiceImpl.java:102-103` | 返回结构匹配 |

### REQ-F04: 前端三 Tab 页面

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 三个 Tab 分别展示三个接口 | ✅ | §5.4.1 页面布局 | `manyu_test1/index.html:60-63` | Tab 按钮实现 |
| Tab 内容区域独立展示结果 | ✅ | §5.4.1 页面布局 | `manyu_test1/index.html:66-104` | 三个独立内容区 |

### REQ-F05: 导出功能

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 导出接口 GET /api/export | ✅ | §5.3.2 W04 | `ExportController.java:29-30` | 接口路径匹配 |
| 支持 xlsx 和 csv 格式 | ✅ | §5.3.2 W04 入参 | `ExportServiceImpl.java:41-43` | 格式校验 |
| 支持按接口名导出 | ✅ | §5.3.2 W04 入参 | `ExportServiceImpl.java:75-101` | apiName 过滤 |
| 导出文件名格式 | ✅ | §5.3.2 W04 出参 | `ExportController.java:36-37` | 文件名格式匹配 |
| **❌ 导出使用硬编码测试数据** | ❌ | §5.3.3.1 时序图显示调用实际服务 | `ExportServiceImpl.java:76,85,94` | 应导出用户实际结果而非测试数据 |

### REQ-F06: 后端埋点

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| AOP 切面记录埋点 | ✅ | §5.2.3.1 方案A | `TrackingAspect.java:30` | AOP 拦截器实现 |
| 异步写入数据库 | ✅ | §5.2.3.1 R07 | `TrackingServiceImpl.java:29` | `@Async` 注解 |
| 记录调用人和次数 | ✅ | §5.2.3.1 时序图 | `TrackingServiceImpl.java:35-43` | 组装 CallRecord 对象 |
| **❌ AOP 切面仅拦截 AlgorithmController** | ❌ | §5.2.2 W05/W06 埋点应覆盖所有接口 | `TrackingAspect.java:30` | 未覆盖 ExportController 和 TrackingController |

### REQ-F07: 前端报表可视化

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 折线图/饼图/柱状图 | ✅ | §5.4.2.2 图表类型切换 | `manyu_test1/index.html:256-308` | ECharts 三种图表实现 |
| 维度选择（人员类型/层级/部门） | ✅ | §5.4.2.2 维度选择 | `manyu_test1/index.html:130-134` | 维度下拉选择 |
| 统计接口 GET /api/tracking/statistics | ✅ | §5.2.2 W05 | `TrackingController.java:26-75` | 接口实现 |
| 统计接口支持维度参数 | ✅ | §5.2.2 W05 入参 | `TrackingController.java:33-35` | 维度校验 |
| **❌ 部门维度显示 ID 而非名称** | ❌ | §5.2.2 W05 响应示例显示部门名称 | `CallRecordMapper.xml:36` | 应 JOIN department 表取 dept_name |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明 |
|------|------|
| ❌ A2.2 — 通配符导入 | `AlgorithmController.java:12` `import org.springframework.web.bind.annotation.*;` |
| ❌ A2.2 — 通配符导入 | `ExportController.java:8` `import org.springframework.web.bind.annotation.*;` |
| ❌ A2.2 — 通配符导入 | `ExportServiceImpl.java:9` `import org.apache.poi.ss.usermodel.*;` |
| ❌ A2.2 — 通配符导入 | `TrackingController.java:6` `import org.springframework.web.bind.annotation.*;` |
| ❌ A2.2 — 通配符导入 | `TrackingController.java:8` `import java.util.*;` |
| ❌ A3.4 — 行宽超限 | `ExportServiceImpl.java:131` — CSV 拼接行超 120 字符 |
| ❌ A3.4 — 行宽超限 | `ExportServiceImpl.java:136` — CSV 拼接行超 120 字符 |
| ❌ A3.4 — 行宽超限 | `TrackingAspect.java:30` — 切点表达式超 120 字符 |
| ✅ A1 | 源文件格式、编码 UTF-8 均正确 |
| ✅ A2.1 | 文件结构符合 package → import → 类 |
| ✅ A2.3 | import 分组符合要求（静态/非静态分组） |
| ✅ A3.1 | K&R 大括号风格正确 |
| ✅ A3.3 | 缩进 4 空格 |
| ✅ A4 | 命名规范符合（包名/类名/方法名/常量） |
| ✅ A5.1 | 重写方法均有 `@Override` |
| ✅ A6 | 数组方括号类型风格正确 |
| ✅ A7 | public 类有 Javadoc 注释 |

---

## 5. Step 4 — 可靠性检查

### 自动化预扫结果

已执行 `scan-all-rules.sh`，覆盖 52/222 条规则，发现 20 个问题（6 P0 / 6 P1 / 8 P2）。以下为合并结果：

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 可靠性 | G1 并发控制 | N/A | — | 无并发场景代码 |
| 可靠性 | G2 幂等拦截 | N/A | — | 非写接口无幂等需求 |
| 可靠性 | G3 事务控制 | N/A | — | 无 `@Transactional` 使用 |
| 可靠性 | G4 SQL与索引 | ✅ | — | 使用 `#{}` 预编译，无隐式转换 |
| 可靠性 | G5 消息 | N/A | — | 无 MQ 使用 |
| 可靠性 | G6 缓存 | N/A | — | 无缓存使用 |
| 可靠性 | G7 调度任务 | N/A | — | 无调度任务 |
| 可靠性 | G8 防御编程 | ❌ | P1 | 见下方 G8.1 |
| 可靠性 | G9 网络调用 | N/A | — | 无外部 RPC/HTTP 调用 |
| 可靠性 | G10 接口契约 | ✅ | — | 字段含义明确 |
| 可靠性 | G11 开发自测 | ❌ | P2 | 无单测文件 |
| 可靠性 | G12 资损防控 | N/A | — | 非资金场景 |
| 可靠性 | G13 监控核对 | ✅ | — | 日志级别正确 |
| 可靠性 | G14 国际化/时区 | ❌ | P1 | 见下方 G14.4 |
| 可靠性 | G15 可灰度 | ✅ | — | 表结构向前兼容（新增表） |
| 可靠性 | G16 可监控 | ❌ | P1 | 见下方 G16.2/G16.3 |
| 可靠性 | G17 可应急 | N/A | — | 非生产级功能 |
| 安全 | S1 SQL注入 | ✅ | — | 全部使用 `#{}` 预编译 |
| 安全 | S2 XSS | N/A | — | 不涉及 HTML 输出 |
| 安全 | S3 SSRF | N/A | — | 无外部 URL 请求 |
| 安全 | S4 命令执行 | N/A | — | 无命令执行 |
| 安全 | S5 XXE | N/A | — | 无 XML 解析 |
| 安全 | S6 反序列化 | N/A | — | 无反序列化 |
| 安全 | S7 文件上传 | N/A | — | 无文件上传 |
| 安全 | S8 访问控制 | N/A | — | 假设已有统一登录 |
| 安全 | S9 数据安全 | N/A | — | 无非敏感数据 |
| 安全 | **S10 CSRF/CORS** | **❌** | **P0** | 见下方 S10.2 |
| Bug 模式 | B/M/I 清单 | ❌ | P1 | 见下方 M016 |

### 详细问题清单

#### 可靠性 — G8.1 防御编程

- **P1** `G8.1` `ExportServiceImpl.java:115-120` — `catch (BusinessException e) { throw e; }` 后直接捕获 `Exception` 并包装为 `RuntimeException`，未保留原始异常链中全部上下文。

#### 可靠性 — G14.4 时区

- **P1** `G14.4` `AlgorithmServiceImpl.java:107` — `LocalDateTime.now()` 使用系统默认时区，未显式指定时区，跨时区部署时行为不一致。
- **P1** `G14.4` `ExportController.java:36` — `LocalDateTime.now()` 使用系统默认时区。
- **P1** `G14.4` `ExportServiceImpl.java:73,127` — `LocalDateTime.now()` 使用系统默认时区。
- **P1** `G14.4` `TrackingServiceImpl.java:43` — `LocalDateTime.now()` 使用系统默认时区。

#### 可靠性 — G16.2 异常日志

- **P1** `G16.2` `ExportServiceImpl.java:115-116` — `catch (BusinessException e) { throw e; }` 捕获业务异常后直接重抛，未记录日志，排障时缺少上下文。

#### 可靠性 — G16.3 日志级别

- **P1** `G16.3` `AsyncConfig.java:24` — 线程池拒绝处理使用 `System.err.println` 而非 logger，不符合日志规范。

#### 安全 — S10.2 CORS 配置

- **P0** `S10.2` `WebConfig.java:16` — `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 同时使用。浏览器规范规定：当 `Access-Control-Allow-Credentials=true` 时，`Access-Control-Allow-Origin` 不能为 `*`。Chrome/Firefox 会直接拒绝请求。

#### Bug 模式 — M016 JavaTimeDefaultTimeZone

- **P1** `M016` `AlgorithmServiceImpl.java:107` — 使用默认时区时间。
- **P1** `M016` `ExportController.java:36` — 使用默认时区时间。
- **P1** `M016` `ExportServiceImpl.java:73` — 使用默认时区时间。
- **P1** `M016` `ExportServiceImpl.java:127` — 使用默认时区时间。
- **P1** `M016` `TrackingServiceImpl.java:43` — 使用默认时区时间。

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` | N/A | — | 未启用自定义规则（文件仅为示例项） |

---

## 7. 结论

- **合并建议**：阻止合并（DO NOT MERGE）
- **P0**：
  1. CORS 配置 `allowedOriginPatterns("*")` + `allowCredentials(true)` 冲突，浏览器拒绝请求
  2. 导出使用硬编码测试数据，非实际用户结果
  3. 部门维度统计显示数字 ID 而非部门名称
  4. 埋点 AOP 切面仅拦截 AlgorithmController，未覆盖 ExportController
  5. 哈希算法 `input.getBytes()` 使用平台默认字符集，跨环境不一致
- **P1/P2**：见 §5 详细问题清单
- **一句话**：功能主体实现基本正确，但存在 5 个 P0 阻塞问题（CORS 冲突、导出数据硬编码、部门维度显示 ID 未 JOIN 名称、埋点覆盖不全、字符集跨环境不一致），需修复后方可合并。

---

## 7.1 问题片段（必填）

### P0 — S10.2 CORS 配置冲突

- **P0** `S10.2` `WebConfig.java:16` — `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 同时使用，浏览器拒绝。
  片段范围：`WebConfig.java:14-20`

```java
L14|    public void addCorsMappings(CorsRegistry registry) {
L15|        registry.addMapping("/api/**")
L16|                .allowedOriginPatterns("*")   // ← 问题：与 allowCredentials(true) 冲突
L17|                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
L18|                .allowedHeaders("*")
L19|                .allowCredentials(true);      // ← 冲突：credentials=true 时 origin 不能为 *
L20|    }
```

### P0 — 导出使用硬编码测试数据

- **P0** `ExportServiceImpl.java:76,85,94` — 导出调用使用硬编码测试数据（`name="World"`、`input="Hello World"`、`testArray={5,3,8,4,2}`），非用户实际执行结果。
  片段范围：`ExportServiceImpl.java:73-101`

```java
L75|            if ("hello".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
L76|                HelloWorldVO hello = algorithmService.helloWorld("World");  // ← 硬编码
L77|                Row row = sheet.createRow(rowNum++);
L78|                row.createCell(0).setCellValue("helloworld");
L79|                row.createCell(1).setCellValue("name=World");
L80|                row.createCell(2).setCellValue(hello.getMessage());
L81|                row.createCell(3).setCellValue(exportTime);
L82|            }
```

### P0 — 部门维度统计显示数字 ID

- **P0** `CallRecordMapper.xml:36` — `user_dept` 维度时显示 `CAST(user_dept_id AS CHAR)` 而非部门名称，应与 `department` 表 JOIN。
  片段范围：`CallRecordMapper.xml:31-42`

```xml
L31|    <select id="countByDimension" resultType="java.util.Map">
L32|        SELECT
L33|            CASE
L34|                WHEN #{dimension} = 'user_type' THEN user_type
L35|                WHEN #{dimension} = 'user_level' THEN user_level
L36|                WHEN #{dimension} = 'user_dept' THEN CAST(user_dept_id AS CHAR)  -- ← 显示数字ID
L37|            END AS label,
L38|            COUNT(*) AS count
L39|        FROM call_record
L40|        GROUP BY label
L41|        ORDER BY count DESC
L42|    </select>
```

### P0 — 埋点 AOP 切面覆盖不全

- **P0** `TrackingAspect.java:30` — 切点仅覆盖 `AlgorithmController.*(..)`，未覆盖 `ExportController` 和 `TrackingController`。
  片段范围：`TrackingAspect.java:30-31`

```java
L30|    @AfterReturning(pointcut = "execution(* com.example.algorithmdemo.controller.AlgorithmController.*(..))", returning = "result")
L31|    public void trackAlgorithmCall(JoinPoint joinPoint, Object result) {
```

### P0 — 哈希算法使用平台默认字符集

- **P0** `AlgorithmServiceImpl.java:61` — `input.getBytes()` 使用平台默认字符集，跨环境（Linux/Windows）运行结果不一致。
  片段范围：`AlgorithmServiceImpl.java:59-63`

```java
L59|        try {
L60|            MessageDigest md = MessageDigest.getInstance(javaAlgo);
L61|            byte[] digest = md.digest(input.getBytes());  // ← 使用平台默认字符集
L62|            String hashValue = HexFormat.of().formatHex(digest);
L63|            return new HashVO(input, algorithm, hashValue, now());
```

### P1 — G14.4 时区未显式指定

- **P1** `G14.4` `AlgorithmServiceImpl.java:107` — `LocalDateTime.now()` 使用系统默认时区。
  片段范围：`AlgorithmServiceImpl.java:106-108`

```java
L106|    private String now() {
L107|        return LocalDateTime.now().format(FORMATTER);  // ← 未指定时区
L108|    }
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `WebConfig.java:16` — 将 `allowedOriginPatterns("*")` 改为具体域名白名单，或在不需要 credentials 时移除 `allowCredentials(true)`。
- [ ] **P0** `ExportServiceImpl.java:76,85,94` — 改为接收用户实际输入参数并调用算法服务获取结果，而非硬编码测试数据。
- [ ] **P0** `CallRecordMapper.xml:36` — `user_dept` 维度改为 LEFT JOIN `department` 表取 `dept_name` 作为 label。
- [ ] **P0** `TrackingAspect.java:30` — 扩展切点表达式，覆盖 `ExportController` 和 `TrackingController` 的埋点。
- [ ] **P0** `AlgorithmServiceImpl.java:61` — 将 `input.getBytes()` 改为 `input.getBytes(StandardCharsets.UTF_8)` 确保跨环境一致性。

### P1

- [ ] **P1** `AlgorithmServiceImpl.java:107` / `ExportController.java:36` / `ExportServiceImpl.java:73,127` / `TrackingServiceImpl.java:43` — 使用 `ZonedDateTime.now(ZoneOffset.UTC)` 或显式指定时区，避免跨时区问题。
- [ ] **P1** `AsyncConfig.java:24` — 将 `System.err.println` 替换为 logger 输出。
- [ ] **P1** `ExportServiceImpl.java:115-116` — 在 `catch (BusinessException e)` 分支中添加日志记录。

### P2（可选）

- [ ] **P2** `AlgorithmController.java:12` / `ExportController.java:8` / `ExportServiceImpl.java:9` / `TrackingController.java:6,8` — 将通配符导入替换为具体导入。
- [ ] **P2** `ExportServiceImpl.java:131,136` / `TrackingAspect.java:30` — 缩短超长行至 ≤120 字符。
- [ ] **P2** — 添加单元测试，覆盖正常流程、边界条件和异常场景。