# 编码实现报告：小猫喵喵叫文生视频服务

- **阶段**：coding（编码实现）
- **技能**：dtazziboot-java-coding-standards v1.1.0
- **OUTPUT_FILE**：`.agents/20260917-生成一个_3_秒视频_内容是_小猫喵喵叫/impl.md`
- **系分方案**：`.agents/20260917-生成一个_3_秒视频_内容是_小猫喵喵叫/design.md`
- **工程坐标**：`com.antdigital.video`，artifact `video-generation-service`
- **技术栈**：Spring Boot 3.3.5 + Java 21 + MyBatis Spring Boot Starter 3.0.3 + MySQL / H2(test)
- **验证状态**：❌ 未执行编译/单测（本环境无 `java`/`mvn`/`gradle`），已做 L1 静态审查

---

## 模块进度追踪

| 序号 | 模块 | READ | TEST | IMPL | CHECK | DOCS | 状态 |
|:----:|------|:----:|:----:|:----:|:-----:|:----:|------|
| 1 | video-generation-service | ✅ | ✅ | ✅ | ✅ | ✅ | 已完成 |

---

## 📖 READ: video-generation-service

**模块职责**：接收「小猫喵喵叫」等文生视频提示词，创建视频生成任务、幂等去重、租户隔离、提交外部引擎、处理引擎回调推进状态机、查询与取消任务。

**关键类列表**：
- `VideoTaskDO` / `VideoResultDO` / `AiEngineJobDO` - 数据对象
- `VideoTaskVO` / `CreateVideoTaskCommand` / `CreateVideoTaskRequest` / `EngineCallbackPayload` - 视图/命令/回调对象
- `VideoTaskMapper` / `VideoResultMapper` / `AiEngineJobMapper` - 数据访问（MyBatis XML）
- `VideoTaskService` / `VideoTaskServiceImpl` - 业务服务（SUT）
- `VideoTaskController` / `OpenapiVideoTaskController` / `VideoEngineCallbackController` - 控制器
- `TaskStatusEnum` / `CallbackStatusEnum` - 状态机枚举
- `AiVideoEngineClient` / `MockAiVideoEngineClient` - 引擎客户端（Mock 占位）
- `VideoStorageService` / `DefaultVideoStorageService` - 结果存储（占位）

**依赖关系**：Service → Mapper / EngineClient / StorageService；Controller → Service。无外部服务依赖（引擎为 Mock）。

**已加载规范**：
- [x] naming.md
- [x] exception-logging.md
- [x] unit-testing.md
- [x] mysql.md
- [x] constants.md
- [x] project-structure.md
- [x] frontend-backend.md

---

## 🧪 TEST: video-generation-service

**测试文件**：
- `src/test/java/com/antdigital/video/service/impl/VideoTaskServiceImplTest.java`
- `src/test/java/com/antdigital/video/model/enums/TaskStatusEnumTest.java`

**测试方法列表**（`VideoTaskServiceImplTest`）：

| 方法 | 测试场景 | 状态 |
|------|----------|:----:|
| should_createTask_when_commandValid | 正常创建+提交引擎 | ⬜ 未运行 |
| should_returnExistingTask_when_idempotencyKeyHit | 幂等键命中 | ⬜ 未运行 |
| should_throwVT001_when_promptBlank | prompt 为空 | ⬜ 未运行 |
| should_throwVT001_when_durationOutOfRange | 时长超范围 | ⬜ 未运行 |
| should_getTask_when_tenantMatch | 租户匹配查询 | ⬜ 未运行 |
| should_throwVT005_when_tenantMismatch | 租户越权 | ⬜ 未运行 |
| should_throwVT004_when_taskNotExist | 任务不存在 | ⬜ 未运行 |
| should_cancel_when_statusNonTerminal | 非终态取消 | ⬜ 未运行 |
| should_throwVT006_when_cancelTerminalStatus | 终态取消 | ⬜ 未运行 |
| should_advanceToSucceeded_when_callbackSuccess | 回调成功推进 | ⬜ 未运行 |
| should_ignoreCallback_when_alreadyReceived | 回调幂等忽略 | ⬜ 未运行 |
| should_throwVT008_when_signatureInvalid | 回调验签失败 | ⬜ 未运行 |
| should_markFailed_when_engineSubmitFailsAllRetries | 引擎重试耗尽 | ⬜ 未运行 |

