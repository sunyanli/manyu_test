# Code Review Checklist

> **Change** 生成"小猫喵喵叫"3秒视频服务 · **分支/Commit** `AI/task-DEV-eecb99b0-…` / `574aa4a` · **日期** 2026-09-17
> 状态仅用 `⬜ ✅ ❌ ⚠️ N/A`。执行顺序：先运行 `scan-all-rules.sh` 再 LLM 逐文件复核。

## Step 1 — 执行队列（产物 A）

> 34 个 `.java` 文件（`git diff HEAD~1 HEAD` 变更），另 9 个非 Java 文件（pom.xml、application.yml、schema.sql、3 个 mapper XML、design.md、impl.md、2 个 docs）标「跳过」。

| # | 文件 | 归属 | Step2 | Step3 | G1-G17 | S1-S10 | 总状态 |
|---|------|------|-------|-------|--------|--------|--------|
| 1 | service/impl/VideoTaskServiceImpl.java | REQ 全量（核心状态机） | ✅ | ⚠️ | ⚠️ | ❌ | ❌ |
| 2 | api/VideoTaskController.java | REQ-W01/W02/W03/W04 | ⚠️ | ✅ | N/A(编排) | ❌ | ❌ |
| 3 | api/OpenapiVideoTaskController.java | REQ-O01/O02 | ⚠️ | ✅ | N/A(编排) | ❌ | ❌ |
| 4 | api/VideoEngineCallbackController.java | REQ-F05 | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| 5 | api/CreateVideoTaskRequest.java | REQ 入参 | ✅ | ✅ | N/A | ✅ | ✅ |
| 6 | api/Responses.java | 响应构造 | ✅ | ✅ | N/A | N/A | ✅ |
| 7 | common/constant/ErrorCodes.java | 错误码 | ⚠️ | ✅ | N/A | N/A | ⚠️ |
| 8 | common/constant/VideoConsts.java | 常量 | ✅ | ✅ | N/A | N/A | ✅ |
| 9 | common/exception/BusinessException.java | 异常 | ✅ | ✅ | N/A | N/A | ✅ |
| 10 | common/exception/GlobalExceptionHandler.java | 异常映射 | ⚠️ | ✅ | ⚠️ | N/A | ⚠️ |
| 11 | common/model/ApiResponse.java | 响应模型 | ✅ | ✅ | N/A | N/A | ✅ |
| 12 | common/model/PageResult.java | 分页 | ✅ | ✅ | N/A | N/A | ✅ |
| 13 | dao/mapper/VideoTaskMapper.java | 数据访问 | ✅ | ⚠️(行宽) | N/A | ✅ | ⚠️ |
| 14 | dao/mapper/VideoResultMapper.java | 数据访问 | ✅ | ✅ | N/A | ✅ | ✅ |
| 15 | dao/mapper/AiEngineJobMapper.java | 数据访问 | ✅ | ✅ | N/A | ✅ | ✅ |
| 16 | manager/engine/AiVideoEngineClient.java | 引擎抽象 | ✅ | ✅ | ⚠️ | N/A | ⚠️ |
| 17 | manager/engine/EngineGenerateRequest.java | 引擎请求 | ✅ | ✅ | N/A | N/A | ✅ |
| 18 | manager/engine/EngineJobStatus.java | 引擎状态 | ✅ | ✅ | N/A | N/A | ✅ |
| 19 | manager/engine/MockAiVideoEngineClient.java | 引擎 Mock | ✅ | ✅ | ⚠️ | N/A | ⚠️ |
| 20 | manager/storage/DefaultVideoStorageService.java | 存储占位 | ✅ | ✅ | N/A | N/A | ✅ |
| 21 | manager/storage/VideoStorageService.java | 存储接口 | ✅ | ✅ | N/A | N/A | ✅ |
| 22 | model/dto/CreateVideoTaskCommand.java | 命令对象 | ✅ | ✅ | N/A | N/A | ✅ |
| 23 | model/dto/EngineCallbackPayload.java | 回调报文 | ✅ | ✅ | N/A | N/A | ✅ |
| 24 | model/dto/VideoTaskQuery.java | 查询参数(冗余) | ⚠️ | ✅ | N/A | N/A | ⚠️ |
| 25-27 | model/entity/{AiEngineJobDO,VideoResultDO,VideoTaskDO}.java | 实体 | ✅ | ✅ | N/A | N/A | ✅ |
| 28-29 | model/enums/{CallbackStatusEnum,TaskStatusEnum}.java | 枚举 | ✅ | ✅ | N/A | N/A | ✅ |
| 30 | model/vo/VideoTaskVO.java | 视图对象 | ✅ | ✅ | N/A | N/A | ✅ |
| 31 | service/VideoTaskService.java | 服务接口 | ✅ | ✅ | N/A | N/A | ✅ |
| 32 | VideoApplication.java | 启动类 | ✅ | ✅ | N/A | N/A | ✅ |
| 33 | test/.../TaskStatusEnumTest.java | 枚举测试 | ✅ | ✅ | N/A | N/A | ✅ |
| 34 | test/.../VideoTaskServiceImplTest.java | 服务测试 | ✅ | ⚠️(行宽) | ⚠️ | N/A | ⚠️ |

