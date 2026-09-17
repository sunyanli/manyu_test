# video-generation-service 模块

## 模块职责

接收文生视频提示词（如「小猫喵喵叫」），创建视频生成任务，提交外部引擎并经由回调推进状态机，支持幂等、租户隔离、查询与取消。

## 关键类

- `VideoTaskServiceImpl`：核心业务（校验、幂等、状态机、回调）。
- `VideoTaskController` / `OpenapiVideoTaskController`：创建与查询接口。
- `VideoEngineCallbackController`：引擎回调入口。
- `VideoTaskMapper` / `VideoResultMapper` / `AiEngineJobMapper`：数据访问。
- `TaskStatusEnum` / `CallbackStatusEnum`：状态机。
- `MockAiVideoEngineClient` / `DefaultVideoStorageService`：外部依赖占位实现。

## 依赖关系

- 服务依赖 MyBatis Mapper、引擎客户端、存储服务。
- 无外部服务依赖（引擎为 Mock，存储为 URL 拼接占位）。

## API 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/video-tasks` | 创建任务（Web） |
| GET | `/api/v1/video-tasks/{taskId}` | 查询任务详情 |
| GET | `/api/v1/video-tasks` | 分页列表 |
| POST | `/api/v1/video-tasks/{taskId}/cancel` | 取消任务 |
| POST | `/openapi/v1/video-tasks` | 创建任务（OpenAPI） |
| GET | `/openapi/v1/video-tasks/{taskId}` | 查询任务（OpenAPI） |
| POST | `/internal/v1/video-callback` | 引擎回调 |

请求头：`X-Tenant-Id` 标识租户。