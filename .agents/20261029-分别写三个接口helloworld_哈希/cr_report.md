# Code Review Report

> **Change** `算法演示与埋点报表系统` · **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-5fe5ac0a-586f-49ed-8c30-64d2c51d1e83` · **日期** `2026-10-29` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。**须先**运行 `scan-all-rules.sh` 并将要点并入 §5，**再**写 LLM 结论。问题须含 `path:line` 或清单 ID：可读性 `A3.4`，安全 `S1.1`，可靠性 `G16.2`，Bug 模式 `B012` / `M005` 等。**每个 ❌/⚠️ 问题在 §7 后必须附 `.java` 问题片段**（见 §7.1）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `29` |
| 变更行数 | `+1587 / -0` |

| 类/接口 | 路径 | 角色（可选） |
|---------|------|--------------|
| `AlgorithmApplication` | `src/main/java/com/example/algorithm/AlgorithmApplication.java` | 应用入口 |
| `AsyncConfig` | `src/main/java/com/example/algorithm/config/AsyncConfig.java` | 异步线程池配置 |
| `CorsConfig` | `src/main/java/com/example/algorithm/config/CorsConfig.java` | 跨域配置 |
| `AlgorithmController` | `src/main/java/com/example/algorithm/controller/AlgorithmController.java` | 算法演示控制器 |
| `ExportController` | `src/main/java/com/example/algorithm/controller/ExportController.java` | 导出功能控制器 |
| `TrackingController` | `src/main/java/com/example/algorithm/controller/TrackingController.java` | 埋点统计控制器 |
| `GlobalExceptionHandler` | `src/main/java/com/example/algorithm/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `AlgorithmCallRecordMapper` | `src/main/java/com/example/algorithm/mapper/AlgorithmCallRecordMapper.java` | 调用记录Mapper |
| `AlgorithmExportTaskMapper` | `src/main/java/com/example/algorithm/mapper/AlgorithmExportTaskMapper.java` | 导出任务Mapper |
| `ApiResponse` | `src/main/java/com/example/algorithm/model/dto/ApiResponse.java` | 通用响应封装 |
| `BubbleSortRequest` | `src/main/java/com/example/algorithm/model/dto/BubbleSortRequest.java` | 冒泡排序请求DTO |
| `BubbleSortResponse` | `src/main/java/com/example/algorithm/model/dto/BubbleSortResponse.java` | 冒泡排序响应DTO |
| `ExportRequest` | `src/main/java/com/example/algorithm/model/dto/ExportRequest.java` | 导出请求DTO |
| `ExportStatusResponse` | `src/main/java/com/example/algorithm/model/dto/ExportStatusResponse.java` | 导出状态响应DTO |
| `HashRequest` | `src/main/java/com/example/algorithm/model/dto/HashRequest.java` | 哈希请求DTO |
| `HashResponse` | `src/main/java/com/example/algorithm/model/dto/HashResponse.java` | 哈希响应DTO |
| `HelloWorldRequest` | `src/main/java/com/example/algorithm/model/dto/HelloWorldRequest.java` | HelloWorld请求DTO |
| `HelloWorldResponse` | `src/main/java/com/example/algorithm/model/dto/HelloWorldResponse.java` | HelloWorld响应DTO |
| `TrackingStatisticsResponse` | `src/main/java/com/example/algorithm/model/dto/TrackingStatisticsResponse.java` | 统计响应DTO |
| `TrendResponse` | `src/main/java/com/example/algorithm/model/dto/TrendResponse.java` | 趋势响应DTO |
| `AlgorithmCallRecord` | `src/main/java/com/example/algorithm/model/entity/AlgorithmCallRecord.java` | 调用记录实体 |
| `AlgorithmExportTask` | `src/main/java/com/example/algorithm/model/entity/AlgorithmExportTask.java` | 导出任务实体 |
| `AlgorithmService` | `src/main/java/com/example/algorithm/service/AlgorithmService.java` | 算法服务接口 |
| `ExportService` | `src/main/java/com/example/algorithm/service/ExportService.java` | 导出服务接口 |
| `TrackingService` | `src/main/java/com/example/algorithm/service/TrackingService.java` | 埋点服务接口 |
| `AlgorithmServiceImpl` | `src/main/java/com/example/algorithm/service/impl/AlgorithmServiceImpl.java` | 算法服务实现 |
| `ExportServiceImpl` | `src/main/java/com/example/algorithm/service/impl/ExportServiceImpl.java` | 导出服务实现 |
| `TrackingServiceImpl` | `src/main/java/com/example/algorithm/service/impl/TrackingServiceImpl.java` | 埋点服务实现 |
| `AlgorithmServiceTest` | `src/test/java/com/example/algorithm/service/AlgorithmServiceTest.java` | 算法服务单元测试 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 1 | 5 | 3 |

