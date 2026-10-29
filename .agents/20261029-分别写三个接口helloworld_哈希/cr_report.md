# Code Review Report

> **Change** `分别写三个接口helloworld_哈希` · **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-5fe5ac0a-586f-49ed-8c30-64d2c51d1e83` / `632b241` · **日期** `2026-10-29` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。**须先**运行 `scan-all-rules.sh` 并将要点并入 §5，**再**写 LLM 结论。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | 13 |
| 变更行数 | `+252 / -55` |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| AsyncConfig | `config/AsyncConfig.java` | 异步线程池配置 |
| CorsConfig | `config/CorsConfig.java` | 跨域配置 |
| AlgorithmController | `controller/AlgorithmController.java` | 算法演示接口 |
| ExportController | `controller/ExportController.java` | 导出接口 |
| TrackingController | `controller/TrackingController.java` | 埋点统计查询接口 |
| AlgorithmCallRecordMapper | `mapper/AlgorithmCallRecordMapper.java` | 埋点记录 Mapper |
| AlgorithmCallRecordSqlProvider | `mapper/AlgorithmCallRecordSqlProvider.java` | 动态 SQL Provider |
| ExportService | `service/ExportService.java` | 导出服务接口 |
| ExportTaskService | `service/ExportTaskService.java` | 导出异步任务服务 |
| TrackingService | `service/TrackingService.java` | 埋点服务接口 |
| ExportServiceImpl | `service/impl/ExportServiceImpl.java` | 导出服务实现 |
| TrackingServiceImpl | `service/impl/TrackingServiceImpl.java` | 埋点服务实现 |
| AlgorithmServiceTest | `service/AlgorithmServiceTest.java` | 算法服务单测 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 1 | 4 | 3 |

---

## 3. Step 2 — 功能（REQ）

### REQ-1: HelloWorld 接口 (F01)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/algorithm/helloworld 返回 HelloWorld 字符串 | ✅ | design.md §5.1.3.1 | `AlgorithmController.java:42-57` + `AlgorithmServiceImpl.java:22-28` | 已实现；参数校验通过 `@Valid` + `HelloWorldRequest` 完成 |

### REQ-2: 哈希算法接口 (F02)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/algorithm/hash 返回哈希值 | ✅ | design.md §5.1.3.2 | `AlgorithmController.java:59-74` + `AlgorithmServiceImpl.java:31-44` | 已实现；支持 MD5/SHA-256；默认 SHA-256 |

### REQ-3: 冒泡排序接口 (F03)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/algorithm/bubbleSort 返回排序结果 | ✅ | design.md §5.1.3.3 | `AlgorithmController.java:76-93` + `AlgorithmServiceImpl.java:47-68` | 已实现；支持 ASC/DESC |

### REQ-4: 导出功能 (F05)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/export/result 创建异步导出任务 | ✅ | design.md §5.2.3.1 | `ExportController.java:36-43` + `ExportServiceImpl.java:32-43` | 已实现异步导出；但未实现 "同一用户并发导出任务数不超过 5 个" 的约束 (R07) |
| GET /api/export/status/{taskId} 查询导出状态 | ✅ | design.md §5.2.2 | `ExportController.java:45-49` + `ExportServiceImpl.java:46-52` | 已实现 |
| GET /api/export/download/{taskId} 下载导出文件 | ⚠️ | design.md §5.2.2 | `ExportController.java:51-58` + `ExportServiceImpl.java:55-65` | 已实现；但 catch IOException 无日志 (见 §7.1) |

### REQ-5: 后端调用埋点 (F06)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 算法接口调用后异步记录埋点 | ✅ | design.md §5.3.3.1 | `AlgorithmController.java:95-104` + `TrackingServiceImpl.java:36-56` | 已实现异步埋点；callerId 为空时默认 "UNKNOWN" |

### REQ-6: 报表可视化查询 (F07/F08)

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/tracking/statistics 按维度统计 | ✅ | design.md §5.3.2 | `TrackingController.java:25-32` + `TrackingServiceImpl.java:58-69` | 已实现；dimension 经过白名单校验 |
| GET /api/tracking/trend 按时间粒度趋势 | ✅ | design.md §5.3.2 | `TrackingController.java:34-42` + `TrackingServiceImpl.java:71-87` | 已实现；但 timeGranularity 未做非空校验 |
| GET /api/tracking/distribution 维度分布 | ✅ | design.md §5.3.2 | `TrackingController.java:44-51` + `TrackingServiceImpl.java:89-109` | 已实现；含占比计算 |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ⚠️ P2 | `A3.4` `TrackingServiceImpl.java:38` — `@Async` 方法签名行宽超过 120 字符 |
| ⚠️ P2 | `A3.4` `TrackingServiceImpl.java:61` — SQL 语句拼接行宽超过 120 字符 |
| ⚠️ P2 | `A3.4` `TrackingServiceImpl.java:92` — SQL 语句拼接行宽超过 120 字符 |
| ✅ | 其他 A1-A7 项均符合规范 |

---

## 5. Step 4 — 可靠性检查

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「已扫无命中」） |
|----|------|------|------|-------------------------------------|
| 可靠性 | `reliability-checklist.md` G1–G17 | ⚠️ | P0/P1/P2 | G16.2 命中 1 处；G11.3 命中 2 处；其余未命中 |
| 安全 | `security-checklist.md` S1–S10 | ✅ | — | S1 通过白名单校验；S9 无硬编码密钥；其余未命中 |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I（120） | ⚠️ | P1/P2 | M016 命中 2 处；A3.4 命中 3 处；其余未命中 |

### 5.1 Bug 模式明细

| ID | 状态 | 备注 |
|----|------|------|
| B001–B081 | ✅ | 已扫无命中 |
| M016 | ❌ | `ExportTaskService.java:55,61` — `LocalDateTime.now()` 未显式指定时区 |
| M003–M015, M017–M027 | ✅ | 已扫无命中 |
| I001–I010 | ✅ | 已扫无命中 |

### 5.2 可靠性明细

| ID | 状态 | 备注 |
|----|------|------|
| G16.2 | ❌ | `ExportServiceImpl.java:62` — catch IOException 无日志记录 |
| G11.3 | ⚠️ | `TrackingController.java:26` — queryStatistics 入参无 `@Valid` 校验，dimension/startTime/endTime 可能为空 |
| G11.3 | ⚠️ | `TrackingController.java:34` — queryTrend 入参无 `@Valid` 校验，timeGranularity 可能为 null 导致 NPE (`TrackingServiceImpl.java:73`) |
| G4.3 | ⚠️ | `AlgorithmCallRecordMapper.java:30-35` — 统计查询无分页，大数据量可能 OOM |
| 其他 G | ✅ | 已扫无命中 |

### 5.3 安全明细

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | SQL 参数均使用 `#{}` 预编译；动态列名通过白名单校验 (`AlgorithmCallRecordSqlProvider.java:46-50`) |
| S10.2 | ✅ | CORS 配置使用 `cors.allowed-origins` 配置变量，非通配符 |
| 其他 S | ✅ | 未命中 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | N/A | — | 未启用自定义规则 |

