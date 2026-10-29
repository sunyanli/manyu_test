# Code Review Checklist（Round 2 / 修复后复审）

> **Change** `todo-create`（新增待办事项） · **分支** `task/DEV-966dcd0a-...` · **范围** `0625c2a..HEAD`（含修复提交 `4b2f55c`） · **日期** `2026-10-29`
>
> **驱动源**：本文件为唯一进度源与驱动源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **执行顺序**：已先执行 `scan-all-rules.sh`（结果见 §4 备注），再由 LLM 逐文件完成 Step 2→3→4→5。
> **预扫命令**：`bash references/script/scan-all-rules.sh src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo src/main/resources/mapper/TodoItemMapper.xml src/main/resources/db/todo_item.sql`
> **预扫结果**：`14 findings (P0=14, P1=0, P2=0) | 52/222 rules scanned`，全部为 `G16.2 CatchWithoutLogging`，逐条复核后确认 **14/14 误报**（catch 块均记录日志后转换为业务异常或计数告警，详见 §4.2 G16.2）。
> **验证**：`mvn -o -B test` → **Tests run: 111, Failures: 0, Errors: 0, Skipped: 0**（surefire 报告 9 个测试类）。HTTP 运行时冒烟因离线仓库缺少 `spring-boot-maven-plugin` 依赖未能启动，已降级为单测 + 静态审查。

---

## Step 1 — 执行队列（产物 A）

> `Step4` 列语义：`G1`…`G17` 对应 `reliability-checklist.md` 第 G*n* 节，`S1`…`S10` 对应 `security-checklist.md` 第 S*n* 节。`✅`=已扫无命中，`N/A`=该文件与本节无关，`⚠️`=命中风险（详见 §4 明细）。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|--------|
| 1 | `src/main/java/com/antdigital/todo/TodoApplication.java` | 启动装配 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 2 | `src/main/java/com/antdigital/todo/common/constant/TodoConstants.java` | REQ-2/6/7/8 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 3 | `src/main/java/com/antdigital/todo/common/enums/DeleteFlagEnum.java` | REQ-6 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 4 | `src/main/java/com/antdigital/todo/common/enums/TodoErrorCodeEnum.java` | REQ-1（错误码） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 5 | `src/main/java/com/antdigital/todo/common/enums/TodoSourceEnum.java` | REQ-6 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 6 | `src/main/java/com/antdigital/todo/common/enums/TodoStatusEnum.java` | REQ-6 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 7 | `src/main/java/com/antdigital/todo/common/exception/TodoBizException.java` | REQ-1（异常契约） | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 8 | `src/main/java/com/antdigital/todo/common/exception/TodoDuplicateKeyException.java` | REQ-3/7 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 9 | `src/main/java/com/antdigital/todo/common/model/ApiResult.java` | REQ-12 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 10 | `src/main/java/com/antdigital/todo/common/util/DesensitizeUtil.java` | REQ-13 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 11 | `src/main/java/com/antdigital/todo/common/util/TodoTextUtil.java` | REQ-2 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 12 | `src/main/java/com/antdigital/todo/controller/TodoController.java` | REQ-1/5/12 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 13 | `src/main/java/com/antdigital/todo/controller/TodoExceptionHandler.java` | REQ-1/8 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 14 | `src/main/java/com/antdigital/todo/dao/entity/TodoItemDO.java` | REQ-11 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 15 | `src/main/java/com/antdigital/todo/dao/manager/TodoItemManager.java` | REQ-1/3 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 16 | `src/main/java/com/antdigital/todo/dao/mapper/TodoItemMapper.java` | REQ-1/3 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 17 | `src/main/java/com/antdigital/todo/integration/AuditIntegration.java` | REQ-4 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 18 | `src/main/java/com/antdigital/todo/integration/AuthIntegration.java` | REQ-5 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 19 | `src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java` | REQ-4 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 20 | `src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java` | REQ-5 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 21 | `src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java` | REQ-5（安全强化） | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ⚠️ 已审有问题 |
| 22 | `src/main/java/com/antdigital/todo/model/dto/OperatorContext.java` | REQ-5 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 23 | `src/main/java/com/antdigital/todo/model/dto/TodoCreateRequest.java` | REQ-1/2 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 24 | `src/main/java/com/antdigital/todo/model/dto/TodoCreateResult.java` | REQ-1 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 25 | `src/main/java/com/antdigital/todo/model/dto/ValidatedTodo.java` | REQ-2 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 26 | `src/main/java/com/antdigital/todo/model/vo/TodoCreateVO.java` | REQ-12 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 27 | `src/main/java/com/antdigital/todo/service/TodoService.java` | REQ-1 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 28 | `src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java` | REQ-7/10 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 已审有问题 |
| 29 | `src/main/java/com/antdigital/todo/service/component/TodoCreateGate.java` | REQ-9 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 30 | `src/main/java/com/antdigital/todo/service/component/TodoMetrics.java` | REQ-10 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 已审有问题 |
| 31 | `src/main/java/com/antdigital/todo/service/component/TodoNoGenerator.java` | REQ-7 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 32 | `src/main/java/com/antdigital/todo/service/component/TodoNoGeneratorImpl.java` | REQ-7/10 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 已审有问题 |
| 33 | `src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java` | REQ-8 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 34 | `src/main/java/com/antdigital/todo/service/component/TodoSequenceProvider.java` | REQ-7 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ 已审 |
| 35 | `src/main/java/com/antdigital/todo/service/component/TodoValidator.java` | REQ-2/6 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 36 | `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java` | REQ-1/3/4/5/6/7/8/10 | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ 已审有问题 |
| 37 | `src/test/java/com/antdigital/todo/common/util/TodoTextUtilTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 38 | `src/test/java/com/antdigital/todo/controller/TodoControllerTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 39 | `src/test/java/com/antdigital/todo/dao/manager/TodoItemManagerTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 40 | `src/test/java/com/antdigital/todo/integration/support/GatewaySignatureVerifierTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 41 | `src/test/java/com/antdigital/todo/integration/support/GatewaySignatureVerifierTestSupport.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 42 | `src/test/java/com/antdigital/todo/service/component/TodoCreateGateTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 43 | `src/test/java/com/antdigital/todo/service/component/TodoNoGeneratorImplTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 44 | `src/test/java/com/antdigital/todo/service/component/TodoValidatorTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |
| 45 | `src/test/java/com/antdigital/todo/service/impl/TodoServiceImplTest.java` | 模块装配/契约 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ 已审 |