---

## 3. Step 2 — 功能（REQ）

### REQ-1: HelloWorld 接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 返回 HelloWorld 字符串 | ✅ | design.md §5.1.2 W01 | `AlgorithmController.java:33-47` `AlgorithmServiceImpl.java:22-28` | 支持自定义name参数，默认返回"Hello, World!" |
| name 长度不超过 100 | ✅ | design.md §5.1.3 R01 | `HelloWorldRequest.java:10` | @Size(max=100) 校验 |

### REQ-2: 哈希算法接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 支持 MD5/SHA-256 | ✅ | design.md §5.1.2 W02 | `AlgorithmServiceImpl.java:31-43` | MessageDigest.getInstance 动态选择算法 |
| content 不能为空 | ✅ | design.md §5.1.3 R02 | `HashRequest.java:11` | @NotBlank 校验 |
| algorithm 只能是 MD5/SHA-256 | ⚠️ | design.md §5.1.3 R03 | `AlgorithmServiceImpl.java:34-36` | 未校验非法算法，直接传入会抛 NoSuchAlgorithmException，已由 GlobalExceptionHandler 捕获处理 |

### REQ-3: 冒泡排序接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 对输入数组执行冒泡排序 | ✅ | design.md §5.1.2 W03 | `AlgorithmServiceImpl.java:47-68` | 支持 ASC/DESC |
| array 不能为空 | ✅ | design.md §5.1.3 R04 | `BubbleSortRequest.java:12` | @NotEmpty 校验 |
| order 只能是 ASC/DESC | ✅ | design.md §5.1.3 R05 | `BubbleSortRequest.java:15` | @Pattern(regexp="^(ASC|DESC)$") 校验 |

### REQ-4: 导出功能

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 提交导出任务 | ✅ | design.md §5.2.2 W04 | `ExportController.java:26-33` | 可创建导出任务 |
| 异步生成导出文件 | ❌ | design.md §5.2.3 | `ExportServiceImpl.java` | 仅创建任务记录，未实现文件生成和下载逻辑 |
| 查询导出任务状态 | ✅ | design.md §5.2.2 W05 | `ExportController.java:35-39` | 可查询状态 |
| 下载导出文件 | ❌ | design.md §5.2.2 W06 | 无实现 | 缺少下载接口实现 |

### REQ-5: 埋点功能

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 异步记录调用埋点 | ✅ | design.md §5.3.3.1 | `TrackingServiceImpl.java:37-56` | @Async 异步写入 |
| callerId 从请求头获取 | ✅ | design.md §5.3.3.1 R08 | `TrackingServiceImpl.java:58-61` | 从 X-User-Id 获取 |
| 埋点失败不阻断主流程 | ✅ | design.md §5.3.3.1 | `TrackingServiceImpl.java:53-55` | try-catch 包裹 |

### REQ-6: 报表查询

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 统计查询 | ✅ | design.md §5.3.2 W07 | `TrackingController.java:25-32` | 按维度统计 |
| 趋势查询 | ✅ | design.md §5.3.2 W08 | `TrackingController.java:34-42` | 按时间粒度查询 |
| 分布查询 | ✅ | design.md §5.3.2 W09 | `TrackingController.java:44-51` | 饼图分布数据 |

### REQ-7: 前端 Tab 页面

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 三个 Tab 展示执行结果 | N/A | design.md §1.2 | `manyu_test1/index.html` | 前端页面在另一个仓库，不在本次审查范围 |

---

## 4. Step 3 — 可读性检查

