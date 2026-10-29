# Code Review Report

> **Change** `排序算法 sortkit · 快速排序健壮性强化（round 3 评审）` · **分支/Commit** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-78d079ee-dba0-4947-b3ee-6848c2a12c5d` / `57cc8d1` · **日期** `2026-10-29` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。本次已**先**运行 `scan-all-rules.sh`（§5 起首段）**再**写 LLM 结论。
>
> **技能守卫与适配声明**：本次变更**不含任何 `.java` 文件**（`sortkit.py`、`test_sortkit.py` 为 Python 3；仓库无 Java 工程，`mvn`/`java` 不可用）。按 `dtazziboot-java-code-review` 的 Java 守卫，Java 专属规则（A1–A7 中 JVM 专有项、B/M/I 阿里 Java 规约、S1–S10 Web/Java 攻击面）一概标 **N/A(非 Java)**；为不中断流水线交付，同一 SDD 清单框架改以 **Python 语义等价**完成功能（REQ）、可读性、可靠性、安全四段核销，并逐条给出 `path:line` 证据。清单见 `.agents/cr/2026-10-29-quicksort-cr-checklist.md`。
>
> **本报告取代** `2026-09-22` round-1 评审（结论「0 问题 / 通过」），round-1 内容保留于 git 历史（`dd557ef` 之后评审提交）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | **0** |
| 变更文件（源码） | `sortkit.py`（283 行，`+59/-23`）、`test_sortkit.py`（216 行，`+17`） |
| 变更文件（文档/产物） | `impl.md`（`+76`）、`__pycache__/sortkit.cpython-312.pyc`、`__pycache__/test_sortkit.cpython-312.pyc` |
| 变更行数 | `+129 / -23`（`git show --stat 57cc8d1`） |

| 类/模块 | 路径 | 角色 |
|---------|------|------|
| 快速排序主体 | `sortkit.py:109-185` | `quick_sort` / `_quick_sort_range` / `_partition` / `_insertion_sort`（本次改动核心） |
| 统一入口与异常 | `sortkit.py:250-283` | `sort()`：校验、降序包装、异常包装 |
| 快排回归用例 | `test_sortkit.py:112-127` | 大量重复值不爆栈、混合重复值正确性 |

**验证证据（本轮实测，非引用 impl.md 自述）**

| 验证项 | 命令/方法 | 结果 |
|--------|-----------|------|
| 单元测试 | `python3 -m unittest test_sortkit` | `Ran 26 tests … OK`（exit 0） |
| 随机对拍 | 400 组（n≤60，取值池 3/10/1000）+ 4 算法 × 80 组 `reverse=True` | 失败 0 |
| 栈深受控 | `sys.setrecursionlimit(200)` 下 10 万元素（4 值重复）、3 万 organ-pipe、5 万已序、5 万逆序、median-of-3 killer 4000 | 全部通过 |
| 契约探测 | `ALGORITHMS.append("bogus")` → `sort(…, "bogus")` | 抛 `KeyError`（非 `InvalidInputError`）→ P2-1 |
| 失败原子性 | `sort([18,17,16,15,14,"a",3,…,4])` | 抛 `NotComparableError` 后 `data` 已被改写为 `[0,1,2,3,4,'a',14,…,17]` → P1-1 |
| 直调公共函数 | `quick_sort([1,"a",2,…])` | 抛原始 `TypeError`（未包装）→ P1-2 |
| 文档/风格 | `awk` 行宽 + `ast.get_docstring` | `sortkit.py:8` 141 字符；两文件 docstring 覆盖率 100% |

---

## 2. 问题计数

| P0（阻塞） | P1（推荐） | P2（参考） |
|----|----|-----|
| **0** | **2** | **9** |

> **blocker_count = 0**（`run_context.json` 保持 `{"blocker_count": 0}`）。核心算法正确性无 P0：三向分区 + 三数取中 + 只递归较小侧的实现经 400 组随机对拍与大规摸退化输入实测全部正确。

---

## 3. Step 2 — 功能（REQ）

> REQ 来源：`design.md` §1.2 / §1.3 / §1.5（F01–F05）/ §4.3+§5.1.2（S01–S05）/ §5.1.3.1（R01–R04、异常场景表）/ §5.1.3.3（R07、R08）。未发明 spec 外功能点。

### REQ-F03: 快速排序（默认算法）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 升序结果正确（含负数/重复值） | ✅ | §1.2.3「提供快速排序（默认算法）」 | `sortkit.py:109-124`；`test_sortkit.py:87-93` | 400 组随机对拍 0 失败 |
| 三数取中选枢轴，规避已序/近序退化 | ✅ | §5.1.3.3 R07「pivot 采用三数取中（首/中/尾）」 | `sortkit.py:151-159` | 5 万已序 / 5 万逆序输入实测正确 |
| 小规模切插入排序 | ✅ | §5.1.3.3 R08「子区长度 ≤ 阈值时切换插入排序」 | `sortkit.py:26`、`:131-133`、`:177-185` | `INSERTION_THRESHOLD = 10` |
| 大量重复元素仍正确且不爆栈 | ✅ | §5.1.3.3 异常场景「大量重复元素…结果仍正确」 | `sortkit.py:160-174`（三向分区）+ `:136-141`（只递归较小侧） | `recursionlimit=200` 下 10 万重复值通过；实现已超出 spec 的「两路分区」下限 |

### REQ-F01 / S01: 统一入口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 默认 `algorithm='quick'`、`reverse=False` | ✅ | §5.1.3.1 R03；§5.1.2 S01 签名 | `sortkit.py:250-283`；`test_sortkit.py:157-163` | 签名与 S01 完全一致 |
| `reverse=True` 降序 | ✅ | §5.1.3.1 R04「以取反比较实现」 | `sortkit.py:272-277`（`_Descending` 包装） | 80 组随机 × 4 算法降序对拍 0 失败 |
| 原地排序并返回同一引用 | ✅ | §5.1.2 S01 出参「排序后的同一序列」 | `sortkit.py:279-283`；`test_sortkit.py:165-171` | `sort(d) is d` 成立 |

### REQ-F05: 边界与异常处理

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 空序列 / 单元素 | ✅ | §5.1.3.1 异常场景表 | `sortkit.py:121-122`、`:200-201`；`test_sortkit.py:95-97` | 直接返回，不进入循环 |
| `data is None` / 非 list | ✅ | §5.1.3.1 R01 + 异常场景「data 为 None」 | `sortkit.py:59-64`、`:266`；`test_sortkit.py:190-197` | 抛 `InvalidInputError`，提示文案与 spec 一致 |
| 未知算法名 | ✅ | §5.1.3.1 R02 | `sortkit.py:267-270`；`test_sortkit.py:199-201` | 抛 `InvalidInputError` 并列出合法取值 |
| 元素不可比较 → `NotComparableError` | ⚠️ | §5.1.3.1 异常场景「比较时抛出 TypeError，包装为 NotComparableError」 | 入口层 ✅ `sortkit.py:280-282`；直调 `quick_sort` ❌ `sortkit.py:121-124` | 仅 `sort()` 包装；`quick_sort([1,"a",…])` 抛原始 `TypeError` → **P1-2** |

### REQ-F04 / REQ-NF: 其他功能与非功能约束

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 归并排序稳定 | ✅ | §5.1.3.4 R09「合并时取左区元素优先」 | `sortkit.py:216-238`；`test_sortkit.py:140-147` | `right[j] < left[i]` 才取右区 |
| 复杂度在文档明示 | ✅ | §1.3「时间复杂度需在文档中明示」 | `sortkit.py:111-113` | 平均 O(n log n) / 空间 O(log n) 栈 / 不稳定，均与实现一致 |
| 快速排序空间复杂度与 spec 一致 | ⚠️ | §5.1.2 表 S04「O(log n) 栈」 vs §1.3「原地算法 O(1)（冒泡/插入/快速）」 | 实现 `sortkit.py:127-141` 取 O(log n) 栈，与 S04 一致 | **spec 自身矛盾**，实现选择更严谨的一侧 → P2-5 |
| 分区方案与 spec 描述对齐 | ⚠️ | §5.1.3.3 异常场景「首版采用两路分区，三路分区可选优化」 | 实现已用三向分区 `sortkit.py:160-174` | 实现优于 spec 下限，但 spec 未同步 → P2-5 |

---

## 4. Step 3 — 可读性检查

> 无 Java：JVM 专有项（A1.1、A2.3、A2.5、A3.1、A3.2、A3.5、A4.1、A5.1、A5.3、A6.1、A6.3–A6.5）**N/A(非 Java)**；A 项以 PEP 8 等价语义核销，明细见 checklist Step 3。

| 结果 | 说明 |
|------|------|
| ⚠️ | 命名/结构/缩进/空行/docstring 覆盖率均符合（`ast` 核验 missing docstrings = 0；无 Tab；`functools`<`typing` 有序）。两处偏差：**A3.4** `sortkit.py:8` 行宽 141 > 120（**P2-6**）；**A7.2** 公共函数 docstring 无 `Raises:` 段，异常契约仅写在 `sort()` 上（**P1-2** 附带项） |

---

## 5. Step 4 — 可靠性检查

**自动化预扫（先执行）**：`scan-all-rules.sh sortkit.py test_sortkit.py …/impl.md` → `Summary: 6 findings (P0=0, P1=0, P2=6) | 52/222 rules scanned`，6 条全为 `impl.md` 的 `A3.4` 文档行宽（报告类文档，非源码，判 `N/A(文档行宽)`）；人工复核补出脚本未覆盖项：`sortkit.py:8` 行宽（P2-6）、失败非原子（P1-1）、双注册表漂移（P2-1）。

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 可靠性 | `reliability-checklist.md` G1–G18 | ⚠️ | P2 | G11.2 边界覆盖缺口（**P2-8**）；G8.1/G8.7/G11.1/G11.3/G16.2/G16.4 均 ✅；G1–G7、G9、G10、G12–G15、G17、G18 无对应场景（纯内存、无 I/O/事务/MQ/缓存/多租户），逐 ID `N/A` |
| 安全 | `security-checklist.md` S1–S10 | N/A | — | 无 SQL/XSS/SSRF/命令执行/XXE/反序列化/上传/鉴权/密钥/CSRF 攻击面；全库无 `os`/`subprocess`/网络/文件 I/O 导入；`cred-helper-test.txt` 为无凭证的环境遗留文本（P2-9） |
| Bug 模式 | `bug-pattern-checklist.md` B001–B081 / M001–M027 / I001–I010 | ✅ | P2 | 118 条逐 ID 核销：可迁移项全部 ✅（B006 断言顺序、B023 异常必抛、B038 递归收敛、B046 循环变量必更新、B057/B058 无集合自操作、B070/B078 无自比较、B075 循环方向、B080 用例均含断言、B081 无结果丢弃、M001 分支互斥、M004/M007/M012 无吞异常、I001 异常类型断言）；其余 Java 专有项 `N/A(非 Java)`。**Python 侧缺陷由人工直读补扫**，见 P1-1/P1-2/P2-1/P2-2 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | N/A | — | 清单内仅示例项（Controller `@Valid`），本项目未配置自定义规则 → `N/A(未启用自定义规则)` |

---

## 7. 结论

- **合并建议**：**修复后合并**（无 P0 阻塞；建议先消除 P1-1/P1-2 及 P2-4 的入库噪音）
- **P0**：无。核心算法正确性经独立对拍与大规摸退化输入实测确认。
- **P1**：
  1. **P1-1** 排序失败（元素不可比较）后入参已被部分重排，非原子，未文档化 —— `sortkit.py:271-282` + `:160-174`，实测复现。
  2. **P1-2** `quick_sort`/`bubble_sort*`/`merge_sort` 直调时不包装 `TypeError`，与 `design.md:296`、`impl.md:181`「不可比较元素统一包装 `NotComparableError`」不一致，异常契约仅在 `sort()` 成立 —— `sortkit.py:109-124`、`:67-83`、`:86-106`、`:188-203`。
- **P2**：
  1. **P2-1** `ALGORITHMS`（可变公开 list）与 `_ALGORITHM_IMPL`（dict）双源，漂移时校验通过但分发抛 `KeyError` —— `sortkit.py:23`、`:242-247`、`:267-270`，实测 `KeyError: 'bogus'`。
  2. **P2-2** `try` 覆盖整个分发过程，任何 `TypeError`（含实现内部缺陷）都被误标为「元素间不可比较」—— `sortkit.py:271-282`。
  3. **P2-3** 最坏 O(n²) 无兜底（无深度上限回退堆排序），median-of-3 仍可被构造对抗序列打满 —— `sortkit.py:127-141`（spec §1.3 允许最坏 O(n²)，故仅 P2）。
  4. **P2-4** `__pycache__/*.pyc` 二进制入库且仓库无 `.gitignore`：每次跑测即产生工作区脏 diff，跨 Python 版本易冲突 —— `__pycache__/sortkit.cpython-312.pyc`、`__pycache__/test_sortkit.cpython-312.pyc`。
  5. **P2-5** 文档与实现/测试不同步：`impl.md:84` 仍写「Lomuto 分区」、`impl.md:62`/`:108` 仍写「24 tests」（实测 26）、`design.md:35` 与 `design.md:253` 空间复杂度互斥、`design.md:369` 仍称「首版采用两路分区」。
  6. **P2-6** `sortkit.py:8` 行宽 141 > 120（`scan-all-rules.sh` 未覆盖 `.py`，人工补扫）。
  7. **P2-7** 类型注解待现代化：`typing.List/Tuple`（`sortkit.py:18`）而非 `list[T]`/`tuple[int,int]`；`_validate_sequence(data)` 无注解（`sortkit.py:59`）。
  8. **P2-8** 回归覆盖缺口：递归栈深受控未固化为单测（`impl.md:194` 仅声称冒烟）、`reverse=True` × 重复值 × quick 组合缺失、异常后入参状态无断言 —— `test_sortkit.py:84-127`。
  9. **P2-9** 仓库根 `cred-helper-test.txt`（环境自检遗留，含 pod 名与时间戳）被跟踪，非本次变更，建议清理 —— `cred-helper-test.txt:1`。
- **一句话**：三向分区 + 只递归较小侧的快速排序实现正确、退化防护实测有效，可合并；主要遗留是「异常契约只覆盖统一入口」「失败后入参非原子」两项调用方可感知的行为缺口，叠加 `.pyc` 入库与文档不同步等工程噪音。

---

## 7.1 问题片段（必填）

> 本次问题均位于非 Java 文件，片段以 Python 给出并带行号标注（`path:start-end`）。

- **P1-1** `sortkit.py:271-282` — 异常路径非原子：比较在分区过程中进行，`TypeError` 抛出时 `data` 已被前面的交换改写，调用方捕获后继续使用将拿到「半排序」列表。实测 `sort([18,17,16,15,14,"a",3,…,4])` → `NotComparableError` 后列表变为 `[0,1,2,3,4,'a',14,15,16,19,12,…,17]`。
  片段范围：`sortkit.py:271-282`

```python
L271|    try:
L272|        if reverse:
L273|            # 逆序比较包装：包装每个元素使比较取反，排序后解包写回原序列
L274|            wrapped = [_Descending(v) for v in data]
L275|            _ALGORITHM_IMPL[algorithm](wrapped)
L276|            for i, item in enumerate(wrapped):
L277|                data[i] = item.value
L278|        else:
L279|            _ALGORITHM_IMPL[algorithm](data)   # 问题：原地分区中途抛错，data 已被部分重排
L280|    except TypeError as exc:
L281|        # 元素不可比较（如 int 与 str 混排），包装为 NotComparableError
L282|        raise NotComparableError("元素间不可比较") from exc
```

- **P1-2** `sortkit.py:109-124` — 公共算法函数不包装比较异常：`design.md:296`／`impl.md:181` 声明「比较时抛出 TypeError，包装为 NotComparableError」，但包装仅存在于 `sort()`；直调 `quick_sort` 得到原始 `TypeError`，且 docstring 未声明 `Raises`（A7.2）。
  片段范围：`sortkit.py:109-124`

```python
L109|def quick_sort(data: List[T]) -> List[T]:
L110|    """
L111|    快速排序（默认算法）：三数取中选枢轴 + 三向分区，小规模切插入排序。
L112|
L113|    平均 O(n log n)，空间 O(log n) 栈，不稳定。
L114|
L115|    Args:
L116|        data: 待排序列表
L117|
L118|    Returns:
L119|        原地排序后的同一列表引用
L120|    """
L121|    if len(data) < 2:
L122|        return data
L123|    _quick_sort_range(data, 0, len(data) - 1)   # 问题：TypeError 不在此包装，也不声明 Raises
L124|    return data
```

- **P2-1** `sortkit.py:23` / `:267-270` — 双注册表漂移：校验用 `ALGORITHMS`（可变 list），分发用 `_ALGORITHM_IMPL`（dict）。实测 `ALGORITHMS.append("bogus")` 后 `sort([2,1], algorithm="bogus")` 抛 `KeyError: 'bogus'`，而非 spec 要求的 `InvalidInputError`。
  片段范围：`sortkit.py:23`、`:265-270`

```python
L23 |ALGORITHMS = ["bubble", "bubble_optimized", "quick", "merge"]
...
L266|    _validate_sequence(data)
L267|    if algorithm not in ALGORITHMS:
L268|        raise InvalidInputError(
L269|            f"未知算法名：{algorithm}，合法取值：{ALGORITHMS}"
L270|        )
```

- **P2-2** `sortkit.py:280` — 捕获范围过宽：`except TypeError` 覆盖整个分发过程，实现内部任何 `TypeError`（如参数传错）都会被改写为「元素间不可比较」，误导排障。
  片段范围：`sortkit.py:279-282`（同 P1-1 片段）：仅比较调用应被保护，dispatch 结构本身不应被纳入。

- **P2-3** `sortkit.py:129-141` — 无最坏情况兜底：仅靠「只递归较小侧」把栈深压到 O(log n)，时间仍可能退化到 O(n²)（median-of-3 killer）。
  片段范围：`sortkit.py:127-141`

```python
L129|    while low < high:
L130|        # 小规模子区切换插入排序，降低递归开销
L131|        if high - low + 1 <= INSERTION_THRESHOLD:
L132|            _insertion_sort(data, low, high)
L133|            return
L134|        lt, gt = _partition(data, low, high)
L135|        # 相等区 [lt, gt] 已就位，仅需处理两侧；递归较小一侧，循环处理较大一侧
L136|        if lt - low < high - gt:
L137|            _quick_sort_range(data, low, lt - 1)
L138|            low = gt + 1
L139|        else:
L140|            _quick_sort_range(data, gt + 1, high)
L141|            high = lt - 1
```

- **P2-4** `__pycache__/sortkit.cpython-312.pyc`、`__pycache__/test_sortkit.cpython-312.pyc` — 字节码（CPython 3.12）随 `57cc8d1` 入库，仓库无 `.gitignore`；建议加 `.gitignore` 并 `git rm --cached`（本次评审只读，未执行）。
- **P2-5** 文档同步（`impl.md:62`、`:84`、`:108`；`design.md:35`、`:253`、`:369`）：

```text
L84 |  - 快速排序：三数取中（首/中/尾）选枢轴 + Lomuto 分区，子区长度 ≤ 10 切插入排序降低递归开销   ← 现实现为三向分区
L108|    | 单测验证 | ✅ | `python3 -m unittest test_sortkit`，24 tests，OK |                      ← 实测 26 tests
L35 |  - 空间复杂度：原地算法 O(1)（冒泡/插入/快速），归并排序 O(n) 辅助空间。                  ← 与 L253「O(log n) 栈」冲突
L369|    | 大量重复元素 | 三路分区可选优化（注：首版采用两路分区，重复元素场景结果仍正确） |      ← 现已落地三向分区
```

- **P2-6** `sortkit.py:8` — 行宽 141 字符（>120），模块 docstring 的算法清单超长：

```python
L7 | - 统一入口：sort(data, algorithm='quick', reverse=False)
L8 | - 算法实现：冒泡（标准/优化）、快速（默认，三数取中 + 三向分区 + 小规模切插入排序）、归并（稳定）
L9 | - 异常体系：SortError -> InvalidInputError / NotComparableError
```

- **P2-7** `sortkit.py:18`、`:59`、`:144` — 注解待现代化：

```python
L18 | from typing import List, Tuple, TypeVar      # 建议 list[T] / tuple[int, int]（Python 3.9+）
L59 | def _validate_sequence(data) -> None:        # 缺入参注解
L144| def _partition(data: List[T], low: int, high: int) -> Tuple[int, int]:
```

- **P2-8** `test_sortkit.py:84-127` — 快排用例集缺口：无「压小 recursionlimit 后仍可排序」的固化断言（`impl.md:194` 仅在报告里声称冒烟做过），无 `reverse=True` × 重复值组合，无 P1-1 的「异常后入参状态」断言。
- **P2-9** `cred-helper-test.txt:1` — `cred-helper write test at 2026-07-16T02:29:04Z from pod agent-807a`（环境自检遗留，已跟踪；无凭证内容，建议清理）。

---

## 8. 修复任务列表

### P0

- 无。

### P1

- [ ] **P1** `sortkit.py:271-282` — 将「元素不可比较」检测前移或改为先校验可比较性（或用 `_Descending` 同款包装路径做副本排序后回写），保证抛 `NotComparableError` 时入参不被部分重排；若保留现语义，则在 `sort()` docstring 显式声明「异常时入参可能处于部分排序状态」。
- [ ] **P1** `sortkit.py:109`/`:67`/`:86`/`:188` — 让四个公共算法函数在比较抛 `TypeError` 时同样包装为 `NotComparableError`（抽出统一装饰器或内部 `_sort_guard`），并在各 docstring 补 `Raises:` 段，使异常契约与 `design.md:296`/`impl.md:181` 一致。

### P2（可选）

- [ ] **P2** `sortkit.py:23`/`:267-270` — 以 `_ALGORITHM_IMPL` 作为唯一权威注册表（`ALGORITHMS = tuple(_ALGORITHM_IMPL)` 且校验改为查 dict 键），消除漂移导致的 `KeyError`。
- [ ] **P2** `sortkit.py:280` — 收窄 `except TypeError` 的作用范围，仅包住比较/分区调用，避免把实现内部 `TypeError` 误标为数据不可比较。
- [ ] **P2** `sortkit.py:129-141` — 增加深度上限（如 `2*log2(n)`）超限即切堆排序的兜底，消除 `median-of-3` killer 的 O(n²) 时间退化（可选）。
- [ ] **P2** `__pycache__/sortkit.cpython-312.pyc`/`test_sortkit.cpython-312.pyc` — 新增 `.gitignore`（`__pycache__/`、`*.pyc`）并从索引移除字节码。
- [ ] **P2** `impl.md:84`/`:62`/`:108`、`design.md:35`/`:369` — 同步文档：改「Lomuto」为「三向分区」、测试数 24→26、统一空间复杂度表述为 O(log n) 栈、把「首版两路分区」更新为已落地三向分区。
- [ ] **P2** `sortkit.py:8` — 拆分模块 docstring 算法清单为多行，使行宽 ≤ 120。
- [ ] **P2** `sortkit.py:18`/`:59`/`:144` — 注解现代化为 `list[T]`/`tuple[int, int]` 并补齐 `_validate_sequence` 入参类型。
- [ ] **P2** `test_sortkit.py:112-127` — 新增「`sys.setrecursionlimit` 压小后仍可排序大数组」「`sort(reverse=True)` × 大量重复值 × quick」「异常后入参状态」三条回归用例。
- [ ] **P2** `cred-helper-test.txt:1` — 删除该环境遗留文件（非本次变更，建议随手清理）。
