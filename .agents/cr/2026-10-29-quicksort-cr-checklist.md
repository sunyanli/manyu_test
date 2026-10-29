# Code Review Checklist

> **Change** `排序算法 sortkit（快速排序健壮性强化 · round 3 评审）` · **分支/Commit** `task/DEV-966dcd0a…` / `57cc8d1` · **日期** `2026-10-29`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
>
> **技能守卫与适配声明（强制记录）**：本次变更**不含任何 `.java` 文件**（`sortkit.py` / `test_sortkit.py` 为 Python 3；仓库无 Java 工程、`mvn`/`java` 不可用）。按 `dtazziboot-java-code-review` 的 Java 守卫，Java 专属清单（A1–A7 中 JVM 专有项、B/M/I 阿里 Java 规约、S1–S10 Web/Java 攻击面）标注 `N/A(非 Java)`；为不中断流水线，按同一 SDD 清单框架在 **Python 语义**层面完成 Step 2（功能）／Step 3（可读性等价项）／Step 4（可靠性等价项）／Step 5 的等价核销。
>
> **执行顺序**：`scan-all-rules.sh` 已先于 LLM 逐文件审查执行，输出见 §4.0。

---

## Step 1 — 执行队列（产物 A）

**变更范围**：`git show --stat 57cc8d1` → `sortkit.py`、`test_sortkit.py`、`impl.md`、`__pycache__/*.pyc ×2`。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G8 | G11 | G16 | S* | 总状态 |
|---|----------------------|----------|-------|-------|----|-----|-----|----|--------|
| 1 | `sortkit.py` | REQ-F03 / S04：快速排序实现本体（`_quick_sort_range` / `_partition`） | ✅ | ⚠️ | ✅ | ⚠️ | ✅ | N/A | ⚠️ 已审有问题 |
| 2 | `test_sortkit.py` | REQ-F05：新增 2 个快排回归用例 | ✅ | ✅ | ✅ | ⚠️ | ✅ | N/A | ⚠️ 已审有问题 |
| 3 | `.agents/changes/task-AUTO-…/impl.md` | 编码实现报告（与实现一致性核对） | ✅ | ⚠️ | N/A | N/A | N/A | N/A | ⚠️ 已审有问题 |
| 4 | `__pycache__/sortkit.cpython-312.pyc` | 二进制产物（本不应入库） | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过（二进制） |
| 5 | `__pycache__/test_sortkit.cpython-312.pyc` | 二进制产物（本不应入库） | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过 | 跳过（二进制） |

- **展开方式**：`git show --stat 57cc8d1`（无 glob）；`.java` 文件数 = **0**。
- **G/S 分列说明**：G1–G7/G9/G10/G12–G15/G17（事务/DB/MQ/缓存/调度/网络/资损/多租户/灰度/应急）与 S1–S10（SQL/XSS/SSRF/命令执行/XXE/反序列化/上传/鉴权/密钥/CSRF）在纯内存算法库中**全部无攻击面/无外部依赖**，逐 ID 标 `N/A(纯内存无 I/O，无对应场景)`，见 §4.3；Step1 只列出与本次变更实际相关的 G8/G11/G16。
- **收口**：队列 5 行全部非 `⬜`（3 行已审、2 行跳过）。

---

## Step 2 — 功能（产物 B）

> REQ 来源：`design.md` §1.2（核心功能）、§1.3（约束与非功能）、§1.5（F01–F05）、§4.3/§5.1.2（S01–S05）、§5.1.3.1（R01–R04、异常场景表）、§5.1.3.3（R07/R08）。未发明 spec 外功能点。

