# Code Review Checklist

> **Change** `hello-java` · **分支/Commit** `AI/task-DEV-9d10e310-7901-11f1-8a9f-59ecae612580-681282d1-a747-43ec-a861-d38148f32f04` / `a153d53` · **日期** `2026-10-16`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。
>
> **执行顺序（强制）**：写入本清单并进入逐文件审查前，先在目标仓库对变更路径运行 `references/script/scan-all-rules.sh`，将输出贴入 Step 3 和 Step 4 备注；再用 LLM 完成 Step 2–5 中脚本未覆盖项及复核。

**预扫结果**：

```text
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: Hello.java
Engine:  ripgrep

=== No findings. 52/222 rules scanned ===
EXIT_CODE=0
```

---

## Step 1 — 执行队列（产物 A）

> **Step4 列语义**：每个 **Sn / Gn** 表示「**本文件**在 Step4 审查中，对 `reliability-checklist.md` 第 **G*n*** 节、`security-checklist.md` 第 **S*n*** 节的扫描结论」。**Bug 模式（B/M/I）** 在下方 **§4.1** 按清单 ID 核销。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|-----|----|----|----|----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|-----|-----|--------|
| 1 | `Hello.java` | REQ-1 | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ 已审有问题 |

- 由 `git diff --name-only HEAD~1 HEAD` 展开：`Hello.java`
- **守卫**：包含 1 个 `.java` 文件，继续审查。
- **收口**：G11 列标 `⚠️`（P2 无单测），其余 G/S 列均 `N/A`（无对应业务场景）。

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | Given 用户需要一个 Hello World 程序; When 执行 `java Hello`; Then 输出 `Hello, World!` | 需求描述原文："写一个hello，java实现" | `Hello.java` | ✅ | `Hello.java:15` — `System.out.println("Hello, World!");` |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | A1.1 文件名 Hello.java = 类名 Hello；A1.2 UTF-8；A1.3 仅 ASCII 空格与换行，无 Tab |
| A2 | 源文件结构/import 顺序 | ⚠️ | A2.1 `Hello.java:7` — 缺少 `package` 声明（使用默认包），生产代码建议添加包名。无 import 语句，A2.2–A2.5 N/A |
| A3 | 代码样式 | ✅ | A3.1 K&R 大括号正确；A3.3 缩进 4 空格；A3.4 行宽 ≤ 120；A3.6–A3.8 合规 |
| A4 | 命名规范 | ✅ | A4.2 类名 `Hello` UpperCamelCase；A4.3 方法名 `main` lowerCamelCase |
| A5 | 编码实践 | ✅ | 无重写、无 catch、无 finalize |
| A6 | 特定元素样式 | ✅ | A6.1 `String[] args` 正确；A6.3 修饰符顺序 `public static` 正确 |
| A7 | Javadoc 规范 | ⚠️ | A7.1 类和方法均有 Javadoc（✅）；A7.2 `Hello.java:5` — `@date` 非标准 Javadoc 块标记，建议改用 `@since` 或移除 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

> 预扫脚本 `scan-all-rules.sh` 对 `Hello.java` 无命中（52/222 规则）。以下由 LLM 逐条核对，本文件为简单 Hello World 程序，不涉及数组、集合、并发、异常、日期、IO、JDBC、JUnit 等场景，所有 Bug 模式均不适用。