> 无 Java：**N/A**。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | UTF-8 编码，无 Tab |
| A2 | 源文件结构/import 顺序 | ⚠️ | `AlgorithmController.java:3` 通配符 import `com.example.algorithm.model.dto.*`; `AlgorithmController.java:10` `org.springframework.web.bind.annotation.*`; `ExportController.java:9` `org.springframework.web.bind.annotation.*` |
| A3 | 代码样式 | ✅ | K&R 风格，缩进 4 空格 |
| A4 | 命名规范 | ✅ | UpperCamelCase / lowerCamelCase 规范 |
| A5 | 编码实践 | ✅ | 无空 catch，@Override 正确 |
| A6 | 特定元素样式 | ✅ | 无 C 风格数组，switch 无相关 |
| A7 | Javadoc 规范 | ✅ | public 类和方法有 Javadoc |

---

## 5. Step 4 — 可靠性检查

### 5.1 Bug 模式（`bug-pattern-checklist.md`）

> **预扫结果**：`scan-all-rules.sh` 扫描 52/222 条规则，命中 9 项。

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「已扫无命中」） |
|----|------|------|------|-------------------------------------|
| Bug 模式 | `bug-pattern-checklist.md` B001-B081 | ✅ | — | 预扫无命中 Blocker 级别 Bug 模式 |
| Bug 模式 | `bug-pattern-checklist.md` M001-M027 | ✅ | — | 预扫无命中 Major 级别 Bug 模式 |
| Bug 模式 | `bug-pattern-checklist.md` I001-I010 | ✅ | — | 预扫无命中 Info 级别 Bug 模式 |

**LLM 复核补充**：
- `M004` CatchAndPrintStackTrace：未命中（无 printStackTrace 调用）。
- `M007` EmptyCatch：未命中（所有 catch 块均有处理逻辑）。
- `M027` ThreadLocalUsage：未命中（无 ThreadLocal 使用）。

### 5.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1-G1.4 | N/A | 无并发写入共享数据场景 |
| G2.1-G2.3 | N/A | 无幂等要求场景 |
| G3.1-G3.2 | ✅ | 无分布式事务，@Transactional 未使用 |
| G4.1-G4.3 | ⚠️ | `AlgorithmCallRecordMapper.java:25-33` selectStatisticsByDimension 使用 `${dimension}` 拼接 SQL，无分页 |
| G5.1 | N/A | 无 MQ 使用 |
| G6.1-G6.2 | N/A | 无缓存使用 |
| G7.1-G7.2 | N/A | 无定时任务 |
| G8.1 | ⚠️ | `AlgorithmController.java:42,58,76` catch 块内无原始异常日志记录 |
| G8.2 | ✅ | 无非核心依赖 |
| G8.3 | ✅ | 无 I/O 流未释放 |
| G8.4 | ⚠️ | `AsyncConfig.java:18-25` ThreadPoolTaskExecutor 在应用关闭时未显式 shutdown |
| G8.5 | N/A | 无 ThreadLocal 使用 |
| G8.6 | ⚠️ | `AsyncConfig.java` ThreadPoolTaskExecutor 默认使用无界队列 LinkedBlockingQueue |
| G9.1-G9.3 | N/A | 无外部 HTTP/RPC 调用 |
| G10.1-G10.2 | ✅ | 接口返回规范 |
| G11.1 | ✅ | 有单测覆盖核心逻辑 |
| G11.2 | ⚠️ | `AlgorithmServiceTest.java` 未覆盖边界值（空数组、超大数组） |
| G11.3 | ⚠️ | `TrackingController.java` 查询参数无空值防御 |
| G11.4 | N/A | 无金额计算 |
| G12.1-G12.2 | N/A | 无资金相关场景 |
| G13.1 | ✅ | 日志级别正确 |
| G14.1-G14.4 | ✅ | 无多租户/金额/时区问题 |
| G15.1-G15.3 | ✅ | 新增表兼容 |
| G16.1 | ⚠️ | 埋点有日志，但缺少指标监控（成功率/耗时/调用量） |
| G16.2 | ⚠️ | `AlgorithmController.java:42,58,76` catch 块吞异常后 throw，但 catch 块内未记录原始异常日志；`TrackingServiceImpl.java:70` getHeaderValue 中 catch Exception 过于宽泛 |
| G16.3 | ✅ | 日志级别基本正确 |
| G16.4 | ✅ | 无空 catch |
| G17.1-G17.3 | N/A | 无开关/降级设计 |

