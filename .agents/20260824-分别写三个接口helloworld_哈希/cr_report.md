# Code Review Report

> **项目名称**: 三接口展示与调用分析报表系统  
> **审查日期**: 2026-08-24  
> **审查范围**: 全量代码（后端 Java Spring Boot 3.x + 前端 React 18/Vite）  
> **审查人**: DTCoder (AI Code Review)

---

## 审查总结

本次审查覆盖 22 个后端源文件 + 6 个测试文件 + 10 个前端文件。整体架构设计合理，代码结构清晰，功能覆盖完整。但存在 **3 个 blocking 问题**、**5 个 important 问题** 和 **7 个 nit 问题**。

### 总体评分: ⚠️ 需修改后合并

| 严重度 | 数量 | 说明 |
|--------|------|------|
| 🔴 blocking | 3 | 必须修复后才能合并 |
| 🟡 important | 5 | 建议修复，讨论后可合并 |
| 🟢 nit | 7 | 非阻塞性优化建议 |
| 💡 suggestion | 3 | 可考虑的可选改进 |

---

## 🔴 Blocking 问题

### 1. TraceableAspect 同步 DB 写入阻塞 API 响应

**文件**: `backend/src/main/java/com/example/demo/aspect/TraceableAspect.java:68`  
**代码**: `callLogRepository.save(log);`

**问题**: AOP 切面在 `finally` 块中同步调用 `callLogRepository.save()`，导致每个 API 请求都必须等待数据库写入完成后才返回响应。随着调用量增长，这会显著增加接口延迟。

**建议修复**: 使用 `@Async` + `@EnableAsync` 异步保存，或通过消息队列解耦。

```java
// 在 DemoApplication 或配置类上加 @EnableAsync
// TraceableAspect 中注入 AsyncTaskExecutor
@Async
private void saveCallLogAsync(...) {
    // 异步保存逻辑
}
```

---

### 2. ExportService 导出硬编码示例数据

**文件**: `backend/src/main/java/com/example/demo/service/ExportService.java:31,39,47`

```java
// line 31
HelloResult result = helloService.greet("Sample");  // 硬编码
// line 39
HashResult result = hashService.computeHash("sample-data", "SHA-256");  // 硬编码
// line 47
BubbleResult result = bubbleService.sort(Arrays.asList(5, 3, 8, 1, 2));  // 硬编码
```

**问题**: 导出接口没有导出实际用户调用产生的数据，而是生成硬编码的示例数据。这与设计文档中"从各个 Service 获取当前数据"的要求不符。

**建议修复**: 导出接口应接收用户参数（而非硬编码），或从数据库/缓存中获取真实的调用记录数据来导出。

---

### 3. AnalyticsChart 对同一维度同时渲染三种图表

**文件**: `frontend/src/components/AnalyticsChart.jsx:137-158`

**问题**: 组件同时渲染三种图表（折线图、饼图、柱状图），但它们都显示**同一维度的相同数据**，造成功能冗余。根据设计文档：
- 折线图 → 时间趋势（按天/小时展示各接口调用量变化曲线）
- 饼图 → 人员类型（各类人员调用占比）
- 柱状图 → 人员部门（各部门调用次数对比）

当前实现将所有图表绑定到同一个 `dimension` 状态，当选择"人员类型"时，三种图表都显示人员类型数据。

**建议修复**: 固定三个图表各自对应的维度，互不依赖：
- 左侧图表固定为折线图（时间趋势 `timeTrend`）
- 中间图表固定为饼图（人员类型 `personType`）
- 右侧图表固定为柱状图（部门 `department`）
- 维度切换下拉框改为仅影响饼图/柱状图的维度选择，或直接移除维度切换，按设计稿固定展示

---

## 🟡 Important 问题

### 4. CSV 导出未使用 OpenCSV 依赖

**文件**: 
- `backend/pom.xml:40-44`（声明了 OpenCSV 依赖）
- `backend/src/main/java/com/example/demo/service/ExportService.java:6`（导入了但未使用）
- `backend/src/main/java/com/example/demo/service/ExportService.java:54-78`（使用手动 StringBuilder 拼接 CSV）

**问题**: 项目依赖中包含了 OpenCSV 5.9，但 CSV 导出使用手动 StringBuilder 拼接，未处理 CSV 转义（如字段含逗号、引号、换行符等情况），存在数据完整性风险。

**建议修复**: 使用 OpenCSV 的 `CSVWriter` 替换手动拼接，确保字段正确转义。

---

### 5. HashController 和 BubbleController 使用 Map 接收请求体

**文件**: 
- `backend/src/main/java/com/example/demo/controller/HashController.java:21`
- `backend/src/main/java/com/example/demo/controller/BubbleController.java:22`

**问题**: 使用 `Map<String, String>` 和 `Map<String, List<Integer>>` 接收请求体，缺乏类型校验和文档化。建议使用专用 DTO 类。

**建议修复**: 
- 创建 `HashRequest` DTO 包含 `input` 和 `algorithm` 字段
- 创建 `BubbleRequest` DTO 包含 `array` 字段

