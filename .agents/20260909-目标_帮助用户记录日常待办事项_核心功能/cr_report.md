# Code Review Report

> **Change** `todo-app` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-bd826ef0-b543-432d-b1ad-b675d86217fd` / `a43da5b` · **日期** `2026-09-09` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**。本次变更为 **Python 项目**（FastAPI + SQLAlchemy + SQLite），原 Java 检查规则中 Java 特定项已标记 `N/A`。问题须含 `path:line` 或清单 ID。
>
> **预扫**：`scan-all-rules.sh src/` — 无命中（52 条可程序化规则全部通过）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.py` 文件数 | `15`（含 7 个空 `__init__.py`） |
| 变更行数 | `+911 / -0` |

| 类/模块 | 路径 | 角色 |
|---------|------|------|
| `TodoItem` | `src/model/entity/TodoItem.py` | 待办事项实体 |
| `User` | `src/model/entity/User.py` | 用户实体 |
| `TodoRequest` | `src/model/dto/TodoRequest.py` | 新增待办请求 DTO |
| `TodoResponse` | `src/model/dto/TodoResponse.py` | 待办响应 DTO |
| `TodoRepository` | `src/repository/todo_repository.py` | 数据访问层 |
| `TodoService` | `src/service/todo_service.py` | 业务服务层 |
| `AuthService` | `src/service/auth_service.py` | 认证服务层 |
| `TodoController` | `src/controller/todo_controller.py` | 待办路由控制器 |
| `AuthController` | `src/controller/auth_controller.py` | 认证路由控制器 |
| `models` | `src/models.py` | SQLAlchemy ORM 模型 |
| `database` | `src/database.py` | 数据库连接管理 |
| `main` | `src/main.py` | FastAPI 应用入口 |
| `TestTodoServiceCreate` | `tests/test_todo_service.py` | 待办服务单测 |
| `TestAuthServiceLogin/VerifyToken` | `tests/test_auth_service.py` | 认证服务单测 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|----|
| 3 | 3 | 2 |

---

## 3. Step 2 — 功能（REQ）

### REQ-1: 新增待办事项（F01）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 合法 title+description，When 调用 create，Then 返回创建的 TodoItem | ⚠️ | design.md §5.2.3.1 R01-R03 | `todo_service.py:40-59` | 参数校验（R01/R02/R03）**已实现且正确**；但数据写入内存列表 `_todo_list` 而非数据库，`set_repository` 注入的 repository 未被使用，**数据不持久化** |
| Given title 为空/纯空格，When 调用 create，Then 返回 TODO_001 | ✅ | design.md §5.2.3.1 R01 | `todo_service.py:28-31` | 正确校验并抛出 `ValueError` |
| Given title 超 200 字符，When 调用 create，Then 返回 TODO_002 | ✅ | design.md §5.2.3.1 R02 | `todo_service.py:32-33` | 正确校验 |
| Given description 超 2000 字符，When 调用 create，Then 返回 TODO_003 | ✅ | design.md §5.2.3.1 R03 | `todo_service.py:35-38` | 正确校验 |

### REQ-2: 查看待办事项列表（F02）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 用户有待办，When 请求列表，Then 返回该用户所有待办（按创建时间倒序、分页） | ⚠️ | design.md §5.2.3.2 R04-R07 | `todo_service.py:61-65` | 仅按 `user_id` 过滤，**无排序**（未实现 R05 倒序），**无分页**（忽略 page/page_size 参数），直接从内存列表返回 |

### REQ-3: 查看待办事项详情（F03）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 待办存在且属于当前用户，When 请求详情，Then 返回该待办 | ⚠️ | design.md §5.2.3.3 R08 | `todo_service.py:67-72` | 从内存列表查找，无数据库查询 |

### REQ-4: 用户登录（F04）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 正确凭据，When 登录，Then 返回 Token | ⚠️ | design.md §5.2.2 W01 | `auth_service.py:51-74` | 密码哈希验证逻辑正确；但用户数据存储在内存 `self._users`，进程重启即丢失 |
| Given Token 有效，When 校验，Then 返回用户 | ⚠️ | design.md §5.2.2 W01 | `auth_service.py:76-98` | Token 存储在内存 `self._tokens`，进程重启即失效 |

### REQ-5: Token 鉴权保护

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 无 Token 或无效 Token，When 访问 /api/todo，Then 返回 AUTH_002 | ❌ | design.md §5.2.3.1 异常场景 | `todo_controller.py:29` | **`current_user_id: int = 1` 硬编码**，无 Token 校验机制，任何人可访问任何用户的待办数据 |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明（违规写 `Ax.x` 与 `path:行`） |
|------|--------------------------------|
| ✅ | Python 命名规范：类名 UpperCamelCase、函数/变量 lower_snake_case、常量 UPPER_SNAKE_CASE，均符合 |
| ⚠️ | `todo_service.py:18-22` — `TodoService.__init__` 中 `todo_list` 参数仅用于测试场景，但参数名和 docstring 未明确说明，容易误导使用者以为这是通用构造参数 |

---

## 5. Step 4 — 可靠性检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 可靠性 | `reliability-checklist.md` G1–G17 | ⚠️ | P1 | G4.3: `todo_service.py:61-65` 列表查询无分页无排序；G8.1: `auth_service.py:19-20` 内存存储用户/Token 无持久化；G16.1: 无日志记录 |
| 安全 | `security-checklist.md` S1–S10 | ❌ | P0 | **S8.1**: `todo_controller.py:29` 硬编码 `current_user_id=1`，无鉴权，存在**水平越权**漏洞 |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I | N/A(非Java) | — | Python 项目，Java 特定规则不适用 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | N/A(未启用自定义规则) | — | 项目无自定义规则配置 |