**测试覆盖摘要**：
- 被测类：`VideoTaskServiceImpl`、`TaskStatusEnum`
- 测试方法数：13（Service）+ 3（枚举）
- 覆盖场景：正常路径 ✓、参数校验 ✓、异常处理 ✓、边界值 ✓、幂等 ✓、租户隔离 ✓、状态机 ✓
- 说明：测试用 JUnit5 + Mockito（`MockitoExtension`）+ AssertJ；未编译运行，Mockito 严格桩模式下已静态剔除 `UnnecessaryStubbing` 隐患。

---

## 🔧 IMPL: video-generation-service

**已实现文件**：

构建/配置：
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/resources/db/schema.sql`
- `src/main/resources/mapper/VideoTaskMapper.xml`
- `src/main/resources/mapper/VideoResultMapper.xml`
- `src/main/resources/mapper/AiEngineJobMapper.xml`

入口与公共层：
- `src/main/java/com/antdigital/video/VideoApplication.java`
- `common/model/ApiResponse.java`、`common/model/PageResult.java`
- `common/constant/ErrorCodes.java`、`common/constant/VideoConsts.java`
- `common/exception/BusinessException.java`、`common/exception/GlobalExceptionHandler.java`

模型层：
- `model/enums/TaskStatusEnum.java`、`model/enums/CallbackStatusEnum.java`
- `model/entity/VideoTaskDO.java`、`VideoResultDO.java`、`AiEngineJobDO.java`
- `model/dto/CreateVideoTaskCommand.java`、`VideoTaskQuery.java`、`EngineCallbackPayload.java`
- `model/vo/VideoTaskVO.java`

DAO / 服务 / 控制层：
- `dao/mapper/VideoTaskMapper.java`、`VideoResultMapper.java`、`AiEngineJobMapper.java`
- `manager/engine/AiVideoEngineClient.java`、`MockAiVideoEngineClient.java`、`EngineGenerateRequest.java`、`EngineJobStatus.java`
- `manager/storage/VideoStorageService.java`、`DefaultVideoStorageService.java`
- `service/VideoTaskService.java`、`service/impl/VideoTaskServiceImpl.java`
- `api/CreateVideoTaskRequest.java`、`api/Responses.java`、`api/VideoTaskController.java`、`api/OpenapiVideoTaskController.java`、`api/VideoEngineCallbackController.java`

**编译验证**：⚠️ 环境受限（无 JDK/Maven）。

**核心实现说明**：
- 创建：prompt 非空且 ≤1024；durationSec 默认 3、范围 1~30；幂等键命中直接返回已有 ID；`taskNo = VT + yyyyMMddHHmmss + 4位序列`；insert 回填主键 → 提交引擎（重试 3 次）→ 写 `ai_engine_job`。
- 查询：按 id 查 + 租户校验（VT_004 / VT_005）；结果 URL 从 `video_result` 合并。
- 取消：仅 `isCancellable()`（CREATED/SUBMITTED/PROCESSING）可取消，否则 VT_006。
- 回调：验签（SHA-256 HMAC 风格）→ 幂等（RECEIVED 忽略）→ 终态忽略 → SUCCEEDED 写结果并推进 / FAILED 写错误 / PROCESSING 仅推进不置 RECEIVED。

---

## ✅ CHECK: video-generation-service

### L1 静态检查

| 检查项 | 规范要求 | 符合情况 |
|--------|----------|:--------:|
| 命名规范 | 类大驼峰、方法小驼峰、常量全大写、DO/DTO/VO 后缀 | ✅ |
| 包结构 | controller/service/manager/dao/model/common 分层 | ✅ |
| 异常日志 | SLF4J + `{}` 占位符、自定义 BusinessException | ✅ |
| 日志输出 | 禁止 System.out / printStackTrace（已 `rg` 扫描无命中） | ✅ |
| 安全规范 | SQL 全部 `#{}` 参数化、Controller `@Valid`、回调验签 | ✅ |
| MySQL规范 | 表名小写、必备 gmt_create/gmt_modified、resultMap、禁 SELECT *（已扫描无命中） | ✅ |
| 常量规范 | 常量类 final + 私有构造、魔法值收敛至 VideoConsts | ✅ |
| 单元测试 | 测试类存在、命名 `{Class}Test`、`should_xxx_when_xxx` | ✅ |
| 前后端规范 | JSON key lowerCamelCase、统一响应 code/msg/data | ✅ |

