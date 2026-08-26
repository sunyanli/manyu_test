# 代码评审报告 — 组织架构管理模块

> 评审阶段：loop-2 (代码评审)
> 评审时间：2026-08-25
> 目标仓库：`manyu_test`（主仓），`manyu_test1`（本次不涉及）
> 评审范围：全量代码（models / schemas / routers / services / middleware / utils / tests / config）
> 评审方式：静态审查（环境无 pytest，按降级协议切换）

---

## 一、评审总览

### 1.1 代码规模

| 模块 | 文件数 | 总行数 | 状态 |
|------|--------|--------|------|
| Models | 4 (base/department/employee/transfer_record) | 129 | ✅ |
| Schemas | 4 (common/department/employee/transfer) | 184 | ⚠️ |
| Routers | 2 (departments/employees) | 265 | ✅ |
| Services | 3 (department/employee/approval_callback) | 501 | ⚠️ |
| Middleware | 1 (auth) | 40 | ⚠️ |
| Utils | 1 (exceptions) | 54 | ✅ |
| Config/DB/Main | 3 (config/database/main) | 103 | ✅ |
| Tests | 4 (conftest + 3 test files) | 572 | ✅ |
| **合计** | **22** | **~1848** | — |

### 1.2 需求对照矩阵

| 需求 | 接口 | 实现状态 | 评审结论 |
|------|------|---------|---------|
| 需求1：部门树懒加载 | `GET /api/departments/tree` | ✅ 已实现 | 通过 |
| 需求1：拖拽调整父部门 | `PUT /api/departments/{id}/move` | ✅ 已实现 | 通过 |
| 需求2：实时唯一性校验 | `GET /api/employees/check` | ✅ 已实现 | ⚠️ 有改进空间 |
| 需求2：员工新增 | `POST /api/employees` | ✅ 已实现 | 通过 |
| 需求3：人员调动 | `POST /api/employees/{id}/transfer` | ✅ 已实现 | ⚠️ 有改进空间 |
| 需求3：调动留痕 | `GET /api/employees/{id}/transfers` | ✅ 已实现 | 通过 |
| 需求4：员工离职 | `PUT /api/employees/{id}/resign` | ✅ 已实现 | 通过 |
| 需求4：状态筛选 | `GET /api/employees?status=` | ✅ 已实现 | 通过 |
| 权限控制 | 中间件 auth.py | ⚠️ 部分实现 | ⚠️ 有缺失 |

---

## 二、问题清单

### 🔴 Blocker（P0 — 必须修复，共 1 个）

#### B1. `update_employee` 未阻止编辑已离职员工

- **文件**: `[manyu_test] app/services/employee_service.py:128-150`
- **严重程度**: 🔴 Blocker
- **需求依据**: 需求4 — "离职人员显示灰色标签，不可编辑"
- **现象**: `update_employee()` 函数仅校验员工是否存在（`emp is None`），未校验 `emp.status`。已离职员工（status=2）仍可通过 `PUT /api/employees/{id}` 编辑姓名、手机号、职位。
- **对比**: `transfer_employee()` (L167) 和 `resign_employee()` (L211) 均正确校验了 `emp.status != 1`。
- **修复建议**:
  ```python
  # 在 employee_service.py update_employee() 中，emp = await db.get(...) 之后加入：
  if emp.status != 1:
      raise BadRequestException("已离职员工不可编辑")
  ```
- **测试覆盖**: `tests/test_employees.py` 缺少"编辑已离职员工应被拒绝"的测试用例。

---

### 🟡 Medium（P1 — 建议修复，共 4 个）

#### M1. 部门主管管辖范围过滤未实现

- **文件**: `[manyu_test] app/middleware/auth.py:36-40`, `app/routers/departments.py`, `app/routers/employees.py`
- **严重程度**: 🟡 Medium
- **需求依据**: 需求角色 — "部门主管：仅可查看本部门及下属部门的人员，可编辑本部门人员部分信息"；设计文档 6.3 节
- **现象**: `require_dept_manager_or_above()` 仅校验角色级别，未根据当前用户的管辖部门 ID 过滤数据。部门主管实际可查看全公司所有部门及员工。
- **影响范围**:
  - `GET /api/departments/tree` — 部门主管可看到全公司部门树
  - `GET /api/employees` — 部门主管可看到全公司员工
  - `GET /api/employees/{id}` — 部门主管可查看任意员工详情
  - `PUT /api/employees/{id}` — 部门主管可编辑任意员工