非 Java 文件：`pom.xml`、`application.yml`、`db/schema.sql`、`mapper/*.xml`(×3)、`design.md`、`impl.md`、`docs/*.md`(×2) → 跳过（Step4 统一 `N/A(非 Java)`，但 schema/yml/xml 在可靠性/安全复核中作为证据引用）。

## Step 2 — 功能（产物 B）

| REQ | Scenario | 结论 | 代码证据 |
|-----|----------|------|----------|
| F01 创建任务+幂等 | prompt 非空/时长 1~30/幂等返回既有 | ⚠️ | VideoTaskServiceImpl.java:76-122；缺 format 枚举校验 |
| F02 查询详情（租户隔离） | tenant 匹配返回 VO，越权 VT_005 | ⚠️ | :126-137 + :266-275；租户头用户可控 |
| F03 获取/预览视频 | 成功回调写 video_result，查询合并 resultUrl | ✅ | :198-200, :132-135 |
| F04 任务列表/取消 | 分页；非终态可取消 | ✅ | :141-166 |
| F05 引擎回调 | 验签+幂等+终态忽略 | ⚠️ | :170-228；幂等读后写竞态 |
| F06 规格管理 | 默认值 720p/16:9/mp4 | ⚠️ | VideoConsts.java；无枚举校验，仅默认 |

## Step 3 — 可读性检查（产物 C）

| ID | 状态 | 备注 |
|----|------|------|
| A1 源文件格式 | ✅ | UTF-8、无 Tab |
| A2 结构/import | ✅ | 无 import *、分组正确 |
| A3 代码样式 | ⚠️ | **A3.4** 8 处超 120 字符（scan，路径见 report） |
| A4 命名规范 | ✅ | 符合驼峰/常量规范 |
| A5 编码实践 | ✅ | @Override 齐全、无 finalize |
| A6 特定元素样式 | ✅ | long 用大写 L |
| A7 Javadoc | ✅ | 关键类有注释 |

## Step 4 — 可靠性/安全/Bug 模式（产物 D）

> 先运行 scan-all-rules.sh，命中：P0×3(G16.2 标注)、P1×1(M016)、P2×8(A3.4)。经 LLM 逐条复核源码行后，重新定级：
> - `CatchWithoutLogging`×3：引擎提交 catch (line 243) **已有 logger.warn**，非命中；`sign()` (line 320) 与测试 (line 278) catch 为重抛（未吞异常），降级为 P1/G16.2 可观测性建议，**非 P0**。
> - G16.2 在 reliability-checklist 行内等级为 **P1**，脚本输出 `[P0]` 为脚本标注偏差。
> - M016 `LocalDateTime.now()` 无时区 → P1（与 G14.4 同源）。

可靠性命中（LLM 复核）：G1.1(并发幂等竞态,P0)、G3.2(事务内外部I/O,P1)、G14.4/M016(时区,P1)、G16.2(重抛无日志,P1)。
安全命中（LLM 复核）：S8.1(接口未接真实鉴权, P0，与 G14.2 租户头可控同源)、S9.1(回调密钥硬编码默认值, P0)。
Bug 模式：无 B/M/I 高风险命中（scan 52/222 已覆盖高危子集）。

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 |
|----|------|
| U1.1 | ✅（Controller 均用 @Valid） |
| U2.* | N/A(未启用业务红线规则) |

## 终检
- [x] 每个 Java 文件 Step2/Step3/G/S 均已核销或 N/A
- [x] Step4 scan 已执行并复核
- [x] 所有 ❌/⚠️ 已写入 cr_report.md（含 ID + path:line）