**静态发现并已修复**：
1. SHA-256 十六进制拼接对负 byte 扩位 → 加 `& 0xff` 掩码（实现+测试同步修复）。
2. PROCESSING 中间回调本会置 RECEIVED 阻塞终态回调 → 改为仅终态置 RECEIVED。
3. 测试严格桩的 `UnnecessaryStubbing`（幂等键 null 分支、验签失败前置）→ 移除无效 stub。

### L2 动态验证

| 验证项 | 状态 | 说明 |
|--------|:----:|------|
| 编译验证 | ⚠️ | 环境无 `java`/`javac`/`mvn`/`gradle`，跳过 |
| 单测验证 | ⚠️ | 同上，跳过 |

#### 📋 待人工验证

```bash
# 需本机安装 JDK 21 + Maven 3.9+
mvn compile -DskipTests
mvn test -Dtest=VideoTaskServiceImplTest,TaskStatusEnumTest
```

---

## 📝 DOCS: video-generation-service

**文档操作**：
- 架构文档：新建 `docs/ARCHITECTURE.md`
- 模块文档：新建 `docs/modules/video-generation-service/README.md`
- 编码报告：已写入本文件

---

## ⚠️ 已知限制 / TODO（需人工复核）

1. **验证未执行**：所有编译/单测结论标「未运行」，本报告为静态审查结果，正式合入前须在本机跑 `mvn test`。
2. **错误码桥接**：常量名沿用 design 的 `VT_001~VT_009`，值映射为规范 5 位 `A/B/C+4位`（如 `VT_001="A0001"`）；`VT_007~VT_009` 为设计之外新增，需确认是否被调用方接受。
3. **HTTP 状态码**：`GlobalExceptionHandler` 对业务/校验异常返回 HTTP 200，与 frontend-backend.md「错误响应应含非 2xx HTTP status」存在冲突，需产品/网关侧确认约定后调整。
4. **引擎/存储为占位实现**：`MockAiVideoEngineClient`、`DefaultVideoStorageService` 仅演示用；真实对象存储签名（design R10）与引擎 HTTPS 接入未实现。
5. **tenantId 来源**：控制器取 `X-Tenant-Id` 请求头，缺失会产生 `MissingRequestHeaderException`（落入 500 处理），真实网关鉴权未接入。
6. **`ai_engine_job.raw_payload` jdbcType**：schema 为 TEXT，XML resultMap 写 VARCHAR，影响极小，CHECK 建议后续更正为 LONGVARCHAR。
7. **`VideoTaskQuery` 疑似冗余**：Service `list` 使用独立参数，未复用该 DTO，可后续删除或接入。

## 产物清点（本次节点，均在 cwd 工作区，未 git 提交）
- 工程源码/配置/资源：38 个文件（`src/main/**`、`pom.xml`）
- 单元测试：2 个文件（`src/test/**`）
- 编码报告：本文件
- 架构文档：`docs/ARCHITECTURE.md`
- 模块文档：`docs/modules/video-generation-service/README.md`