- **设计预期**: 设计文档 6.3 节给出了伪代码 — 通过 `departments.path` 物化路径匹配验证管辖范围。当前完全缺失。
- **修复建议**: 
  1. 在 `get_current_user()` 中增加 `managed_dept_id` 字段
  2. 在路由/Services 层增加管辖范围过滤逻辑
  3. 或作为已知限制，在 MVP 阶段文档中明确标注

#### M2. Alembic 数据库迁移脚本缺失

- **文件**: 整个 `alembic/` 目录不存在
- **严重程度**: 🟡 Medium
- **需求依据**: 设计文档 4.2 节目录结构明确包含 `alembic/`；技术选型约束包含 Alembic ≥1.12
- **现象**: 当前仅通过 `auto_create_tables=True` + `Base.metadata.create_all` 自动建表，无可追溯的版本化迁移脚本。生产环境无法安全执行 schema 变更。
- **修复建议**: 执行 `alembic init alembic`，生成初始迁移脚本，将 `auto_create_tables` 仅用于开发/测试环境。

#### M3. `check_unique` 端点未使用 Pydantic Schema 校验输入

- **文件**: `[manyu_test] app/routers/employees.py:24-32`, `app/schemas/employee.py:12-14`
- **严重程度**: 🟡 Medium
- **需求依据**: 设计文档 5.3 节 — "Pydantic Schema 校验"
- **现象**: 
  - `EmployeeCheckRequest` Schema 定义了 `field: str = Field(..., pattern=r"^(employeeNo|phone)$")` 但路由中未使用，直接用 `Query(...)` 接收裸参数。
  - 对 `field` 参数的校验下推到 service 层，由 `check_unique()` 中 if/else 处理，不符合"校验在边界完成"的最佳实践。
  - 路由中 `is_exist` → 手动映射为 `isExist`（camelCase），而 `EmployeeCheckResult` schema 定义的 `is_exist` (snake_case) 完全未使用。
- **修复建议**: 使用 `EmployeeCheckRequest` 作为 Query 依赖或显式校验，在 Pydantic 层完成 field 校验。

#### M4. `requirements.txt` 存在重复依赖声明

- **文件**: `[manyu_test] requirements.txt:9,13`
- **严重程度**: 🟡 Medium（代码质量）
- **现象**: `httpx>=0.24.0` 在第 9 行和第 13 行重复声明。
- **修复建议**: 删除重复行。

---

### 🟢 Low（P2 — 改进建议，共 3 个）

#### L1. `approval_callback` 缺少 `triggerTime` 字段

- **文件**: `[manyu_test] app/services/approval_callback.py:24-33`
- **严重程度**: 🟢 Low
- **需求依据**: 设计文档 9.3 节审批回调契约包含 `triggerTime` 字段
- **现象**: 回调请求体中缺少 `triggerTime` 字段（设计文档约定的 ISO 8601 时间戳）。
- **修复建议**: 在回调 JSON 中增加 `"triggerTime": datetime.utcnow().isoformat()`。

#### L2. `_cascade_update_children` SQL LIKE 潜在通配符问题

- **文件**: `[manyu_test] app/services/department_service.py:215`
- **严重程度**: 🟢 Low（风险极低，path 为内部生成）
- **现象**: `Department.path.like(f"{old_path}/%")` — 如果 `old_path` 包含 SQL 通配符 `%` 或 `_`，LIKE 会误匹配。当前 path 由 `/` + 数字 ID 组成，不存在此问题。
- **修复建议**: 长期看，可考虑对 path 中的特殊字符做转义或使用 `startswith` 等价查询（如 `SUBSTRING` + `=`）。

#### L3. 测试覆盖缺口

- **文件**: `[manyu_test] tests/`
- **严重程度**: 🟢 Low
- **具体缺口**:
  - 缺少"编辑已离职员工应被拒绝"测试用例
  - 缺少"部门主管越权操作"测试用例（如部门主管尝试新增员工）
  - 缺少"部门主管仅查看管辖范围"测试用例
  - 缺少"check_unique 非法 field 参数"测试用例
- **修复建议**: 补充上述测试用例，提升覆盖率至 ≥80% 目标。

---

## 三、优点总结

### ✅ 设计与实现对齐良好

