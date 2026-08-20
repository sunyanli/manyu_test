# Code Review Checklist

> **Change** 三示例接口（helloworld/哈希/冒泡排序）+ 三 Tab 页面 + 导出 + 埋点统计报表 · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-fea6c9db-7e0f-4099-8d47-5e4b4fceb86a` / `e38e697` · **日期** `2026-08-20`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项已从 `⬜` 变为其他状态；`N/A` 已写原因。
> **执行顺序（强制）**：已先在多仓对变更路径运行 `references/script/scan-all-rules.sh`（22 findings），输出并入 Step 3/Step 4 备注；其余由 LLM 完成 Step 2-5 及复核。

---

## Step 1 — 执行队列（产物 A）

> Java 守卫通过：manyu_test 含 54 个 `.java` 文件，进入审查；manyu_test1 为 Vue 前端（无 `.java`），按技能 Java 守卫标记「跳过」，仅做跨仓契约核验（见报告 REQ-7/8）。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|-----|----|----|----|----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|-----|-----|--------|
| 1 | AlgoDemoApplication.java | F01-F07 启动 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 2 | demo/controller/DemoController.java | W01-W03 | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 3 | demo/service/DemoService.java | S01-S03 契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 4 | demo/service/impl/DemoServiceImpl.java | W01-W03 业务 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️G16.2 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 5 | demo/algorithm/BubbleSortAlgorithm.java | F03 算法 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 6 | demo/algorithm/HashUtils.java | F02 算法 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 7-12 | demo/model/dto/* （6 DTO 含 Request/VO） | W01-W03 出入参 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 13-14 | demo/model/enums/HashAlgorithm.java, SortOrder.java | 枚举契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 15 | export/controller/ExportController.java | W04 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 16 | export/model/dto/ExportRequest.java | W04 入参 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 17 | export/model/enums/ExportFormat.java | W04 格式 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 18 | export/model/enums/ExportTarget.java | W04 目标 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 19 | export/service/ExportService.java | S04 契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 20 | export/service/impl/ExportServiceImpl.java | W04 业务 | ❌ | ⚠️ | ❌G1 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️G14 | ⚠️G16.2 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️S7 | ❌ |
| 21 | export/util/CsvExportUtil.java | W04 CSV | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️S7 | ⚠️ |
| 22 | tracking/annotation/TrackCall.java | F06 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 23 | tracking/aspect/TrackAspect.java | F06 埋点 | ⚠️ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 24 | tracking/async/CallRecordQueue.java | F06 异步写 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️G8 | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 25 | tracking/controller/TrackingController.java | W05-W07 | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️G14 | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 26 | tracking/dao/TrackingMapper.java | W05-W07 DAO | ✅ | ✅ | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 27 | tracking/model/entity/CallRecordDO.java | call_record 契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 28-33 | tracking/model/dto/* （6 VO） | W05-W07 出参 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 34-38 | tracking/model/enums/* （5 枚举） | 枚举契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 39 | tracking/service/TrackingService.java | S05-S06 契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 40 | tracking/service/impl/TrackingServiceImpl.java | W05-W07 业务 | ✅ | ✅ | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | ⚠️G11.2 | N/A | N/A | N/A | ⚠️G16.2 | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 41 | common/context/CallContextResolver.java | A03/S07 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️S8 | ⚠️ |
| 42 | common/context/CallerInfo.java | A03 数据 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 43 | common/exception/BizException.java | 异常契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 44 | common/exception/ErrorCode.java | 错误码 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 45 | common/exception/GlobalExceptionHandler.java | 全局转译 | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 46 | common/web/CommonResponse.java | 出参契约 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 47 | config/WebConfig.java | CORS | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️S10 | ⚠️ |
| 48 | resources/application.yml | 配置 | ⚠️ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️S9 | ⚠️ |
| 49 | resources/db/schema.sql | 数据契约 | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 50 | resources/mapper/TrackingMapper.xml | DAO SQL | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 51-54 | src/test/**（6 测试类合并 4 行） | 单测 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️M016(测试) | N/A | ⚠️G16.2(预扫误报复核) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 跳过 | manyu_test1/**（Vue/js/html） | 非 Java | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 |

- 守卫结果：manyu_test 通过（54 个 .java）；manyu_test1 无 .java → Java 守卫：按技能仅审查 Java，前端单独标注「跳过（非 Java，跨仓契约已在报告 REQ-7/8 核验）」。
- 收口：每文件各 Sn/Gn 列均非 `⬜`（跳过文件除外）；Step 4 逐条 ID 表见下方核销。

---

## Step 2 — 功能（产物 B）

> 仅从 spec/tasks 提 REQ，勿臆造。不符 spec 标 P0。

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据 |
|-----|----------|----------------------|----------|------|----------|
| REQ-1 | W01 helloworld 三态（默认/带名/超长校验） | design 5.1 W01 R01-R02 | DemoController/DemoServiceImpl/HelloWorldRequest/VO | ✅ | DemoController.java:41-44；DemoServiceImplTest 2 用例 |
| REQ-2 | W02 哈希 MD5/SHA256/SM3 + 4096 字节边界 + 枚举校验 | design 5.1 W02 R01-R02 + A04 | DemoServiceImpl/HashUtils/HashRequest | ✅ | HashUtils.java:29-34；HashUtilsTest 4 用例；DemoServiceImplTest 3 用例 |
| REQ-3 | W03 冒泡排序三变体 + 10000 上限 + 有限数校验 | design 5.1 W03 R01-R02 | DemoServiceImpl/BubbleSortAlgorithm | ✅ | BubbleSortAlgorithmTest 5 用例 + DemoServiceImplTest 3 用例 |
| REQ-4 | W04 导出各页面/报表 + EXPORT_001/002 + 防注入 | design 5.2 W04 R01-R05 | Export*/CsvExportUtil | ❌(P0 信号量) | ExportServiceImpl.java:76-87（over-release）；CsvExportUtilTest 3 用例 |
| REQ-5 | F06 埋点：次数+调用人+摘要+兜底 | design 5.3.3.1 R01-R04 | TrackAspect/CallRecordQueue/CallContextResolver | ⚠️ | TrackAspectTest 2 用例；摘要失真见报告 §7.1-3 |
| REQ-6 | F07 报表 W05/W06/W07 | design 5.3.2 | Tracking*/TrackingMapper.xml | ✅ | TrackingServiceImplTest 6 用例 |
| REQ-7 | F04 前端三 Tab + 图表 + 导出（跨仓） | design 5.5 | manyu_test1/** | ✅ | App.vue/api/index.js（非 Java 跳过核验） |
| REQ-8 | 跨仓契约 call_record/W01-W07/对齐点 | design 8.3 | schema.sql + Mapper + 前端 api | ✅ | 字段/路径/入参枚举一致 |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注（命中写 path:line） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 文件名=顶层类名、UTF-8、无 Tab（抽查） |
| A2 | 源文件结构/import 顺序 | ⚠️ | GlobalExceptionHandler.java:1-15、TrackingServiceImpl.java:1-26 import 未分组/非严格字典序（P2） |
| A3 | 代码样式 | ⚠️ | 行宽 >120：DemoController.java:43、ExportServiceImpl.java:100-102（P2） |
| A4 | 命名规范 | ✅ | 类/方法/常量/包名符合 A4；`SORT_RESULT_LIMIT` 等 UPPER_SNAKE（A4.4）✓ |
| A5 | 编码实践 | ⚠️ | TrackAspect.java:97-104 以方法名字符串驱动 switch（重构脆弱，P2）；@Override 已标 ✓ |
| A6 | 特定元素样式 | ✅ | K&R 括号、修饰符顺序、switch default 均合规 |
| A7 | Javadoc 规范 | ✅ | 主要 public 类/方法均有 Javadoc（A7.1）；简单 getter 省略（A7.3）✓ |

> 预扫 A* 无命中；LLM 复核结论与上表一致。

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（预扫命中 + LLM 补扫）

| ID | 状态 | 备注（path:line） |
|----|------|--------------------------------------------------|
| B008 | ⚠️ | CallRecordQueue.java:56 Executors.newSingleThreadScheduledExecutor → LLM 复核降 P1（单任务、有界队列、守护线程+PreDestroy 关闭） |
| B001-B007,B009-B081 | ✅ | LLM 补扫按 bug-pattern-checklist 120 条核对：未命中（无锁释放缺失、无断言吞异常、无移位溢出、无浮点金额、无 == 包装比较、无 SQL 拼接、无未释放资源） |
| M001-M015,M017-M027 | ✅ | LLM 补扫未命中（含资源释放、事务边界、并发集合、equals/hashCode 等） |
| M016 | ⚠️ | ExportServiceImpl.java:70,82 / TrackingController.java:45,63,81 / TrackingServiceImplTest（预扫 11 处）→ P1 默认时区 |
| I001-I010 | ✅ | LLM 补扫未命中 |

### 4.2 可靠性（G）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1-G1.4 | ⚠️ | G1 命中：ExportServiceImpl.java:76-87 并发信号量"未获取即释放"（P0，报告 §7.1-1）；其余 N/A |
| G2.1-G2.3 | N/A | 无写接口幂等场景（埋点表 append-only，异步批量写无重复键需求） |
| G3.1-G3.2 | N/A | 无事务/跨库强一致场景 |
| G4.1-G4.3 | ✅ | 聚合 SQL 直查 + 索引覆盖（idx_*_time）；无函数索引隐式转换；报表查询有 index 前缀 |
| G5.1 | N/A | 无 MQ |
| G6.1-G6.2 | N/A | 无缓存 |
| G7.1-G7.2 | N/A | 无调度任务（除埋点 flush，已在 G8 覆盖） |
| G8.1 | ✅ | 埋点失败均有 ERROR/WARN 日志 + 计数（CallRecordQueue.java:76,90） |
| G8.2 | ✅ | 埋点异步非阻塞，不阻塞主链路 |
| G8.3 | ✅ | 无流/连接/锁资源遗漏（try-with-resources 不需要场景） |
| G8.4 | ✅ | @PreDestroy shutdown+awaitTermination（CallRecordQueue.java:98-111） |
| G8.5 | N/A | 无 ThreadLocal 使用 |
| G8.6 | ✅ | 业务队列有界（capacity 10000）；定时执行器单任务（另见 B008） |
| G9.1-G9.3 | N/A | 无外部 RPC/HTTP 调用 |
| G10.1-G10.2 | ✅ | 成功/失败状态由 code 显式表达；契约新增向后兼容 |
| G11.1 | ✅ | 6 个测试类覆盖算法/服务/切面/CSV/统计 |
| G11.2 | ⚠️ | TrackingServiceImpl.java:132 Duration.toDays() 截断放宽 90 天边界（P2）；空集合/空参已覆盖（BubbleSortAlgorithmTest:51-52） |
| G11.3-G11.4 | ✅ | 入参校验：@Valid + @Size/@NotBlank/@NotEmpty + sanitize（NaN/Infinity）+ 4096 字节上限；无浮点金额 |
| G12.1-G12.2 | N/A | 无资损场景 |
| G13.1 | ✅ | WARN 用于业务非法（parseAlgorithm），ERROR 用于系统异常 |
| G14.1-G14.4 | ⚠️ | G14.4 命中 → M016 同源（LocalDateTime 未显式时区，P1）；无金额/租户场景 |
| G15.1-G15.3 | ✅ | 新增表/接口，向前兼容（schema.sql 新建，无存量破坏） |
| G16.1 | ✅ | 埋点支持调用量/耗时/状态/队列积压指标（droppedCount/writeFailCount） |
| G16.2 | ⚠️ | DemoServiceImpl.java:142、ExportServiceImpl.java:170、TrackingServiceImpl.java:140,151 catch 无日志（P1）；预扫 HashUtils:57（有 cause 包装=误报）、CallRecordQueue:89（有 LOGGER.error=误报）、:105（中断恢复=误报） |
| G16.3-G16.4 | ✅ | 日志级别正确；无空 catch/printStackTrace |
| G17.1-G17.3 | ✅ | tracking.enabled / export.enabled 配置开关（application.yml:21-34），schema 新增回滚=删表 |

### 4.3 安全（S）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1-S1.3 | ✅ | 全 #{} 参数化；${} 仅白名单映射（TrackingServiceImpl.java:156-171），无注入 |
| S2.1-S2.3 | N/A | 无 HTML/JS 输出（纯 JSON API） |
| S3.1-S3.3 | N/A | 无外部 URL 请求 |
| S4.1-S4.2 | N/A | 无系统命令 |
| S5.1-S5.2 | N/A | 无 XML 解析 |
| S6.1-S6.3 | N/A | 无反序列化边界（Spring JSON 默认安全） |
| S7.1-S7.3 | ⚠️ | S7.1 降 P2：CSV 公式注入前缀缺 \t/\r（CsvExportUtil.java:50）；无上传场景 |
| S8.1-S8.4 | ⚠️ | S8.1 P1：无登录态拦截器，X-Caller-* 请求头可伪造身份（CallContextResolver.java:41-54 + 前端 api/index.js:9-17） |
| S9.1-S9.4 | ⚠️ | S9.1 P1：application.yml:10-11 明文 root/root 凭证硬编码 |
| S10.1-S10.3 | ✅ | CORS 白名单仅 localhost:5173（WebConfig.java:20-24），无 *；无跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

### 5.1 自定义扩展（customized-checklist.md）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1-U2.3 | N/A(未启用自定义规则) | 清单仅示例项，团队/项目未配置私有规则 |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3`、**S1-S10 / G1-G17** 各列均非 `⬜`（跳过文件除外）；
- [x] Step 2 的每个 REQ/Scenario 均非 `⬜`
- [x] Step 3 的 A1-A7 均非 `⬜`
- [x] Step 4 全部 **G/S** 与 **B001-B081 / M001-M027 / I001-I010** ID 均核销（无关项标 N/A 附原因）
- [x] Step 5 全部 U* ID 均非 `⬜`（N/A(未启用自定义规则)）
- [x] 所有 `❌/⚠️` 已写入 report，且包含 `ID + path:line`（见 cr_report.md §3-§7.1）