### 5.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ❌ | `AlgorithmCallRecordMapper.java:25-29` 使用 `${dimension}` 拼接 SQL，存在注入风险；`selectTrend` 中 `#{format}` 在 GROUP BY 中无法使用预编译 |
| S1.2 | ✅ | 无 order by/group by 动态字段 |
| S1.3 | N/A | 无 like/in 查询 |
| S2.1-S2.3 | N/A | 无 HTML/JS 输出 |
| S3.1-S3.3 | N/A | 无外部 URL 请求 |
| S4.1-S4.2 | ✅ | 无命令执行 |
| S5.1-S5.2 | N/A | 无 XML 解析 |
| S6.1-S6.3 | N/A | 无反序列化 |
| S7.1-S7.3 | N/A | 无文件上传 |
| S8.1 | ⚠️ | 接口未接入鉴权，仅依赖请求头传递身份 |
| S8.2 | ✅ | 增删改用 POST，查询用 GET |
| S8.3 | N/A | 无敏感数据 ID |
| S8.4 | N/A | 无 Cookie 处理 |
| S9.1 | ⚠️ | `application.yml:8` 数据库密码硬编码 |
| S9.2 | ✅ | 无敏感信息日志记录 |
| S9.3 | ✅ | 使用 HTTPS（假设） |
| S9.4 | N/A | 无随机数需求 |
| S10.1 | N/A | 无 CSRF 防护需求 |
| S10.2 | ❌ | `CorsConfig.java:20` CORS 配置 `allowedOrigins("*")` 允许所有来源 |
| S10.3 | N/A | 无 URL 跳转 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「未启用自定义规则」） |
|----|------|------|------|------------------------------------------|
| 自定义扩展 | `customized-checklist.md` U* | N/A | — | 未启用自定义规则 |

---

## 7. 结论

- **合并建议**：修复后合并（需修复 P0 问题）
- **P0**：1. SQL 注入风险（S1.1）
- **P1/P2**：5. CORS 配置、导出功能不完整、异步线程池配置、异常日志记录、密码硬编码
- **一句话**：整体功能基本实现，但存在 SQL 注入安全漏洞和导出功能不完整问题，需修复后方可合并。

---

## 7.1 问题片段（必填）

### P0 问题

- **P0** `S1.1` `src/main/java/com/example/algorithm/mapper/AlgorithmCallRecordMapper.java:25-29` — SQL 注入：使用 `${dimension}` 直接拼接 SQL 语句，dimension 参数来自用户输入，未做白名单校验。

  片段范围：`AlgorithmCallRecordMapper.java:25-33`

```java
L25|    @Select("SELECT "
L26|            + "  ${dimension} AS dimension_key, COUNT(*) AS count "
L27|            + "FROM algorithm_call_record "
L28|            + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
L29|            + "GROUP BY ${dimension}")
L30|    List<Map<String, Object>> selectStatisticsByDimension(
L31|            @Param("dimension") String dimension,
L32|            @Param("startTime") String startTime,
L33|            @Param("endTime") String endTime);
```

- **P0** `S1.1` `src/main/java/com/example/algorithm/mapper/AlgorithmCallRecordMapper.java:35-41` — SQL 注入：selectTrend 方法中 `#{format}` 在 GROUP BY 中无法使用预编译参数，实际会被 MyBatis 替换为字符串拼接。

  片段范围：`AlgorithmCallRecordMapper.java:35-46`

```java
L35|    @Select("SELECT "
L36|            + "  DATE_FORMAT(gmt_create, #{format}) AS time_label, COUNT(*) AS count "
L37|            + "FROM algorithm_call_record "
L38|            + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
L39|            + "  AND (#{algorithmType} IS NULL OR algorithm_type = #{algorithmType}) "
L40|            + "GROUP BY DATE_FORMAT(gmt_create, #{format}) "
L41|            + "ORDER BY time_label")
```

### P1 问题

- **P1** `S10.2` `src/main/java/com/example/algorithm/config/CorsConfig.java:19-20` — CORS 允许所有来源，存在安全风险。

  片段范围：`CorsConfig.java:17-24`

```java
L17|            public void addCorsMappings(CorsRegistry registry) {
L18|                registry.addMapping("/api/**")
L19|                        .allowedOrigins("*")
L20|                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
L21|                        .allowedHeaders("*")
L22|                        .maxAge(3600);
L23|            }
```

- **P1** `G16.2` `src/main/java/com/example/algorithm/controller/AlgorithmController.java:42-45` — catch 块内未记录原始异常日志，直接 throw 可能导致原始异常堆栈丢失。

  片段范围：`AlgorithmController.java:38-46`