| REQ | Scenario | Spec 证据（原文/章节） | 关联文件 | 状态 | 代码证据 |
|-----|----------|------------------------|----------|------|----------|
| REQ-F03-a | `quick_sort(data)` 对整型/混合符号输入升序正确 | §1.2.3「提供快速排序（默认算法）」；§4.3 S04 `quick_sort(data)` | `sortkit.py` | ✅ | `sortkit.py:109-124` 入口；`test_sortkit.py:87-93`；本轮复验 400 组随机（n≤60，含重复）0 失败 |
| REQ-F03-b | 枢轴三数取中（首/中/尾）规避已序/逆序退化 | §5.1.3.3 R07「pivot 采用三数取中（首/中/尾）」；§1.3「通过随机化/三数取中优化」 | `sortkit.py` | ✅ | `sortkit.py:151-159`；`test_sortkit.py:99-101`；本轮 5 万已序/逆序输入验证 |
| REQ-F03-c | 子区长度 ≤ 阈值切插入排序 | §5.1.3.3 R08「子区长度 ≤ 阈值时切换插入排序」 | `sortkit.py` | ✅ | `sortkit.py:26` `INSERTION_THRESHOLD=10`、`sortkit.py:131-133`、`sortkit.py:177-185` |
| REQ-F03-d | 大量重复元素不退化（结果正确且不爆栈） | §5.1.3.3 异常场景「大量重复元素：三路分区可选优化（首版两路）」 | `sortkit.py` | ✅ | `sortkit.py:160-174` 三向分区；`test_sortkit.py:112-127`；本轮 `recursionlimit=200` 下 10 万重复值/双取值通过 |
| REQ-F01 | 统一入口默认 quick、`reverse=True` 降序、原地返回同引用 | §5.1.3.1 R03/R04、§5.1.2 S01 出参「排序后的同一序列」 | `sortkit.py` | ✅ | `sortkit.py:250-283`；`test_sortkit.py:157-179` |
| REQ-F05-a | 空/单元素直接返回 | §5.1.3.1 异常场景「空序列 [] / 单元素序列」 | `sortkit.py` | ✅ | `sortkit.py:121-122`、`:200-201`；`test_sortkit.py:95-97` |
| REQ-F05-b | None / 非 list → `InvalidInputError` | §5.1.3.1 R01 + 异常场景表 | `sortkit.py` | ✅ | `sortkit.py:59-64`、`:266-270`；`test_sortkit.py:190-201` |
| REQ-F05-c | 未知算法名 → `InvalidInputError` | §5.1.3.1 R02 + 异常场景「算法名拼写错误」 | `sortkit.py` | ✅ | `sortkit.py:267-270`；`test_sortkit.py:199-201` |
| REQ-F05-d | 元素不可比较 → 包装 `NotComparableError` | §5.1.3.1 异常场景「比较时抛出 TypeError，包装为 NotComparableError」 | `sortkit.py` | ⚠️ | 入口层 ✅（`sortkit.py:280-282`，实测通过）；**公共算法函数直调未包装**（`quick_sort([1,"a",…])` → `TypeError`）→ P1-2 |
| REQ-F04 | 归并排序稳定 | §5.1.3.4 R09「合并时取左区元素优先」 | `sortkit.py` | ✅ | `sortkit.py:216-238`；`test_sortkit.py:140-147` |
| REQ-NF1 | 复杂度在文档明示 | §1.3「时间复杂度需在文档中明示」 | `sortkit.py` | ✅ | `sortkit.py:111-113`（平均 O(n log n)、空间 O(log n) 栈、不稳定） |
| REQ-NF2 | 快速排序空间复杂度与 spec 一致 | §5.1.2 表 S04「O(log n) 栈」 vs §1.3「原地算法 O(1)（冒泡/插入/快速）」 | `sortkit.py` / `design.md` | ⚠️ | 实现取 O(log n) 栈（与 S04 表一致，`:127-141` 尾递归消除）；**spec 两处自相矛盾** → P2-5 |
| REQ-NF3 | 三向分区与 spec 描述对齐 | §5.1.3.3 异常场景「首版采用两路分区，三路分区可选优化」 | `sortkit.py` | ⚠️ | 实现已采用三向分区（`:160-174`，优于 spec 下限）；spec 未同步 → P2-5 |

---

## Step 3 — 可读性检查（产物 C）