| ID | 状态 | 备注 |
|----|------|------|
| B001 | N/A | 无 parse/of 方法调用 |
| B002 | N/A | 无数组比较 |
| B003 | N/A | 无 Arrays.fill |
| B004 | N/A | 无数组 toString |
| B005 | N/A | 无 Arrays.asList |
| B006 | N/A | 无 JUnit assertEquals |
| B007 | N/A | 无 catch(Throwable) |
| B008 | N/A | 无 Executors |
| B009 | N/A | 无位移操作 |
| B010 | N/A | 无 BigDecimal |
| B011 | N/A | 无包装类型比较 |
| B012 | N/A | 无 Calendar 操作 |
| B013 | N/A | 无 Calendar 操作 |
| B014 | N/A | 无集合查询 |
| B015 | N/A | 无 toArray |
| B016 | N/A | 无 Comparable |
| B017 | N/A | 无 this==null |
| B018 | N/A | 无三目运算符 |
| B019 | N/A | 无 Money API |
| B020 | N/A | 无常量溢出 |
| B021 | N/A | 无 Jedis |
| B022 | N/A | 无 SimpleDateFormat |
| B023 | N/A | 无未抛出异常 |
| B024 | N/A | 无未启动线程 |
| B025 | N/A |无双括号初始化 |
| B026 | N/A | 无 equals(null) |
| B027 | N/A | 无 equals 实现 |
| B028 | N/A | 无 DateUtil |
| B029 | N/A | 无 setter |
| B030 | N/A | 无浮点比较 |
| B031 | N/A | 无 String.format |
| B032 | N/A | 无注解 getClass |
| B033 | N/A | 无 Unsafe |
| B034 | N/A | 无 Hashtable |
| B035 | N/A | 无恒等二元运算 |
| B036 | N/A | 无 IdentityHashMap |
| B037 | N/A | 无可变参数 |
| B038 | N/A | 无递归 |
| B039 | N/A | 无 indexOf |
| B040 | N/A | 无 isInstance |
| B041 | N/A | 无 JDBC |
| B042 | N/A | 无 JUnit3 |
| B043 | N/A | 无 JUnit4 内部类 |
| B044 | N/A | 无 JUnit 混用 |
| B045 | N/A | 无包装类型加锁 |
| B046 | N/A | 无循环条件 |
| B047 | N/A | 无 compare |
| B048 | N/A | 无 Math.round |
| B049 | N/A | 无日期格式化 |
| B050 | N/A | 无日期格式化 |
| B051 | N/A | 无 Boolean.getBoolean |
| B052 | N/A | 无日期格式化 |
| B053 | N/A | 无 JUnit fail |
| B054 | N/A | 无 EqualsTester |
| B055 | N/A | 无 Mockito |
| B056 | N/A | 无 Arrays.asList 修改 |
| B057 | N/A | 无增强 for 循环修改集合 |
| B058 | N/A | 无集合自操作 |
| B059 | N/A | 无 nCopies |
| B060 | N/A | 无三目运算 null 拆箱 |
| B061 | N/A | 无 BASE64Encoder |
| B062 | N/A | 无 ClassLoader 转型 |
| B063 | N/A | 无 javax.xml |
| B064 | N/A | 无 Optional 比较 |
| B065 | N/A | 无 Pojo 自赋值 |
| B066 | N/A | 无 Math.random 强转 |
| B067 | N/A | 无 Random 取余 |
| B068 | N/A | 无自赋值 |
| B069 | N/A | 无自比较 |
| B070 | N/A | 无自 equals |
| B071 | N/A | 无 size()>=0 |
| B072 | N/A | 无 Stream.toString |
| B073 | N/A | 无 StringBuilder(char) |
| B074 | N/A | 无 substring(0) |
| B075 | N/A | 无可疑 for 循环 |
| B076 | N/A | 无 @Transactional |
| B077 | N/A | 无 catch(Throwable) |
| B078 | N/A | 无 isEqualTo 自身 |
| B079 | N/A | 无 @Mock 赋值 |
| B080 | N/A | 无无断言单测 |
| B081 | N/A | 无集合 sort 后未用 |
| M001 | N/A | 无重复条件判断 |
| M002 | N/A | 无 instanceof |
| M003 | N/A | 无包装类构造器 |
| M004 | N/A | 无 printStackTrace |
| M005 | N/A | 无内部类 |
| M006 | N/A | 无编译期布尔常量 |
| M007 | N/A | 无空 catch |
| M008 | N/A | 无 equals/hashCode |
| M009 | N/A | 无不兼容 equals |
| M010 | N/A | 无位运算 |
| M011 | N/A | 无 switch fall-through |
| M012 | N/A | 无 finally return/throw |
| M013 | N/A | 无类型转换 |
| M014 | N/A | 无枚举 getClass |
| M015 | N/A | 无字段隐藏 |
| M016 | N/A | 无默认时区方法 |
| M017 | N/A | 无 JUnit4 缺 @Test |
| M018 | N/A | 无 lock/unlock |
| M019 | N/A | 无 enum switch |
| M020 | N/A | 无缺 @Override |
| M021 | N/A | 无非重写 equals |
| M022 | N/A | 无 Optional.of(null) |
| M023 | N/A | 无 Object.toString 打印 |
| M024 | N/A | 无 Optional.get() |
| M025 | N/A | 无 final 类 protected |
| M026 | N/A | 无 static @Mock |
| M027 | N/A | 无 ThreadLocal |
| I001 | N/A | 无 JUnit 异常断言 |
| I002 | N/A | 无 @DoNotMock |
| I003 | N/A | 无 @AutoValue |
| I004 | N/A | 无 java.util.Date |
| I005 | N/A | 无 JUnit3 特性 |
| I006 | N/A | 无 setUp |
| I007 | N/A | 无 tearDown |
| I008 | N/A | 无 dataProvider |
| I009 | N/A | 无单测统计 |
| I010 | N/A | 无容器测试 |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | N/A | 无并发场景 |
| G1.2 | N/A | 无并发场景 |
| G1.3 | N/A | 无并发场景 |
| G1.4 | N/A | 无并发场景 |
| G2.1 | N/A | 无写接口/MQ 消费 |
| G2.2 | N/A | 无重试/定时任务 |
| G2.3 | N/A | 无幂等键约定 |
| G3.1 | N/A | 无分布式事务 |
| G3.2 | N/A | 无 @Transactional |
| G4.1 | N/A | 无 SQL |
| G4.2 | N/A | 无 SQL |
| G4.3 | N/A | 无 SQL |
| G4.4 | N/A | 无 SQL |
| G5.1 | N/A | 无 MQ |
| G6.1 | N/A | 无缓存 |
| G6.2 | N/A | 无缓存 |
| G7.1 | N/A | 无调度任务 |
| G7.2 | N/A | 无调度任务 |
| G8.1 | N/A | 无异常处理 |
| G8.2 | N/A | 无外部依赖 |
| G8.3 | N/A | 无 I/O 资源 |
| G8.4 | N/A | 无线程池/定时任务 |
| G8.5 | N/A | 无 ThreadLocal |
| G8.6 | N/A | 无线程池 |
| G8.7 | N/A | 无线程池 |
| G9.1 | N/A | 无网络调用 |
| G9.2 | N/A | 无网络调用 |
| G9.3 | N/A | 无网络调用 |
| G10.1 | N/A | 无接口契约 |
| G10.2 | N/A | 无接口契约 |
| G11.1 | ⚠️ | `Hello.java` — 新增逻辑无单测，P2（程序为简单 Hello World，影响极低） |
| G11.2 | N/A | 无边界场景 |
| G11.3 | N/A | args 参数未使用，无防御性校验需求 |
| G11.4 | N/A | 无数值运算 |
| G12.1 | N/A | 无资损场景 |
| G12.2 | N/A | 无止血需求 |
| G13.1 | N/A | 无日志 |
| G14.1 | N/A | 无金额 |
| G14.2 | N/A | 无多租户 |
| G14.3 | N/A | 无时区处理 |
| G14.4 | N/A | 无日期格式化 |
| G15.1 | N/A | 无 DB 变更 |
| G15.2 | N/A | 无新旧接口 |
| G15.3 | N/A | 无不兼容逻辑 |
| G16.1 | N/A | 无核心链路 |
| G16.2 | N/A | 无异常路径 |
| G16.3 | N/A | 无日志级别 |
| G16.4 | N/A | 无空 catch |
| G17.1 | N/A | 无功能开关 |
| G17.2 | N/A | 无降级预案 |
| G17.3 | N/A | 无数据变更 |
| G18.1 | N/A | 安全补强不适用 |
| G18.2 | N/A | 安全补强不适用 |
| G18.3 | N/A | 安全补强不适用 |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | N/A | 无 SQL |
| S1.2 | N/A | 无 SQL |
| S1.3 | N/A | 无 SQL |
| S2.1 | N/A | 无 HTML/JS 输出 |
| S2.2 | N/A | 无富文本 |
| S2.3 | N/A | 无模板引擎 |
| S3.1 | N/A | 无外部 URL 请求 |
| S3.2 | N/A | 无 URL 跳转 |
| S3.3 | N/A | 无 HTTP 超时 |
| S4.1 | N/A | 无系统命令 |
| S4.2 | N/A | 无文件/图片操作 |
| S5.1 | N/A | 无 XML 解析 |
| S5.2 | N/A | 无 XPath |
| S6.1 | N/A | 无反序列化 |
| S6.2 | N/A | 无 JSON 反序列化 |
| S6.3 | N/A | 无敏感字段 |
| S7.1 | N/A | 无文件上传 |
| S7.2 | N/A | 无文件下载 |
| S7.3 | N/A | 无文件重命名 |
| S8.1 | N/A | 无接口鉴权 |
| S8.2 | N/A | 无 GET 增删改 |
| S8.3 | N/A | 无数据 ID |
| S8.4 | N/A | 无 Cookie |
| S9.1 | N/A | 无密钥 |
| S9.2 | N/A | 无敏感日志 |
| S9.3 | N/A | 无传输/存储加密 |
| S9.4 | N/A | 无随机数 |
| S10.1 | N/A | 无 CSRF |
| S10.2 | N/A | 无 CORS |
| S10.3 | N/A | 无 URL 跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

### 5.1 自定义扩展（`customized-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | N/A | `customized-checklist.md` 仅含示例项，未启用自定义规则 |
| U1.2 | N/A | 同上 |
| U1.3 | N/A | 同上 |
| U2.1 | N/A | `customized-checklist.md` U2 节为空，未启用自定义规则 |
| U2.2 | N/A | 同上 |
| U2.3 | N/A | 同上 |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3`、**S1–S10 / G1–G17** 各列均非 `⬜`（G11=⚠️，其余=N/A）；
- [x] Step 2 的每个 REQ/Scenario 均非 `⬜`
- [x] Step 3 的 A1–A7 均非 `⬜`
- [x] Step 4 全部 **G/S** 与 **B001–B081 / M001–M027 / I001–I010** ID 均非 `⬜`（均为 N/A 或 ⚠️，含原因）
- [x] Step 5 全部 U* ID 均非 `⬜`（均为 N/A(未启用自定义规则)）
- [x] 所有 `❌/⚠️` 已写入 report，且包含 `ID + path:line`