---

## 7. 结论

- **合并建议**：修复后合并
- **P0**：
  1. `ExportServiceImpl.java:62` — catch IOException 无日志记录，异常被静默吞掉，排障困难
- **P1**：
  1. `ExportTaskService.java:55,61` — `LocalDateTime.now()` 未显式指定时区（M016）
  2. `TrackingController.java:26,34` — 查询接口入参缺少校验，存在 NPE 风险（G11.3）
  3. `AlgorithmCallRecordMapper.java:30-35` — 统计查询无分页，大数据量可能 OOM（G4.3）
- **P2**：
  1. `TrackingServiceImpl.java:38,61,92` — 行宽超过 120 字符（A3.4）
- **一句话**：整体代码结构清晰，功能基本实现符合 spec；但存在 1 处 P0 异常日志缺失和若干 P1 入参校验不足、查询未分页的问题，修复后方可合并。

---

## 7.1 问题片段（必填）

- **P0** `G16.2` `src/main/java/com/example/algorithm/service/impl/ExportServiceImpl.java:62` — catch IOException 无日志记录，异常被静默吞掉。  
  片段范围：`src/main/java/com/example/algorithm/service/impl/ExportServiceImpl.java:55-66`

```java
L55|    @Override
L56|    public InputStream downloadExportFile(String taskId) {
L57|        AlgorithmExportTask task = exportTaskMapper.selectByTaskId(taskId);
L58|        if (task == null || task.getFileUrl() == null) {
L59|            return new ByteArrayInputStream(new byte[0]);
L60|        }
L61|        try {
L62|            return new FileInputStream(new File(task.getFileUrl()));
L63|        } catch (IOException e) {
L64|            return new ByteArrayInputStream(new byte[0]);
L65|        }
L66|    }
```