- 变更范围内 `.java` 共 **45** 个（主代码 36 / 测试 9），全部纳入队列，无 `⬜` 残留；非 Java 变更（`pom.xml`、`application.yml`、`TodoItemMapper.xml`、`todo_item.sql`、文档）随所属代码项一并核对。
- 既有排序模块（`com.antdigital.sort`）不在本次 change 范围，未纳入队列。

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | Given 合法 requestId+itemName，When POST /api/todo/item/create，Then 落库一条待办并返回 id/todoNo/status | design §4.1 W01、§5.1.2「URI: POST /api/todo/item/create」、§5.1.3.1 时序图 | `TodoController.java`、`TodoServiceImpl.java`、`TodoItemMapper.xml` | ✅ | `TodoController.java:68-74`；`TodoServiceImpl.java:149-194`；测试 `TodoServiceImplTest#should_createTodo_when_validRequest`、`TodoControllerTest#should_returnSuccessResult_when_validRequest` |
| REQ-2 | Given 名称含全角空白/控制字符/emoji，When 校验，Then trim+NFC+剔除控制字符后 1~128 校验；requestId 仅字母数字-_ 且 ≤64 | design §5.1.3.1 R01~R04、§5.1.2 S03 | `TodoValidator.java`、`TodoTextUtil.java` | ✅ | `TodoValidator.java:35-106`；`TodoTextUtil.java:46-123`；测试 `TodoValidatorTest`（22）、`TodoTextUtilTest`（12） |
| REQ-3 | Given 同一 tenantId+requestId 重复提交（含并发），When 创建，Then 返回首次结果且 idempotent=true，不报错 | design §5.1.3.1 R07、§5.1.3.1 并发控制「冲突后重查返回首次结果」 | `TodoServiceImpl.java`、`TodoItemManager.java`、`TodoItemMapper.xml` | ⚠️ | `TodoServiceImpl.java:157-181`；测试 `should_returnFirstResult_when_sameRequestIdSubmittedTwice`、`should_returnFirstResult_when_duplicateRequestIdRace`；并发 ≥3 同 requestId 时存在锁升级死锁（见 §7.1 P1-1） |
| REQ-4 | Given 创建成功，When 事务提交后，Then 异步写审计（requestId/todoNo/operatorId），失败不阻断 | design §5.1.3.1 R10、§4.4 I02 | `TodoServiceImpl.java`、`AuditIntegrationImpl.java` | ✅ | `TodoServiceImpl.java:311-339`（afterCommit 注册）；`AuditIntegrationImpl.java:88-96`；测试 `should_stillSucceed_when_auditThrows` |
| REQ-5 | Given 网关注入身份/租户/权限，When 创建，Then 身份只取可信上下文，缺失即 TODO_0005；无权限点即 TODO_0006 | design §5.1.3.1 R05/R06、§6.4.2.1~6.4.2.3 | `GatewayAuthIntegrationImpl.java`、`GatewaySignatureVerifier.java`、`TodoServiceImpl.java` | ✅ | `GatewayAuthIntegrationImpl.java:65-83`；`TodoServiceImpl.java:253-276`；测试 `TodoControllerTest`（7）、`GatewaySignatureVerifierTest`（7） |
| REQ-6 | Given 客户端传 status/is_deleted/source，When 落库，Then status=0、is_deleted=0、source 缺省 web 且「不接受前端指定」 | design §5.1.3.1 R08 | `TodoServiceImpl.java`、`TodoValidator.java` | ⚠️ | `TodoServiceImpl.java:294-298` 强制 status/deleted；但 `TodoValidator.java:100-106` 接受客户端 source，与 R08「不接受前端指定」冲突（W01 入参表又允许 source，spec 自相矛盾，见 §7.1 P1-6） |
| REQ-7 | Given 取号冲突，When 落库失败，Then 重试 1 次取新号；仍冲突返回 TODO_0007 | design §5.1.3.1 R09、A09 编号格式 | `TodoNoGeneratorImpl.java`、`TodoServiceImpl.java` | ✅ | `TodoServiceImpl.java:166-193`（attempt<=1）；`TodoNoGeneratorImpl.java:77-85`；测试 `should_retryAndSucceed_when_todoNoConflictOnce`、`should_throwTodoNoConflict_when_todoNoAlwaysConflicts`、`TodoNoGeneratorImplTest#should_generateTodoNo_when_sequenceGiven` |
| REQ-8 | Given 单用户 1 分钟内超过阈值，When 创建，Then 返回 TODO_0009 | design §5.1.3.1 R11、§6.3 限流 | `TodoRateLimiter.java` | ✅ | `TodoRateLimiter.java:73-84`（窗口 60s，默认 30）；测试 `should_throwRateLimited_when_exceedsLimit` |
| REQ-9 | Given 总开关关闭或租户未命中灰度，When 创建，Then 返回 TODO_0010（503），不落库 | design §7.2 可灰度、§7.3 一级应急 | `TodoCreateGate.java`、`TodoServiceImpl.java` | ✅ | `TodoCreateGate.java:68-81`；`TodoServiceImpl.java:151`；测试 `TodoCreateGateTest`（4）+ `TodoServiceImplTest` 三例 |
| REQ-10 | Given 创建链路，When 调用/失败/幂等/限流/降级，Then 上报 §6.5 五项指标 | design §6.5、§7.1 | `TodoMetrics.java`、`TodoServiceImpl.java`、`TodoNoGeneratorImpl.java` | ⚠️ | `TodoMetrics.java:114-198`；`TodoServiceImpl.java:127-139`；降级指标恒计数（见 §7.1 P1-3） |
| REQ-11 | Given 建表脚本，When 建表，Then 字段/索引与 §5.1.1.1 一致（uk_todo_item_no、uk(tenant_id,request_id)、两个 idx） | design §5.1.1.1 表结构/索引 | `src/main/resources/db/todo_item.sql`、`TodoItemDO.java` | ✅ | `todo_item.sql:5-27`；`TodoItemDO.java:16-206`（索引名与 `TodoDuplicateKeyException` 常量一致） |
| REQ-12 | Given 创建成功，When 返回 W01，Then data.id 为 String、gmtCreate 为 yyyy-MM-dd HH:mm:ss、code=OK/msg=SUCCESS | design §5.1.2 W01 出参表（修订后 data.id 为 String） | `TodoController.java`、`TodoCreateVO.java`、`ApiResult.java` | ✅ | `TodoController.java:82-94`；`ApiResult.java:56-69`；测试 `TodoControllerTest#should_returnSuccessResult_when_validRequest` |
| REQ-13 | Given 日志/审计输出，When 打印，Then 不出现 itemName/itemDesc 原文，敏感信息脱敏 | design §6.4.3.2 | `TodoServiceImpl.java`、`TodoItemDO.java`、`DesensitizeUtil.java` | ✅ | `TodoServiceImpl.java:160-190`（仅 requestId/todoNo/creatorId）；`TodoItemDO.java:192-205`；`DesensitizeUtil.java:58-66` |