| 方面 | 评价 |
|------|------|
| **数据模型** | 邻接表 + 物化路径双模式设计合理，`parent_id` 支持懒加载，`path` 支持子树快速定位和循环引用检测 |
| **唯一性保障** | 应用层 `check_unique()` + DB 唯一索引 `uk_employee_no` / `uk_phone` 双保底，防并发竞态 |
| **循环引用检测** | `move_department` 中 `new_parent.path.startswith(dept.path + "/")` 检测准确 |
| **深度限制** | 移动时计算 `subtree_depth + new_level - 1 > max_dept_level`，逻辑正确 |
| **软删除设计** | 部门 `status=0`、员工 `status=2`，保留历史数据可追溯 |
| **调动事务** | 同一 flush 内完成 dept_id 更新 + transfer_record 写入，逻辑内聚 |
| **审批回调解耦** | `asyncio.create_task` 异步通知，失败不阻塞主流程 |
| **异常体系** | 自定义 `AppException` + 子类（NotFound/Conflict/Forbidden/BadRequest），统一处理器 |
| **测试设计** | 使用 SQLite 内存数据库 + `dependency_overrides`，隔离性好，覆盖核心场景 |
| **响应格式** | 统一 `{code, data, msg}` 贯穿所有接口 |

### ✅ 需求功能覆盖

- 需求1（部门树懒加载 + 拖拽）：完整实现，含 `hasChildren` 标记、循环引用检测、深度校验、级联更新子孙节点
- 需求2（唯一性校验 + 新增）：完整实现，含手机号格式校验、部门存在性校验
- 需求3（人员调动 + 留痕）：完整实现，含调动记录写入、审批回调通知
- 需求4（员工离职 + 状态筛选）：完整实现，含逻辑删除、状态过滤、重复离职防护

---

## 四、跨仓对齐点检查

| 对齐项 | 状态 | 说明 |
|--------|:----:|------|
| `manyu_test` ↔ `manyu_test1` API 契约 | N/A | `manyu_test1` 本次无代码变更，后续通过 `/openapi.json` 消费 |
| 统一响应格式 `{code, data, msg}` | ✅ | 所有接口返回一致 |
| 错误码枚举 | ✅ | `exceptions.py` 定义完整，覆盖 400/403/404/409/422 |
| 审批回调接口契约 | ⚠️ | 缺少 `triggerTime` 字段（见 L1） |
| `manyu_test1` 仓库状态 | ✅ | 仅含 `README.md`，无代码变更，无冲突 |

---

## 五、评审结论

| 维度 | 评级 | 说明 |
|------|:----:|------|
| 功能完整性 | ⭐⭐⭐⭐ | 四大核心需求全部实现，仅 1 个 Blocker（离职员工编辑未拦截） |
| 代码质量 | ⭐⭐⭐⭐ | 分层清晰，命名规范，异常处理完整；少量 Schema 未使用、重复依赖 |
| 安全性 | ⭐⭐⭐ | 鉴权架构完整但部门主管范围过滤缺失；`check_unique` 无频率限制 |
| 可维护性 | ⭐⭐⭐ | 缺 Alembic 迁移脚本，DB schema 变更不可追溯 |
| 测试覆盖 | ⭐⭐⭐ | 核心场景覆盖良好，但缺少越权/边界场景测试 |

**综合评级**: ⭐⭐⭐⭐ (4/5) — **建议修复 Blocker 后合并**

---

## 六、附件

### 6.1 问题统计

| 级别 | 数量 | 编号 |
|------|:----:|------|
| 🔴 Blocker | 1 | B1 |
| 🟡 Medium | 4 | M1, M2, M3, M4 |
| 🟢 Low | 3 | L1, L2, L3 |
| **合计** | **8** | — |

### 6.2 审查文件清单

```
app/__init__.py
app/main.py
app/config.py
app/database.py
app/models/base.py
app/models/department.py
app/models/employee.py
app/models/transfer_record.py
app/schemas/common.py
app/schemas/department.py
app/schemas/employee.py
app/schemas/transfer.py
app/routers/departments.py
app/routers/employees.py
app/services/__init__.py
app/services/department_service.py
app/services/employee_service.py
app/services/approval_callback.py
app/middleware/auth.py
app/utils/exceptions.py
tests/conftest.py
tests/test_departments.py
tests/test_employees.py
tests/test_transfers.py
requirements.txt
pyproject.toml
```

---

> 报告版本：v1.0 | 评审者：DTCoder | 状态：审查完成，待修复 B1