> 无 Java：JVM 专有项 `N/A(非 Java)`；A 项以 Python/PEP 8 等价语义核销。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1.1 | 文件名 = 顶层类名 | N/A | 非 Java：`sortkit.py` 模块名与库名一致 |
| A1.2 | 编码 UTF-8 | ✅ | 两文件 UTF-8，中文 docstring 无乱码 |
| A1.3 | 无 Tab | ✅ | `awk '/\t/'` 0 命中 |
| A2.1 | 文件结构顺序 | ✅ | docstring → import → 常量 → 异常类 → 函数，段间空行 |
| A2.2 | 禁止 `import *` | ✅ | 显式 import（`sortkit.py:17-18`） |
| A2.3 | 静态/非静态 import 分组 | N/A | 非 Java 概念 |
| A2.4 | import 字典序 | ✅ | `functools` < `typing`；同组内 `List, Tuple, TypeVar` 有序 |
| A2.5 | 重载方法连续 | N/A | Python 无重载 |
| A3.1 | K&R 大括号 | N/A | Python 无大括号 |
| A3.2 | 空 catch 不可简写 | N/A | Python 无 catch 块语法 |
| A3.3 | 缩进 4 空格、禁 Tab | ✅ | 两文件均 4 空格 |
| A3.4 | 行宽 ≤ 120 | ⚠️ | `sortkit.py:8` 141 字符（`scan-all-rules.sh` 未覆盖 .py，人工补扫）→ P2-6 |
| A3.5 | 运算符换行规则 | N/A | 非 Java 风格项 |
| A3.6 | 成员之间空行 | ✅ | 顶层函数间 2 空行，方法间 1 空行 |
| A3.7 | 关键字与 `(` 空格 | ✅ | `if (`/`while (`/`for (` 一致 |
| A3.8 | 运算符两侧空格 | ✅ | 二元运算符两侧留空，`data[low]` 索引不留空 |
| A4.1 | 包名全小写 | N/A | 无包概念 |
| A4.2 | 类名 UpperCamelCase | ✅ | `SortError`/`InvalidInputError`/`NotComparableError`/`_Descending` |
| A4.3 | 方法名 lowerCamelCase | ✅ | Python 用 `snake_case`：`quick_sort`/`_partition`/`_insertion_sort` 一致 |
| A4.4 | 常量 UPPER_SNAKE_CASE | ✅ | `ALGORITHMS`、`INSERTION_THRESHOLD`、`_ALGORITHM_IMPL` |
| A4.5 | 变量/参数命名 | ✅ | `low`/`high`/`lt`/`gt`/`pivot` 语义明确，无前后缀 |
| A4.6 | 泛型命名 | ✅ | `T = TypeVar("T")` |
| A4.7 | 测试类命名 | ✅ | `TestQuickSort` 等，1 类对应 1 被测单元 |
| A5.1 | `@Override` | N/A | 非 Java 概念 |
| A5.2 | catch 块不可为空 | ✅ | `sortkit.py:280-282` 捕获后包装并 `raise ... from exc`，非空 |
| A5.3 | 静态方法用类名调用 | N/A | 非 Java 概念 |
| A5.4 | 禁重写 `finalize()` | ✅ | 未使用 |
| A6.1 | 数组方括号属类型 | N/A | 非 Java 概念 |
| A6.2 | switch fall-through | ✅ | Python 无 switch；`_partition` 的 `if/elif/else` 分支互斥无穿透 |
| A6.3 | 修饰符顺序 | N/A | 非 Java 概念 |
| A6.4 | 注解位置 | N/A | 非 Java 概念 |
| A6.5 | long 字面量大写 `L` | N/A | 非 Java 概念 |
| A7.1 | public 成员必须有文档 | ✅ | `python3 -c ast` 核验：两文件 **missing docstrings: []** |
| A7.2 | 块标记顺序 `@param→@return→@throws` | ⚠️ | `quick_sort`/`_quick_sort_range` 等公共函数 docstring 无 `Raises:` 段，异常契约未在函数侧声明 → 关联 P1-2 |
| A7.3 | 简单 getter 可省略 | ✅ | 无冗余 docstring |
| A7.4 | 多段落分隔 | ✅ | 模块 docstring 用空行分节 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.0 自动化预扫输出（`scan-all-rules.sh`，先于 LLM 执行）

命令（仓库根，目标为展开后的变更路径，非 glob）：

```bash
bash /root/.agentix/skills/managed/dtazziboot-java-code-review/references/script/scan-all-rules.sh \
  sortkit.py test_sortkit.py .agents/changes/task-AUTO-…/impl.md
```

完整输出（节选，6 条全部落在文档行宽）：

```text
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: sortkit.py test_sortkit.py .agents/changes/task-AUTO-…/impl.md
Engine:  ripgrep

[P2] A3.4 — LineWidthExceeded: …/impl.md:140
[P2] A3.4 — LineWidthExceeded: …/impl.md:161
[P2] A3.4 — LineWidthExceeded: …/impl.md:19
[P2] A3.4 — LineWidthExceeded: …/impl.md:194
[P2] A3.4 — LineWidthExceeded: …/impl.md:205
[P2] A3.4 — LineWidthExceeded: …/impl.md:9

=== Summary: 6 findings (P0=0, P1=0, P2=6) | 52/222 rules scanned ===
```