---

## Step 3 — 可读性检查（产物 C）

> 预扫（`scan-all-rules.sh`）对 A 类未报告命中；LLM 复核结果如下。行宽以**显示宽度**（CJK 计 2）复核，`src` 全量 **0 处 > 120**。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 文件名=类名、UTF-8、无 Tab（全量 `src` 0 处 Tab） |
| A2 | 源文件结构/import 顺序 | ⚠️ | A2.4：`java.*` 组置于 `org.*` 之后（如 `TodoServiceImpl.java:23-30`、`TodoCreateGate.java:6-14`），全仓库一致，P2 |
| A3 | 代码样式 | ✅ | K&R、4 空格、行宽 ≤120、成员间空行 |
| A4 | 命名规范 | ✅ | 类/方法/常量/包名均符合；测试类 `XxxTest` |
| A5 | 编码实践 | ✅ | `@Override` 齐备（`TodoServiceImpl.java:124/204/317`）；无空 catch、无 `finalize` |
| A6 | 特定元素样式 | ✅ | `switch` 有 `default`（`TodoExceptionHandler.java:155-174`）；long 字面量 `L` 后缀（`TodoConstants.java:73/158/163`） |
| A7 | Javadoc 规范 | ✅ | public 类/方法均有 Javadoc，块标记顺序正确；getter 省略符合 A7.3 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`，B001–B081 / M001–M027 / I001–I010）

> 预扫仅覆盖 25/81 B + 6/27 M + 2/10 I；下表为预扫 + LLM 逐条核销结论。`✅ 无命中` 表示已按规则语义扫描本 change 全部 Java 文件。

| ID | 状态 | 备注（命中写 `path:line`；预扫可粘贴脚本摘要） |
|----|------|--------------------------------------------------|
| B001 | ✅ 无命中 | — |
| B002 | ✅ 无命中 | — |
| B003 | ✅ 无命中 | — |
| B004 | ✅ 无命中 | — |
| B005 | ✅ 无命中 | — |
| B006 | ✅ 使用 AssertJ，无 assertEquals 顺序问题 | — |
| B007 | ✅ 无命中 | — |
| B008 | ✅ 无命中 | — |
| B009 | ✅ 无命中 | — |
| B010 | ✅ 无命中 | — |
| B011 | ✅ 未发现包装类型 `==` 比较 | — |
| B012 | N/A | N/A（无 Calendar） |
| B013 | N/A | N/A（无 Calendar） |
| B014 | ✅ 无命中 | — |
| B015 | ✅ 无命中 | — |
| B016 | ✅ 无命中 | — |
| B017 | ✅ 无命中 | — |
| B018 | ✅ 无命中 | — |
| B019 | N/A | N/A（无 Money 类型） |
| B020 | ✅ 无命中 | — |
| B021 | N/A | N/A（无 Jedis） |
| B022 | ✅ 无命中 | — |
| B023 | ✅ 无命中 | — |
| B024 | ✅ 无命中 | — |
| B025 | ✅ 无命中 | — |
| B026 | ✅ 无命中 | — |
| B027 | ✅ 无命中 | — |
| B028 | N/A | N/A（无 commons DateUtil） |
| B029 | ✅ 无命中 | — |
| B030 | ✅ 无命中 | — |
| B031 | ✅ 无命中 | — |
| B032 | ✅ 无命中 | — |
| B033 | N/A | N/A（无 Unsafe） |
| B034 | ✅ 无命中 | — |
| B035 | ✅ 无命中 | — |
| B036 | N/A | N/A（无 IdentityHashMap） |
| B037 | ✅ 无命中 | — |
| B038 | ✅ 无命中 | — |
| B039 | ✅ 无命中 | — |
| B040 | N/A | N/A（无 isInstance） |
| B041 | ✅ 无命中 | — |
| B042 | N/A | N/A（无 JUnit3 测试） |
| B043 | N/A | N/A（无内部类 `@Test`） |
| B044 | N/A | N/A（无混用） |
| B045 | ✅ 无命中 | — |
| B046 | ✅ 无命中 | — |
| B047 | N/A | N/A（无 Float.compare） |
| B048 | N/A | N/A（无 Math.round） |
| B049 | ✅ 日期格式 `yyyyMMdd` 正确 | — |
| B050 | ✅ `yyyy-MM-dd HH:mm:ss` 24 小时制 | — |
| B051 | ✅ 无命中 | — |
| B052 | ✅ 未使用 `YYYY` | — |
| B053 | ✅ 异常断言统一用 AssertJ `assertThatThrownBy` | — |
| B054 | N/A | N/A（无 EqualsTester） |
| B055 | ✅ 未使用 Mockito`,`when/verify` | — |
| B056 | ✅ 无命中 | — |
| B057 | ✅ 无命中 | — |
| B058 | ✅ 无命中 | — |
| B059 | N/A | N/A（无 nCopies） |
| B060 | ✅ 无命中 | — |
| B061 | N/A | N/A（无 sun.misc） |
| B062 | N/A | N/A（无 ClassLoader 强转） |
| B063 | N/A | N/A（无 javax.xml.bind） |
| B064 | ✅ 无命中 | — |
| B065 | ✅ 无命中 | — |
| B066 | ✅ 无命中 | — |
| B067 | ✅ 无命中 | — |
| B068 | ✅ 无命中 | — |
| B069 | ✅ 无命中 | — |
| B070 | ✅ 无命中 | — |
| B071 | ✅ 无命中 | — |
| B072 | ✅ 无命中 | — |
| B073 | ✅ 无命中 | — |
| B074 | ✅ 无命中 | — |
| B075 | ✅ 无命中 | — |
| B076 | ✅ `@Transactional` 均在 public 方法 | — |
| B077 | ✅ 无命中 | — |
| B078 | ✅ 无命中 | — |
| B079 | ✅ 无命中 | — |
| B080 | ✅ 全部测试含断言 | — |
| B081 | ✅ 无命中 | — |
| M001 | ✅ 无命中 | — |
| M002 | ✅ 无命中 | — |
| M003 | ✅ 无命中 | — |
| M004 | ✅ 无命中 | — |
| M005 | ✅ 无命中 | — |
| M006 | ✅ 无命中 | — |
| M007 | ✅ 无空 catch | — |
| M008 | ✅ 未重写 equals | — |
| M009 | ✅ 无命中 | — |
| M010 | ✅ 无命中 | — |
| M011 | ✅ `switch` 分支均 return | — |
| M012 | ✅ 无命中 | — |
| M013 | ✅ 无命中 | — |
| M014 | ✅ 无命中 | — |
| M015 | ✅ 无命中 | — |
| M016 | ✅ 时间均显式指定时区 | — |
| M017 | ✅ 无命中 | — |
| M018 | ✅ 无命中 | — |
| M019 | ✅ `switch` 覆盖枚举且含 default | — |
| M020 | ✅ 重写方法均有 `@Override` | — |
| M021 | ✅ 无命中 | — |
| M022 | ✅ 无命中 | — |
| M023 | ✅ 无命中 | — |
| M024 | ✅ 无命中 | — |
| M025 | ✅ 无命中 | — |
| M026 | ✅ 无命中 | — |
| M027 | ✅ 未使用 ThreadLocal | — |
| I001 | ✅ 无命中 | — |
| I002 | N/A | N/A（无 `@DoNotMock`） |
| I003 | N/A | N/A（无 `@AutoValue`） |
| I004 | N/A | N/A（全仓未使用 `java.util.Date`） |
| I005 | N/A | N/A（无 JUnit3） |
| I006 | N/A | N/A（无 JUnit4 `setUp`） |
| I007 | N/A | N/A（无 JUnit4 `tearDown`） |
| I008 | N/A | N/A（无 `dataProvider`） |
| I009 | N/A | N/A（统计型规则） |
| I010 | N/A | N/A（无容器启动测试） |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | ⚠️ | 唯一键冲突后的幂等回查用 `FOR UPDATE`，与失败 INSERT 的共享锁叠加可致锁升级死锁：`TodoServiceImpl.java:172` + `TodoItemMapper.xml:51`（P1-1） |
| G1.2 | ✅ | 冲突回查后基于最新记录直接返回首次结果，无二次更新 |
| G1.3 | ✅ | 无乐观锁重试逻辑 |
| G1.4 | ✅ | 仅单行锁，无多资源锁顺序问题 |
| G2.1 | ✅ | 写接口以 `requestId` 幂等键 + 唯一索引兜底 |
| G2.2 | ✅ | 重试沿用同一 `requestId`，服务端重复返回首次结果 |
| G2.3 | ✅ | 幂等键字段名/粒度与系分 A08、R07 一致 |
| G3.1 | ✅ | 单库本地事务，无分布式事务 |
| G3.2 | ⚠️ | 事务内含 `todoRateLimiter`/指标调用（轻量本地操作），外部审计已移至 afterCommit：`TodoServiceImpl.java:151-153`（可接受，无阻塞 I/O） |
| G4.1 | ✅ | 业务规则在代码，SQL 仅单表等值查询 |
| G4.2 | ✅ | 查询列与索引一致（tenant_id, request_id），无函数/隐式转换 |
| G4.3 | N/A（本期无列表查询） |  |
| G5.1 | N/A（本期无 | MQ） |
| G6.1 | N/A（本期无缓存） |  |
| G6.2 | N/A（本期无缓存） |  |
| G7.1 | N/A（本期无调度任务） |  |
| G7.2 | N/A（本期无调度任务） |  |
| G8.1 | ✅ | 异常均记录并转换为明确错误码，无静默吞异常 |
| G8.2 | ⚠️ | 系分 §6.3 要求创建接口独立线程池/信号量（默认 50 并发）隔离，代码未实现（P1-5） |
| G8.3 | ✅ | 无流/连接/锁资源（Hikari 管理连接） |
| G8.4 | ✅ | 审计消费线程 `@PreDestroy` 优雅停机：`AuditIntegrationImpl.java:101-118` |
| G8.5 | ✅ | 未使用 ThreadLocal |
| G8.6 | ✅ | 未使用 `Executors` 默认线程池（仅 `new Thread`） |
| G9.1 | N/A（本期无外部 | RPC/HTTP 调用） |
| G9.2 | ✅ | DB 超时 1s（`application.yml:22` `default-statement-timeout: 1`、`:15` 连接超时 1000ms） |
| G9.3 | ✅ | 重试用幂等键（`requestId` 不变） |
| G10.1 | ✅ | 查询未命中返回 null 且有明确语义（非异常） |
| G10.2 | ✅ | 新增接口，无契约变更 |
| G11.1 | ✅ | 新增逻辑均有单测（111 用例全通过） |
| G11.2 | ✅ | 覆盖空/全角空白/emoji/128/512 边界、幂等与冲突路径；DB 并发路径无集成测试（见 P1-1） |
| G11.3 | ✅ | 入参 null/空集合均有防御（`TodoValidator.java:36-38`、`OperatorContext.java:47`） |
| G11.4 | ✅ | 无金额/浮点运算 |
| G12.1 | N/A（非资金场景） |  |
| G12.2 | ✅ | 有总开关、灰度与回滚预案（`TodoCreateGate`） |
| G13.1 | ✅ | 成功 INFO、业务异常 WARN、系统异常 ERROR |
| G14.1 | N/A（无金额） |  |
| G14.2 | ✅ | 租户来自网关注入并参与查询条件（`TodoServiceImpl.java:241-246`） |
| G14.3 | ✅ | 本地 `LocalDateTime` + 固定业务时区，无 UTC 混用 |
| G14.4 | ✅ | `DateTimeFormatter` 均显式 pattern，时间取值显式时区（`TodoConstants.java:138-143`） |
| G15.1 | ✅ | 全新增表，无存量结构变更 |
| G15.2 | ✅ | 新增接口，旧接口不受影响 |
| G15.3 | ✅ | 不兼容逻辑由开关控制（`todo.create.enabled` / `auth.gateway.verify.enabled`） |
| G16.1 | ⚠️ | 五项指标已埋点，但 `todo_no_degrade_total` 恒计数，告警失效（P1-3）；`todo_create_total` 每次请求动态注册（P2） |
| G16.2 | ✅ | 预扫 14 处 CatchWithoutLogging 全部误报：`TodoItemManager.java:73`、`AuditIntegrationImpl.java:106/130/138`、`GatewaySignatureVerifier.java:148/166`、`SnowflakeSequenceProvider.java:142`、`TodoServiceImpl.java:132/135/170/182/334`、测试 `TodoControllerTest.java:185`、`GatewaySignatureVerifierTest.java:135` 均在 catch 内 `LOGGER.error/warn` 并转换/抛出 |
| G16.3 | ✅ | 级别正确 |
| G16.4 | ✅ | 无空 catch / printStackTrace |
| G17.1 | ✅ | `todo.create.enabled` 配置中心可关，无需重启 |
| G17.2 | ✅ | 审计/取号弱依赖有降级；登录态强依赖 fail-closed |
| G17.3 | ✅ | 全新增表，回滚无需数据脚本 |
| G18.1 | ✅ | 入参长度/字符集校验完整 |
| G18.2 | ✅ | 日志不打印名称/描述原文 |
| G18.3 | ✅ | 密钥来自配置中心，缺失 fail-closed |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | 全 `#{}` 参数化，无 `${}`（`TodoItemMapper.xml`） |
| S1.2 | N/A（无动态 | order by/表名） |
| S1.3 | N/A（无 | like/in 查询） |
| S2.1 | ✅ | JSON 数据响应，按系分 §5.1.2 由展示端转义（纯文本语义，不落 HTML） |
| S2.2 | N/A（不支持富文本） |  |
| S2.3 | N/A（无模板引擎） |  |
| S3.1 | N/A（无出网请求） |  |
| S3.2 | N/A |  |
| S3.3 | N/A |  |
| S4.1 | ✅ | 无命令拼接 |
| S4.2 | ✅ | 无文件/图片操作 |
| S5.1 | N/A（无 | XML 解析） |
| S5.2 | N/A |  |
| S6.1 | N/A（无 | Java 原生反序列化） |
| S6.2 | ✅ | Jackson 默认，无多态类型 |
| S6.3 | N/A（无敏感字段 | DTO 序列化） |
| S7.1 | N/A（无上传） |  |
| S7.2 | N/A |  |
| S7.3 | N/A |  |
| S8.1 | ⚠️ | 已接入鉴权且身份头签名校验 fail-closed；但签名不绑定 method/path/body，300s 内可重放（P1-4） |
| S8.2 | ✅ | 写操作仅 POST |
| S8.3 | N/A（本期无按 | ID 读取/操作接口） |
| S8.4 | N/A（服务端不下发 | Cookie） |
| S9.1 | ✅ | 密钥由配置中心注入，默认空且 fail-closed（`GatewaySignatureVerifier.java:113-117`） |
| S9.2 | ✅ | 日志不含名称/描述原文，审计事件不含正文 |
| S9.3 | ✅ | JDBC `useSSL=true`，HMAC-SHA256 + 常量时间比较 |
| S9.4 | ✅ | 无随机数需求（未使用 Random） |
| S10.1 | N/A（无 | Cookie 会话，鉴权走签名头，CSRF 不成立） |
| S10.2 | N/A（未开放 | CORS） |
| S10.3 | N/A（无跳转） |  |

---

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | ✅ | Controller 入参使用 `@Valid` + `@NotBlank`（`TodoController.java:69`、`TodoCreateRequest.java:21-28`） |
| U1.2 | N/A(未启用自定义规则) | `customized-checklist.md` U2 无内容，仅示例项 |
| U1.3 | N/A(未启用自定义规则) | 同上 |
| U2.1 | N/A(未启用自定义规则) | 同上 |
| U2.2 | N/A(未启用自定义规则) | 同上 |
| U2.3 | N/A(未启用自定义规则) | 同上 |

---

## 终检（防漏检）

- [x] 执行队列 45 个文件 `Step2`/`Step3`/`G1–G17`/`S1–S10` 各列均非 `⬜`
- [x] Step 2 的 13 个 REQ 均非 `⬜`
- [x] Step 3 的 A1–A7 均非 `⬜`
- [x] Step 4 的 G/S 与 B001–B081 / M001–M027 / I001–I010 均非 `⬜`（`N/A` 已注明原因）
- [x] Step 5 的 U* 均非 `⬜`
- [x] 所有 `⚠️` 已写入 `cr_report.md`，含 `ID + path:line` 与代码片段

