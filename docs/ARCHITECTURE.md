# 架构文档：video-generation-service

## 模块边界

`video-generation-service` 是单体 Spring Boot 服务，提供「文生视频」任务的全生命周期管理：创建、幂等去重、租户隔离、外部引擎提交、回调推进状态机、查询与取消。

## 分层

| 层 | 包 | 职责 |
|----|----|------|
| 入口 | `api` | REST 控制器（Web / OpenAPI / 回调） |
| 服务 | `service` / `service.impl` | 业务编排与状态机（SUT） |
| 领域 | `manager` | 外部依赖抽象（引擎、存储） |
| 数据 | `dao.mapper` | MyBatis Mapper 接口 + XML |
| 模型 | `model` | 实体 / DTO / VO / 枚举 |
| 公共 | `common` | 响应 / 异常 / 常量 |

## 核心状态机

```
CREATED -> SUBMITTED -> PROCESSING -> SUCCEEDED
                            \       -> FAILED
CREATED / SUBMITTED / PROCESSING -> CANCELLED
```

## 模块列表

| 模块 | 说明 | 模块文档 |
|------|------|----------|
| video-generation-service | 小猫喵喵叫文生视频服务 | [README](modules/video-generation-service/README.md) |

## 约束

- MySQL 表名/字段小写、必备 `gmt_create`/`gmt_modified`、索引前缀 `idx_`/唯一键 `uk_`。
- 错误码 5 位：来源（A 用户 / B 系统 / C 第三方）+ 四位编号。
- 单元测试 JUnit5 + Mockito + AssertJ，命名 `should_xxx_when_xxx`。
- SQL 一律 `#{}` 参数化，禁止 `SELECT *`。