**脚本误报/漏报人工复核**：
- 误报：6 条均为 Markdown 文档行宽（含表格/路径），`impl.md` 为报告类文档，按 `A3.4` 的「Javadoc URL 例外」精神不构成代码风格问题 → 记 `N/A(文档行宽，非源码)`。
- 漏报（人工补）：`sortkit.py:8` 141 字符超 120（脚本未覆盖 `.py` 行宽规则）→ P2-6。
- 漏报（人工补）：脚本面向 Java API 语义，Python 侧的异常非原子（P1-1）、双注册表漂移（P2-1）等需 LLM 直读代码发现，见下表明细。

### 4.1 Bug 模式（`bug-pattern-checklist.md`，B/M/I 逐 ID）

> 等级映射 Blocker→P0 / Major→P1 / Info→P2。命中项均给出 `path:line`；未命中的 Java 专有规则标 `N/A` 并说明原因。

| ID | 状态 | 备注 |
|----|------|------|
| B001 | N/A | Java 日期/UUID 解析 API，Python 无对应；`_partition` 无 parse 调用 |
| B002 | N/A | Java 数组 `equals` 语义；Python `==` 为值比较 |
| B003 | N/A | `Arrays.fill` 无对应 |
| B004 | N/A | 数组 `toString` 无对应 |
| B005 | N/A | `Arrays.asList` 装箱无对应 |
| B006 | ✅ | 断言参数顺序：`test_sortkit.py` 全部 `assertEqual(expected, actual)` 顺序正确（如 `:52`、`:93`、`:110`） |
| B007 | ✅ | 未捕获 `BaseException`/`Exception` 宽泛类型于测试；`sortkit.py:280` 仅捕 `TypeError` |
| B008 | N/A | 无线程池 |
| B009 | N/A | 无位运算移位 |
| B010 | N/A | 无 BigDecimal 等价场景 |
| B011 | N/A | 无包装类型 `==` 比较（`_Descending.__eq__` 为显式值比较） |
| B012 | N/A | 无 Calendar |
| B013 | N/A | 无 Calendar |
| B014 | N/A | 无泛型集合查询 |
| B015 | N/A | 无 `toArray` |
| B016 | ⚠️→N/A | 可比较类型自洽：`_Descending.__lt__` 与 `__eq__` 均以 `value` 为比较域，方向一致（`:50-56`），无不对称；非 Java Comparable |
| B017 | ✅ | 无 `this == null` 等价判断 |
| B018 | N/A | 无三目数值提升 |
| B019 | N/A | 无 Money API |
| B020 | N/A | 无常量溢出（元素规模受内存限制） |
| B021 | N/A | 无 Jedis |
| B022 | N/A | 无 SimpleDateFormat（无时间处理） |
| B023 | ✅ | 无「创建异常未抛出」：`sortkit.py:268-270`、`:282` 均 `raise` |
| B024 | N/A | 无线程 |
| B025 | N/A | 无双括号初始化（Python 无匿名内部类） |
| B026 | N/A | 无 `x.equals(null)` 等价写法 |
| B027 | ✅ | `_Descending.__eq__` 仅比较 `value`，未误比字段（`:53-56`） |
| B028 | N/A | 无 DateUtil |
| B029 | N/A | 无 setter |
| B030 | N/A | 无浮点 `==` 比较；比较委托元素自身 `<` |
| B031 | N/A | 无 `String.format`；`sortkit.py:268-270` f-string 占位与参数一一对应 |
| B032 | N/A | 无注解 |
| B033 | N/A | 无 Unsafe |
| B034 | N/A | 无 Hashtable |
| B035 | ✅ | 无同一对象二元自运算（`lt`/`gt` 独立推进） |
| B036 | N/A | 无 IdentityHashMap |
| B037 | N/A | 无 varargs |
| B038 | ✅ | 递归必收敛：`_quick_sort_range` 每次循环收缩区间（`:129` while + `:136-141`），`_partition` 返回后相等区必非空 |
| B039 | N/A | 无 String.indexOf |
| B040 | N/A | 无 `Class.isInstance` |
| B041 | N/A | 无 JDBC |
| B042 | N/A | unittest 方法发现规则：`test_` 前缀 + 类继承 `TestCase`，26 个全部执行（实测 Ran 26 tests） |
| B043 | N/A | 无「内部类 @Test 不执行」等价场景 |
| B044 | N/A | 未混用 JUnit3/4 |
| B045 | N/A | 无 synchronized/锁 |
| B046 | ✅ | 循环条件必被更新：`_partition` `while i <= gt` 中 `i` 或 `gt` 每轮必变（`:164-173`）；`_quick_sort_range` `while low < high` 每轮收缩（`:136-141`）；`_insertion_sort` `while j >= low` 中 `j` 递减（`:182-184`） |
| B047 | N/A | 无 compare 类型转换 |
| B048 | N/A | 无 Math.round |
| B049 | N/A | 无日期格式 |
| B050 | N/A | 无日期格式 |
| B051 | N/A | 无系统属性读取 |
| B052 | N/A | 无日期格式 |
| B053 | N/A | 无 try-catch 式断言；用 `assertRaises` 上下文管理器（`:192-206`）已等价 |
| B054 | N/A | 无 EqualsTester |
| B055 | N/A | 无 Mockito |
| B056 | N/A | 无 `Arrays.asList` 定长列表 |
| B057 | ✅ | 无「遍历中修改集合」：`sortkit.py:276-277` 写回发生在包装列表枚举**之后**的独立循环，且 `wrapped` 与 `data` 为不同对象 |
| B058 | ✅ | 无集合自传入：`:274` 构造新列表，`_ALGORITHM_IMPL[algorithm](wrapped)` 参数与被调对象不同 |
| B059 | N/A | 无 `Collections.nCopies` |
| B060 | N/A | 无 null 拆箱 |
| B061 | N/A | 无 Base64 |
| B062 | N/A | 无 ClassLoader |
| B063 | N/A | 无 javax.xml |
| B064 | N/A | 无 Optional |
| B065 | N/A | 无 Pojo setter 自赋值 |
| B066 | N/A | 无 `(int)Math.random()`；测试用 `random.randint`（`test_sortkit.py:105`） |
| B067 | ✅ | 未用 `random() % n`；取模均落在非随机位置（`(low+high)//2`，`:151`） |
| B068 | N/A | 无字段自赋值 |
| B069 | N/A | 无 `compareTo` 自比较 |
| B070 | ✅ | 无自身比较：`test_algorithms_registry_matches_implementations`（`:208-212`）比较两个独立对象 `expected` 与 `ALGORITHMS` |
| B071 | ✅ | `high - low + 1 <= INSERTION_THRESHOLD`（`:131`）非恒真判断；无 `size() >= 0` 等价写法 |
| B072 | N/A | 无 Stream |
| B073 | N/A | 无 StringBuilder |
| B074 | N/A | 无 `substring(0)` |
| B075 | ✅ | for 循环方向与边界一致：`range(low+1, high+1)`（`:179`）、`range(low, high)`（`:80`、`:99`）、`range(len(left))`（`:222`） |
| B076 | N/A | 无 @Transactional |
| B077 | N/A | 无 CatchThrowable |
| B078 | ✅ | 无 `assertThat(x).isEqualTo(x)` 等价自比较 |
| B079 | N/A | 无 Mock |
| B080 | ✅ | 26 个用例均含断言（`assertEqual`/`assertIs`/`assertRaises`），无「只调不断言」 |
| B081 | ✅ | 无「新建集合排序后丢弃」；`sort()` 原地返回同一引用并被断言（`:165-171`） |
| M001 | ✅ | 无重复判断同一条件：`_partition` 三分支互斥（`:165-173`） |
| M002 | N/A | 无 instanceof |
| M003 | N/A | 无包装类构造器 |
| M004 | ✅ | 无 `printStackTrace` 等价（无 print 调试残留） |
| M005 | N/A | 无内部类 |
| M006 | N/A | 无编译期常量布尔表达式 |
| M007 | ✅ | 无空 except：`sortkit.py:280-282` 有包装与 `raise ... from exc` |
| M008 | N/A | 等价规则（重写 `__eq__` 需配套 `__hash__`）：`_Descending` 定义了 `__eq__` 但未定义 `__hash__`（Python 自动置 `None`，实例不可 hash）；该类仅作排序包装、不入 dict/set → 无命中风险，记 N/A(设计上不需要可哈希) |
| M009 | N/A | 无不可能相等的类型间 equals |
| M010 | N/A | 无位运算 |
| M011 | N/A | 无 switch |
| M012 | ✅ | 无 finally 中 return/throw（无 finally 使用） |
| M013 | N/A | 无浮点强转 |
| M014 | N/A | 无枚举 |
| M015 | N/A | 无字段隐藏 |
| M016 | N/A | 无时区相关调用 |
| M017 | N/A | 测试方法发现：`unittest` 按 `test_` 前缀收集，实测 26 个全部执行 |
| M018 | N/A | 无显式锁 |
| M019 | N/A | 无枚举 switch；`sort()` 对 `ALGORITHMS` 全量校验后又以 `_ALGORITHM_IMPL` 分发，覆盖缺口见 P2-1 |
| M020 | N/A | 非 Java 方法重写；`_Descending.__lt__/__eq__` 为 dunder 重写，语义正确 |
| M021 | N/A | 无 `equals(SpecificType)` |
| M022 | N/A | 无 Optional |
| M023 | N/A | 无未重写 `toString` 的打印 |
| M024 | N/A | 无 Optional |
| M025 | N/A | 无 final class |
| M026 | N/A | 无 @Mock static |
| M027 | N/A | 无 ThreadLocal |
| I001 | ✅ | 异常断言已校验类型（`assertRaises(InvalidInputError/NotComparableError)`，`:190-206`）；未断言 message 文本 → 覆盖度缺口见 P2-8 |
| I002 | N/A | 无 @DoNotMock |
| I003 | N/A | 无 @AutoValue |
| I004 | N/A | 无 java.util.Date |
| I005 | N/A | 无 JUnit3 |
| I006 | N/A | 无 setUp，用例自带 Arrange |
| I007 | N/A | 无 tearDown |
| I008 | N/A | 无 dataProvider |
| I009 | ✅ | 用例计数可统计：实测 `Ran 26 tests`（`impl.md:108` 记 24，不同步 → P2-5） |
| I010 | N/A | 无 Spring 容器测试 |

