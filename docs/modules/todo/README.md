# todo 模块

## 模块职责

提供待办事项的新增能力（内部用户记录日常待办）：用户提交事项名称（必填）与描述（选填），服务校验后落库并返回创建结果。

## 关键类

| 类 | 职责 |
|----|------|
| `TodoDO` | 待办事项数据对象，对应表 `todo` |
| `TodoCreateRequest` | 新增请求对象（name/description，JSR-303 校验） |
| `TodoVO` | 待办事项视图对象（id/name/description/gmtCreate） |
| `TodoMapper` + `TodoMapper.xml` | MyBatis 数据访问（insert，自增主键回填） |
| `TodoService` / `TodoServiceImpl` | 业务服务：参数校验、落库、DO→VO 转换 |
| `TodoController` | `POST /api/todos/create` REST 接口 |
| `Result` / `BizException` / `GlobalExceptionHandler` | 统一响应与全局异常处理 |

## 依赖关系

Controller → Service → DAO(MyBatis) → MySQL（表 `todo`，见 `src/main/resources/db/schema.sql`）。无外部服务依赖。

## API 接口列表

### POST /api/todos/create

新增待办事项（当前最小闭环仅此一个接口）。

请求体：

```json
{
  "name": "事项名称（必填，<=64字符）",
  "description": "事项描述（选填，<=512字符）"
}
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { "id": 100, "name": "买牛奶", "description": "每周两箱", "gmtCreate": "2026-09-24T12:00:00.000+08:00" }
}
```

失败响应（统一错误码 A/B/C + 4 位数字，错误码不作为用户提示文案）：

| code | 场景 |
|------|------|
| A0400 | 请求体校验失败（@Valid） |
| A0001 | 名称空白 / 请求为空 |
| A0002 | 名称超过 64 字符 |
| A0003 | 描述超过 512 字符 |
| B0002 | 数据库写入失败 |
| B0001 | 未预期系统异常 |
