# Code Review Checklist

> **Change** 快速排序算法实现 · **分支/Commit** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-b27f693b-1577-4173-b769-156647714829` · **日期** `2026-10-29`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。

---

## Step 1 — 执行队列（产物 A）

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|-----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|-----|--------|
| 1 | `pom.xml` | Maven 构建配置 | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |
| 2 | `src/main/java/com/example/algorithm/sort/QuickSort.java` | 快速排序核心实现 | ⚠️ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ |
| 3 | `src/test/java/com/example/algorithm/sort/QuickSortTest.java` | 单元测试 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ |

---

## Step 2 — 功能（产物 B）

> 需求来源：《实现一个快速排序算法》

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | 对 `int[]` 进行原地快速排序 | 需求描述：实现一个快速排序算法 | `QuickSort.java` | ✅ | `QuickSort.sort(int[])` 第 28 行；测试 `should_sortCorrectly_when_givenRandomArray` 通过 |
| REQ-2 | 排序结果为非递减顺序 | 需求描述：实现一个快速排序算法 | `QuickSort.java` | ✅ | 所有测试用例均断言排序后数组按非递减顺序排列 |
| REQ-3 | 处理空数组、单元素数组等边界 | 需求描述：实现一个快速排序算法 | `QuickSort.java` | ✅ | 第 30-32 行对 `array.length < 2` 提前返回；测试覆盖空数组、单元素数组 |
| REQ-4 | 对 `null` 输入进行防御性校验 | 需求描述：实现一个快速排序算法 | `QuickSort.java` | ✅ | 第 29 行 `Objects.requireNonNull(array, "array must not be null")`；测试 `should_throwException_when_givenNullArray` 通过 |
| REQ-5 | Javadoc 描述与代码实现一致 | 需求描述：实现一个快速排序算法；`QuickSort.java:8-9` | `QuickSort.java` | ❌ | 文档声称使用 Hoare 分区方案，但 `partition` 实现（第 61-72 行）为 Lomuto 分区方案（选取最右元素为 pivot，单向扫描） |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注 |
|----|--------|------|------|
| A1 | 源文件格式 | ✅ | 文件名与顶层类名一致；编码 UTF-8；无 Tab |
| A2 | 源文件结构/import 顺序 | ✅ | 顺序正确；无 `import *`；静态 import 分组正确 |
| A3 | 代码样式 | ✅ | K&R 大括号；4 空格缩进；行宽 ≤ 120；运算符空格规范 |
| A4 | 命名规范 | ✅ | 包名全小写；类名 UpperCamelCase；方法/变量 lowerCamelCase |
| A5 | 编码实践 | ✅ | 无空 catch；静态方法通过类名调用；无 `finalize` 重写 |
| A6 | 特定元素样式 | ✅ | 数组方括号属于类型；修饰符顺序符合规范 |
| A7 | Javadoc 规范 | ✅ | public 类和方法均有 Javadoc；块标记顺序正确 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

> 预扫结果：`scan-all-rules.sh` 对 `pom.xml` 报出 2 条 `[P0] S1.1 — MyBatisSqlInjection`，经人工复核为 pom.xml 依赖配置误报，与 MyBatis SQL 注入无关。其余规则未命中。

| ID | 状态 | 备注 |
|----|------|------|
| B001 ~ B081 | ✅ | 无命中 |
| M001 ~ M027 | ✅ | 无命中 |
| I001 ~ I010 | ✅ | 无命中 |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 ~ G1.4 | N/A | 无并发/锁场景 |
| G2.1 ~ G2.3 | N/A | 无幂等/写接口场景 |
| G3.1 ~ G3.2 | N/A | 无事务/分布式事务 |
| G4.1 ~ G4.3 | N/A | 无 SQL |
| G5.1 | N/A | 无 MQ |
| G6.1 ~ G6.2 | N/A | 无缓存 |
| G7.1 ~ G7.2 | N/A | 无调度任务 |
| G8.1 ~ G8.6 | N/A | 无 I/O、线程池、ThreadLocal |
| G9.1 ~ G9.3 | N/A | 无网络/RPC/DB 调用 |
| G10.1 ~ G10.2 | N/A | 无接口契约 |
| G11.1 | ✅ | 新增逻辑有单测且断言完整 |
| G11.2 | ⚠️ | 已覆盖空、单元素、已排序、逆序、重复、负数；未覆盖大数组/性能边界、极大/极小整数 |
| G11.3 | ✅ | `Objects.requireNonNull` 对 null 入参做了防御性校验 |
| G11.4 | N/A | 无金额/浮点运算 |
| G12.1 ~ G12.2 | N/A | 无资损场景 |
| G13.1 | N/A | 无日志 |
| G14.1 ~ G14.4 | N/A | 无国际化/时区/金额 |
| G15.1 ~ G15.3 | N/A | 无 schema/接口变更 |
| G16.1 ~ G16.4 | N/A | 无日志/监控 |
| G17.1 ~ G17.3 | N/A | 无开关/降级 |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 ~ S1.3 | N/A | 无 SQL/MyBatis |
| S2.1 ~ S2.3 | N/A | 无 XSS 输出场景 |
| S3.1 ~ S3.3 | N/A | 无 SSRF |
| S4.1 ~ S4.2 | N/A | 无命令执行 |
| S5.1 ~ S5.2 | N/A | 无 XML/XPath |
| S6.1 ~ S6.3 | N/A | 无反序列化 |
| S7.1 ~ S7.3 | N/A | 无文件上传/下载 |
| S8.1 ~ S8.4 | N/A | 无访问控制接口 |
| S9.1 ~ S9.4 | N/A | 无密钥/凭证/随机数 |
| S10.1 ~ S10.3 | N/A | 无 CSRF/CORS/跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 ~ U2.3 | N/A | 未启用自定义规则 |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3`、**S1–S10 / G1–G17** 各列均非 ``；
- [x] Step 2 的每个 REQ/Scenario 均非 `⬜`；
- [x] Step 3 的 A1–A7 均非 `⬜`；
- [x] Step 4 全部 **G/S** 与 **B001–B081 / M001–M027 / I001–I010** ID 均非 `⬜`；
- [x] Step 5 全部 U* ID 均非 ``；
- [x] 所有 `❌/⚠️` 已写入 report，且包含 `ID + path:line`。