### 4.2 可靠性（`reliability-checklist.md`，G 逐 ID）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | N/A | 纯内存计算，无事务/并发写（`design.md:299`「无共享可变状态」） |
| G1.2 | N/A | 无加锁更新 |
| G1.3 | N/A | 无乐观锁 |
| G1.4 | N/A | 无多资源加锁 |
| G2.1 | N/A | 无写接口/消息消费 |
| G2.2 | N/A | 无重试/定时任务/MQ |
| G2.3 | N/A | 无幂等键约定 |
| G3.1 | N/A | 无分布式事务 |
| G3.2 | N/A | 无 @Transactional |
| G4.1 | N/A | 无 SQL |
| G4.2 | N/A | 无 SQL/索引 |
| G4.3 | N/A | 无分页查询 |
| G4.4 | N/A | 无 SQL |
| G5.1 | N/A | 无 MQ |
| G6.1 | N/A | 无缓存 |
| G6.2 | N/A | 无缓存双写 |
| G7.1 | N/A | 无调度任务 |
| G7.2 | N/A | 无调度开关 |
| G8.1 | ✅ | 非 happy-path 单一路径：异常路径显式包装并链式抛出（`sortkit.py:280-282`），非吞异常 |
| G8.2 | N/A | 无外部依赖/超时 |
| G8.3 | N/A | 无 I/O 流、连接、锁资源 |
| G8.4 | N/A | 无线程池 |
| G8.5 | N/A | 无 ThreadLocal |
| G8.6 | N/A | 无线程池 |
| G8.7 | ✅ | 防御性编程：入口 `_validate_sequence` 校验 None/非 list（`:59-64`、`:266`），算法名白名单校验（`:267-270`） |
| G9.1 | N/A | 无外部调用 |
| G9.2 | N/A | 无外部调用超时 |
| G9.3 | N/A | 无重试 |
| G10.1 | N/A | 无「null 兼表无数据与异常」契约；None 入参统一抛 `InvalidInputError`（`:61-62`） |
| G10.2 | N/A | 无跨服务契约 |
| G11.1 | ✅ | 新增逻辑有单测且含断言（`test_sortkit.py:112-127`，共 26 用例） |
| G11.2 | ⚠️ | 边界覆盖缺口：递归栈深受控未固化为回归用例（`impl.md:194` 仅声称冒烟验证）；`reverse=True` × 重复值 × quick 组合缺失；异常后入参状态无断言 → P2-8 |
| G11.3 | ✅ | 入参空/非 list 有防御校验（`:59-64`）；空列表返回空（`:121-122`） |
| G11.4 | N/A | 无金额/数值运算（仅索引算术，`(low+high)//2` 在 list 长度内无溢出风险） |
| G12.1 | N/A | 无资金场景 |
| G12.2 | N/A | 无线上流量，无需止血开关 |
| G13.1 | N/A | 无日志 |
| G14.1 | N/A | 无金额 |
| G14.2 | N/A | 无多租户 |
| G14.3 | N/A | 无时区存储 |
| G14.4 | N/A | 无时间格式化 |
| G15.1 | N/A | 无表结构变更 |
| G15.2 | N/A | 既有 `bubble_sort.py` 未改动，旧调用方兼容（`impl.md:131` 已核） |
| G15.3 | N/A | 无灰度开关需求（库级纯函数） |
| G16.1 | N/A | 无服务链路，无需指标埋点 |
| G16.2 | ✅ | 异常可追溯：`raise NotComparableError(...) from exc` 保留原始 `TypeError` 上下文（`:282`）；无日志体系（库不改调用方日志） |
| G16.3 | N/A | 无日志 |
| G16.4 | ✅ | 无空 catch、无 `printStackTrace` 等价、无「捕获后继续执行」（`:280-282` 为 re-raise） |
| G17.1 | N/A | 无功能开关 |
| G17.2 | N/A | 无降级通道 |
| G17.3 | N/A | 无数据变更 |
| G18.1 | N/A | 安全补强：无认证面 |
| G18.2 | N/A | 无越权面 |
| G18.3 | N/A | 无敏感数据流转 |

