# Step4 接口设计

## 4.1 oneapi（Web 控制台接口，/api 前缀）
| 编号 | 接口名称 | 方法 | 路径 | 模块 |
|------|----------|------|------|------|
| W01 | helloworld 执行 | POST | /api/demo/helloworld | demo |
| W02 | 哈希算法执行 | POST | /api/demo/hash | demo |
| W03 | 冒泡排序执行 | POST | /api/demo/bubble-sort | demo |
| W04 | 页面结果导出 | POST | /api/export | export |
| W05 | 调用概况查询 | GET | /api/tracking/overview | tracking |
| W06 | 维度统计查询 | GET | /api/tracking/stats | tracking |
| W07 | 时间趋势查询 | GET | /api/tracking/trend | tracking |

## 4.2 OpenAPI（对外接口）
- 本设计不提供 OpenAPI 对外接口，原因：全部能力面向 Web 控制台演示场景，无外部业务系统调用方。

## 4.3 内部接口（Service 层）
| 编号 | 接口名称 | 类 | 方法签名 |
|------|----------|------|----------|
| S01 | helloworld 服务 | DemoService | String hello(DemoContext ctx) |
| S02 | 哈希服务 | DemoService | HashResult hash(String text, HashAlgorithm algorithm, DemoContext ctx) |
| S03 | 冒泡排序服务 | DemoService | SortResult bubbleSort(List<BigDecimal> input, SortOrder order, boolean optimized, DemoContext ctx) |
| S04 | 页面导出服务 | ExportService | ExportFile export(ExportTarget target, ExportFormat format, DateRange range, DemoContext ctx) |
| S05 | 埋点记录服务 | TrackingService | void record(CallRecord record)（异步批量入库） |
| S06 | 概况/维度/趋势统计 | TrackingService | OverviewVO overview(...); StatsVO stats(StatsDimension dim, DateRange range); TrendVO trend(Granularity g, DateRange range) |
| S07 | 调用人上下文解析 | CallContextResolver | CallerInfo resolve()（从登录上下文/请求头解析 人员ID/姓名/类型/层级/部门） |

## 4.4 集成接口（Integration 层）
| 编号 | 接口名称 | 类 | 方法签名 | 说明 |
|------|----------|------|----------|------|
| I01 | 登录/人员信息解析 | UserInfoClient | CallerInfo getUserInfo(String token) | 集成统一登录体系（办公网 BUC 等，A03）；演示环境可用请求头模拟解析，属可选集成点 |