- **P1** `M016` `src/main/java/com/example/algorithm/service/ExportTaskService.java:55` — `LocalDateTime.now()` 未显式指定时区。  
  片段范围：`src/main/java/com/example/algorithm/service/ExportTaskService.java:49-57`

```java
L49|    private void updateTaskStatus(String taskId, String status, String fileUrl, String errorMsg) {
L50|        AlgorithmExportTask task = new AlgorithmExportTask();
L51|        task.setTaskId(taskId);
L52|        task.setStatus(status);
L53|        task.setFileUrl(fileUrl);
L54|        task.setErrorMsg(errorMsg);
L55|        task.setGmtModified(LocalDateTime.now());
L56|        exportTaskMapper.updateStatus(task);
L57|    }
```

- **P1** `G11.3` `src/main/java/com/example/algorithm/controller/TrackingController.java:34` — queryTrend 入参未校验，timeGranularity 为 null 时 `toUpperCase()` 会 NPE。  
  片段范围：`src/main/java/com/example/algorithm/controller/TrackingController.java:34-42`

```java
L34|    @GetMapping("/trend")
L35|    public ApiResponse<TrendResponse> queryTrend(
L36|            @RequestParam String timeGranularity,
L37|            @RequestParam String startTime,
L38|            @RequestParam String endTime,
L39|            @RequestParam(required = false) String algorithmType) {
L40|        TrendResponse response = trackingService.queryTrend(timeGranularity, startTime, endTime, algorithmType);
L41|        return ApiResponse.ok(response);
L42|    }
```

对应问题路径：
```java
L72|    public TrendResponse queryTrend(String timeGranularity, String startTime, String endTime, String algorithmType) {
L73|        String format = switch (timeGranularity.toUpperCase()) {
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `src/main/java/com/example/algorithm/service/impl/ExportServiceImpl.java:62` — catch IOException 块内添加日志记录：`LOG.error("Failed to download export file for task: {}", taskId, e);`

### P1

- [ ] **P1** `M016` `src/main/java/com/example/algorithm/service/ExportTaskService.java:55,61` — `LocalDateTime.now()` 替换为 `LocalDateTime.now(ZoneId.systemDefault())` 或其他显式时区
- [ ] **P1** `G11.3` `src/main/java/com/example/algorithm/controller/TrackingController.java:26` — queryStatistics 入参添加 `@NotBlank` 或 `required = false` 配合非空校验
- [ ] **P1** `G11.3` `src/main/java/com/example/algorithm/controller/TrackingController.java:34` — queryTrend 入参 `timeGranularity` 添加非空校验或空值防御
- [ ] **P1** `G4.3` `src/main/java/com/example/algorithm/mapper/AlgorithmCallRecordMapper.java:30-35` — 统计查询添加 LIMIT 分页或最大返回条数限制

### P2（可选）

- [ ] **P2** `A3.4` `src/main/java/com/example/algorithm/service/impl/TrackingServiceImpl.java:38` — 拆分超长方法签名至多行
- [ ] **P2** `A3.4` `src/main/java/com/example/algorithm/service/impl/TrackingServiceImpl.java:61` — 拆分超长 SQL 拼接语句
- [ ] **P2** `A3.4` `src/main/java/com/example/algorithm/service/impl/TrackingServiceImpl.java:92` — 拆分超长 SQL 拼接语句