### 4.3 安全（`security-checklist.md`，S 逐 ID）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | N/A | 无 SQL（纯内存） |
| S1.2 | N/A | 无动态表名/排序字段 SQL |
| S1.3 | N/A | 无 like/in 查询 |
| S2.1 | N/A | 无 HTML/JS 输出 |
| S2.2 | N/A | 无富文本 |
| S2.3 | N/A | 无模板引擎 |
| S3.1 | N/A | 无外部 URL 请求 |
| S3.2 | N/A | 无跳转 |
| S3.3 | N/A | 无网络调用 |
| S4.1 | N/A | 无系统命令执行（全库无 `os`/`subprocess` 导入） |
| S4.2 | N/A | 无文件/图片操作 |
| S5.1 | N/A | 无 XML 解析 |
| S5.2 | N/A | 无 XPath |
| S6.1 | N/A | 无反序列化 |
| S6.2 | N/A | 无 JSON 反序列化 |
| S6.3 | N/A | 无敏感字段 |
| S7.1 | N/A | 无文件上传 |
| S7.2 | N/A | 无路径拼接 |
| S7.3 | N/A | 无文件存储 |
| S8.1 | N/A | 无接口鉴权面 |
| S8.2 | N/A | 无 HTTP 方法 |
| S8.3 | N/A | 无数据 ID |
| S8.4 | N/A | 无 Cookie |
| S9.1 | N/A | 无密钥硬编码（`cred-helper-test.txt` 为环境遗留文本、无凭证，见 P2-9） |
| S9.2 | N/A | 无日志 |
| S9.3 | N/A | 无传输/存储 |
| S9.4 | N/A | 无随机数安全场景（测试随机仅造数据） |
| S10.1 | N/A | 无写操作接口 |
| S10.2 | N/A | 无 CORS |
| S10.3 | N/A | 无 URL 跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

### 5.1 自定义扩展（`customized-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | N/A | `customized-checklist.md` 仅含示例项（Controller `@Valid`），本项目无自定义规则 → `N/A(未启用自定义规则)` |
| U2.1 | N/A | `N/A(未启用自定义规则)` |
| U2.2 | N/A | `N/A(未启用自定义规则)` |
| U2.3 | N/A | `N/A(未启用自定义规则)` |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3`、`G8/G11/G16`、`S*` 各列均非 `⬜`（跳过文件除外）
- [x] Step 2 的每个 REQ/Scenario 均非 `⬜`
- [x] Step 3 的 A1.1–A7.4 均非 `⬜`
- [x] Step 4 全部 G（G1.1–G18.3）与 S（S1.1–S10.3）及 B001–B081 / M001–M027 / I001–I010 均非 `⬜`（`N/A` 均写原因）
- [x] Step 5 全部 U* 均非 `⬜`
- [x] 所有 `❌/⚠️` 已写入 report，且包含 `ID + path:line`