---

### 6. ExportController 和 AnalyticsController 缺少 @Traceable 注解

**文件**: 
- `backend/src/main/java/com/example/demo/controller/ExportController.java`
- `backend/src/main/java/com/example/demo/controller/AnalyticsController.java`

**问题**: 导出接口和分析报表接口未标注 `@Traceable`，导致这些接口的调用不会被记录到埋点日志中，分析报表无法反映完整的调用情况。

**建议修复**: 在 `ExportController.export()` 和 `AnalyticsController.getSummary()` 方法上添加 `@Traceable` 注解。

---

### 7. AnalyticsChart 折线图和柱状图的 tooltip 格式不正确

**文件**: `frontend/src/components/AnalyticsChart.jsx:49`

```javascript
tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)', // {d}% 仅适用于饼图
}
```

**问题**: `{d}%` 是饼图的百分比格式，折线图和柱状图使用 `{d}` 不显示百分比，应区分图表类型的 tooltip 格式。

---

## 🟢 Nit 问题

### 8. 未使用的 import 语句

| 文件 | 行号 | 未使用的导入 |
|------|------|-------------|
| `frontend/src/components/HelloTab.jsx` | 5 | `const { TextArea } = Input;` |
| `frontend/src/components/HashTab.jsx` | 6 | `const { TextArea } = Input;` |
| `frontend/src/components/BubbleTab.jsx` | 5 | `const { TextArea } = Input;` |
| `frontend/src/components/BubbleTab.jsx` | 6 | `const { Title, Text } = Typography;` (Title 未使用) |
| `frontend/src/components/HashTab.jsx` | 7 | `const { Title, Text } = Typography;` (Title 未使用) |
| `frontend/src/pages/Dashboard.jsx` | 2 | `Space` 未使用 |
| `frontend/src/components/ExportButton.jsx` | 2 | `Space` 未使用 |
| `frontend/src/components/BubbleTab.jsx` | 2 | `List` 未使用 |
| `backend/src/main/java/com/example/demo/service/ExportService.java` | 6,13 | `CSVWriter`, `OutputStreamWriter` 未使用 |

---

### 9. Bubble CSV 导出中数组字段格式问题

**文件**: `backend/src/main/java/com/example/demo/service/ExportService.java:73-74`

```java
sb.append(result.getOriginalArray()).append(",")
  .append(result.getSortedArray()).append(",")
```

`List.toString()` 输出 `[1, 2, 3]` 格式，包含方括号和空格，作为 CSV 格式不标准。

---

### 10. 测试覆盖率不足

| 测试文件 | 缺失的测试用例 |
|---------|---------------|
| `AnalyticsControllerTest.java` | 缺少 `personLevel` 和 `timeTrend` 维度的测试 |
| `ExportControllerTest.java` | 缺少 `bubble` 类型的导出测试 |
| `BubbleControllerTest.java` | 缺少边界值测试（单元素数组、空数组、重复元素） |
| `HashControllerTest.java` | 缺少无效算法入参的异常测试 |

---

## 💡 改进建议

### 11. 前端使用 React.memo 优化图表组件

**文件**: `frontend/src/components/AnalyticsChart.jsx`

`AnalyticsChart` 在 dimension 未变化时不应重新渲染，使用 `React.memo` 可避免不必要的 ECharts 重绘。

### 12. 后端全局异常处理

**建议**: 增加 `@ControllerAdvice` 全局异常处理器，统一处理 `IllegalArgumentException` 等业务异常，返回标准 `ApiResponse<T>` 格式，避免直接返回 500 错误。

### 13. 前端请求头动态化

**文件**: `frontend/src/services/api.js`

当前调用人信息在 Axios 实例创建时静态设置，无法动态切换用户。建议使用请求拦截器动态读取。

---

## 需求覆盖检查

| 需求 | 状态 | 备注 |
|------|------|------|
| HelloWorld GET 接口 | ✅ 已实现 | `GET /api/hello?name=xxx` |
| 哈希算法 POST 接口 | ✅ 已实现 | `POST /api/hash`，支持 SHA-256/MD5/SHA-512 |
| 冒泡排序 POST 接口 | ✅ 已实现 | `POST /api/bubble-sort` |
| 前端三 Tab 页面 | ✅ 已实现 | HelloTab / HashTab / BubbleTab |
| 导出按钮 + 后端导出 API | ⚠️ 部分实现 | 后端导出接口已实现，但导出的是硬编码数据 |
| 后端埋点(AOP + @Traceable) | ✅ 已实现 | 但同步写入需优化 |
| 前端可视化报表(折线图/饼图/柱状图) | ⚠️ 部分实现 | 三种图表同时渲染同一维度数据 |
| 维度切换(人员类型/层级/部门) | ⚠️ 部分实现 | 维度切换影响所有图表，而非按设计分离 |

---

## 最终建议

**阻止合并的 3 个问题必须修复**（尤其是 ExportService 硬编码数据和 AnalyticsChart 图表逻辑），其余问题建议在后续迭代中修复。