---

## 7. 结论

- **合并建议**：**阻止合并**
- **P0**：
  1. `todo_controller.py:29` — 所有 `/api/todo` 接口硬编码 `current_user_id=1`，**无 Token 鉴权**，任何人可创建/查看任何用户的待办事项（S8.1 水平越权）
  2. `todo_service.py:40-59` — `create_todo` 写入内存列表而非数据库，**数据不持久化**，进程重启即丢失
  3. `auth_service.py:19-20` — 用户/Token 存储在内存字典，**无持久化**，重启后所有用户数据和 Token 失效
- **P1**：
  1. `todo_service.py:61-65` — `list_todos` 无排序（R05 要求按创建时间倒序）、无分页（R06/R07）
  2. `auth_controller.py:38` — `get_current_user` 硬编码 `current_user_id=1`
  3. `todo_service.py:18-22` — `todo_list` 参数设计混淆测试与生产场景
- **P2**：
  1. `src/models.py:32` — `TodoItem.user_id` 缺少 `ForeignKey` 约束（design.md §3 定义了 FK）
  2. 全项目无 logging 记录（关键操作无审计日志）
- **一句话**：核心功能逻辑（参数校验）正确，但**数据层完全未接入数据库**、**鉴权完全缺失**，属于不可交付状态。

---

## 7.1 问题片段

- **P0** `S8.1` `src/controller/todo_controller.py:29` — 硬编码用户 ID，无鉴权，水平越权漏洞。
  片段范围：`src/controller/todo_controller.py:25-36`

```python
L25|@router.post("")
L26|def create_todo(
L27|    request: TodoRequest,
L28|    service: TodoService = Depends(get_todo_service),
L29|    current_user_id: int = 1,  # 问题：硬编码用户ID，无Token校验
L30|) -> TodoResponse:
L31|    """新增待办事项。"""
L32|    try:
L33|        item = service.create_todo(user_id=current_user_id, request=request)
L34|    except ValueError as exc:
L35|        raise HTTPException(status_code=400, detail=str(exc))
L36|    return TodoResponse.from_entity(item)
```

- **P0** `G8.1` `src/service/todo_service.py:40-59` — 数据写入内存列表而非数据库，不持久化。
  片段范围：`src/service/todo_service.py:40-59`

```python
L40|    def create_todo(self, user_id: int, request: TodoRequest) -> TodoItem:
L41|        """创建一条新的待办事项。"""
L48|        self._validate_title(request.title)
L49|        self._validate_description(request.description)
L50|
L51|        self._next_id += 1
L52|        item = TodoItem(
L53|            id=self._next_id,
L54|            title=request.title,
L55|            description=request.description,
L56|            user_id=user_id,
L57|        )
L58|        self._todo_list.append(item)  # 问题：写入内存列表，未使用 repository
L59|        return item
```

- **P0** `G8.1` `src/service/auth_service.py:17-20` — 用户/Token 内存存储，无持久化。
  片段范围：`src/service/auth_service.py:14-26`

```python
L14|class AuthService:
L15|    """认证服务 — 用户登录与 Token 校验。"""
L16|
L17|    def __init__(self):
L18|        """Initialize with an in-memory user store (for testing)."""
L19|        self._users: Dict[str, User] = {}      # 问题：内存存储用户
L20|        self._tokens: Dict[str, Dict] = {}      # 问题：内存存储Token
```

- **P1** `G4.3` `src/service/todo_service.py:61-65` — 列表查询无排序无分页。
  片段范围：`src/service/todo_service.py:61-65`

```python
L61|    def list_todos(self, user_id: int, page: int = 1, page_size: int = 20) -> List[TodoItem]:
L62|        """查询某用户的所有待办事项，按创建时间倒序排列。"""
L63|        if page_size > 100:
L64|            page_size = 100
L65|        return [item for item in self._todo_list if item.user_id == user_id]  # 问题：无排序、无分页
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `src/controller/todo_controller.py:29` — 移除硬编码 `current_user_id=1`，实现 Token 校验依赖注入（从 Header 提取 Token → 调用 `AuthService.verify_token` → 获取真实 user_id）
- [ ] **P0** `src/service/todo_service.py:40-59` — `create_todo` 方法中优先使用 `self._repository.create()` 持久化到数据库，仅在无 repository 时回退到内存列表
- [ ] **P0** `src/service/auth_service.py:17-20` — 用户和 Token 存储接入数据库（通过 repository 层），替代内存字典

### P1

- [ ] **P1** `src/controller/auth_controller.py:38` — `get_current_user` 移除硬编码 `current_user_id=1`，实现 Token 校验
- [ ] **P1** `src/service/todo_service.py:61-65` — `list_todos` 优先使用 `self._repository.list_by_user()` 实现排序（`ORDER BY gmt_create DESC`）和分页
- [ ] **P1** `src/service/todo_service.py:18-22` — 重构构造函数，将测试专用路径与生产路径分离（如工厂方法 `TodoService.for_testing()` / `TodoService.for_production()`）

### P2（可选）

- [ ] **P2** `src/models.py:32` — 为 `TodoItem.user_id` 添加 `ForeignKey("user.id")` 约束
- [ ] **P2** 全项目 — 在关键操作（登录、创建待办）中添加 `logging` 日志记录