```java
L38|        try {
L39|            String message = algorithmService.helloWorld(request);
L40|            trackingService.recordCall("/api/algorithm/helloworld", "HELLOWORLD",
L41|                    request.getName(), "SUCCESS", (int) (System.currentTimeMillis() - start));
L42|            return ApiResponse.ok(new HelloWorldResponse(message));
L43|        } catch (Exception e) {
L44|            trackingService.recordCall("/api/algorithm/helloworld", "HELLOWORLD",
L45|                    request.getName(), "FAILED", (int) (System.currentTimeMillis() - start));
L46|            throw e;
L47|        }
```

- **P1** `G8.6` `src/main/java/com/example/algorithm/config/AsyncConfig.java:18-25` — ThreadPoolTaskExecutor 未显式配置队列类型，默认使用无界队列 LinkedBlockingQueue，在高并发下可能导致 OOM。

  片段范围：`AsyncConfig.java:17-26`

```java
L17|    @Override
L18|    public Executor getAsyncExecutor() {
L19|        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
L20|        executor.setCorePoolSize(4);
L21|        executor.setMaxPoolSize(10);
L22|        executor.setQueueCapacity(100);
L23|        executor.setThreadNamePrefix("tracking-");
L24|        executor.initialize();
L25|        return executor;
L26|    }
```

- **P1** `S9.1` `src/main/resources/application.yml:7-8` — 数据库密码硬编码在配置文件中。

  片段范围：`application.yml:5-9`

```yaml
L5|spring:
L6|  datasource:
L7|    url: jdbc:mysql://localhost:3306/algorithm_demo?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
L8|    username: root
L9|    password: root
```

### P2 问题

- **P2** `A2.2` `src/main/java/com/example/algorithm/controller/AlgorithmController.java:3` — 使用通配符 import。

  片段范围：`AlgorithmController.java:1-10`

```java
L3|import com.example.algorithm.model.dto.*;
L10|import org.springframework.web.bind.annotation.*;
```

- **P2** `A2.2` `src/main/java/com/example/algorithm/controller/ExportController.java:9` — 使用通配符 import。

  片段范围：`ExportController.java:8-10`

```java
L9|import org.springframework.web.bind.annotation.*;
```

- **P2** `G11.2` `src/test/java/com/example/algorithm/service/AlgorithmServiceTest.java` — 单元测试未覆盖边界值场景（空数组、超大数组、非法算法名称等）。

---

## 8. 修复任务列表

### P0

- [ ] **P0** `S1.1` `AlgorithmCallRecordMapper.java:25-29` — 将 `${dimension}` 改为白名单校验后拼接，或使用 `<choose><when>` 枚举支持的维度字段，避免 SQL 注入。
- [ ] **P0** `S1.1` `AlgorithmCallRecordMapper.java:35-41` — 将 `#{format}` 在 GROUP BY 中的使用改为服务端预定义的时间粒度到 DATE_FORMAT 格式的映射，避免 SQL 注入。

### P1

- [ ] **P1** `S10.2` `CorsConfig.java:20` — 将 `allowedOrigins("*")` 改为具体的域名白名单。
- [ ] **P1** `G16.2` `AlgorithmController.java:42-46` — 在 catch 块内添加 `LOG.error("Algorithm execution failed", e);` 记录原始异常。
- [ ] **P1** `G8.6` `AsyncConfig.java:22` — 显式配置有界队列，如 `new LinkedBlockingQueue<>(100)`，或改用 `ThreadPoolExecutor` 显式构造。
- [ ] **P1** `S9.1` `application.yml:9` — 将数据库密码从配置文件中提取，使用环境变量或配置中心管理。
- [ ] **P1** `功能缺失` — 补充 ExportServiceImpl 的文件生成和下载逻辑，或补充缺失的下载接口 Controller。

### P2（可选）

- [ ] **P2** `A2.2` `AlgorithmController.java:3,10` — 将通配符 import 替换为具体类 import。
- [ ] **P2** `A2.2` `ExportController.java:9` — 将通配符 import 替换为具体类 import。
- [ ] **P2** `G11.2` `AlgorithmServiceTest.java` — 补充边界值测试：空数组、超大数组（>10000）、非法算法名称等。
