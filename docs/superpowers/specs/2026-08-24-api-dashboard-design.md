# 三接口展示与调用分析报表系统 — 设计文档

> 日期: 2026-08-24
> 技术栈: Java Spring Boot 3.x + React 18.x
> 状态: 设计草案

---

## 1. 概述

### 1.1 业务目标
构建一个包含三个后端接口（HelloWorld、哈希算法、冒泡排序）的展示页面，并提供结果导出、接口调用埋点及可视化报表功能。

### 1.2 核心功能
1. 三个 REST 接口（HelloWorld / Hash / BubbleSort）
2. 前端三 Tab 页面展示各接口调用结果
3. 导出按钮 + 后端导出 API
4. 后端调用埋点（调用次数、调用人）
5. 前端可视化报表（折线图/饼图/柱状图，按人员类型/层级/部门维度）

---

## 2. 整体架构

```
┌──────────────────────────────────────────────────┐
│                   React SPA                       │
│  ┌──────┐┌──────┐┌──────┐┌──────┐┌───────────┐ │
│  │Hello ││ Hash ││Bubble││导出  ││ 报表仪表盘 │ │
│  │ Tab  ││ Tab  ││ Tab  ││按钮  ││ (图表)    │ │
│  └──────┘└──────┘└──────┘└──────┘└───────────┘ │
└──────────────────────┬───────────────────────────┘
                       │ HTTP REST API
┌──────────────────────▼───────────────────────────┐
│              Spring Boot Backend                  │
│  ┌──────────────────────────────────────────────┐ │
│  │ Controller Layer                              │ │
│  │  HelloController / HashController /           │ │
│  │  BubbleController / ExportController /        │ │
│  │  AnalyticsController                          │ │
│  └──────────────────────┬───────────────────────┘ │
│  ┌──────────────────────▼───────────────────────┐ │
│  │ Service Layer                                 │ │
│  │  HelloService / HashService / BubbleService  │ │
│  │  ExportService / AnalyticsService             │ │
│  └──────────────────────┬───────────────────────┘ │
│  ┌──────────────────────▼───────────────────────┐ │
│  │ AOP Aspect (埋点切面)                         │ │
│  │  @Traceable 注解 → 拦截 + 记录调用日志        │ │
│  └──────────────────────┬───────────────────────┘ │
│  ┌──────────────────────▼───────────────────────┐ │
│  │ Data Layer (Spring Data JPA)                  │ │
│  │  CallLog Entity / Repository                  │ │
│  │  H2 (开发) / MySQL (生产)                      │ │
│  └──────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

---

## 3. 后端详细设计

### 3.1 项目结构

```
backend/
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java
│   ├── controller/
│   │   ├── HelloController.java
│   │   ├── HashController.java
│   │   ├── BubbleController.java
│   │   ├── ExportController.java
│   │   └── AnalyticsController.java
│   ├── service/
│   │   ├── HelloService.java
│   │   ├── HashService.java
│   │   ├── BubbleService.java
│   │   ├── ExportService.java
│   │   └── AnalyticsService.java
│   ├── model/
│   │   ├── HelloResult.java
│   │   ├── HashResult.java
│   │   ├── BubbleResult.java
│   │   ├── CallLog.java
│   │   └── AnalyticsQuery.java
│   ├── repository/
│   │   └── CallLogRepository.java
│   ├── aspect/
│   │   └── TraceableAspect.java
│   ├── annotation/
│   │   └── Traceable.java
│   └── dto/
│       ├── ApiResponse.java
│       ├── ExportRequest.java
│       └── AnalyticsResponse.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
└── pom.xml
```

### 3.2 接口定义

#### 3.2.1 HelloWorld 接口

```
GET /api/hello?name=xxx
Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "greeting": "Hello, xxx! Welcome to DTCoder Demo."
  }
}
```

#### 3.2.2 哈希算法接口

```
POST /api/hash
Request Body: { "input": "hello", "algorithm": "SHA-256" }
Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "input": "hello",
    "algorithm": "SHA-256",
    "hash": "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
  }
}
```

支持的算法：SHA-256, MD5, SHA-512

#### 3.2.3 冒泡排序接口

```
POST /api/bubble-sort
Request Body: { "array": [5, 3, 8, 1, 2] }
Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "originalArray": [5, 3, 8, 1, 2],
    "sortedArray": [1, 2, 3, 5, 8],
    "swapCount": 7,
    "comparisonCount": 10
  }
}
```

#### 3.2.4 导出接口

```
GET /api/export?type=hello&format=csv
GET /api/export?type=hash&format=csv
GET /api/export?type=bubble&format=csv
Response: 文件流 (Content-Disposition: attachment)
```

支持的 format: csv (默认), xlsx

#### 3.2.5 埋点分析接口

```
GET /api/analytics/summary?dimension=personType&startTime=...&endTime=...
GET /api/analytics/summary?dimension=personLevel&startTime=...&endTime=...
GET /api/analytics/summary?dimension=department&startTime=...&endTime=...

Response:
{
  "code": 200,
  "data": {
    "dimension": "personType",
    "series": [
      { "label": "研发", "value": 150 },
      { "label": "测试", "value": 80 },
      { "label": "产品", "value": 45 }
    ],
    "totalCalls": 275
  }
}
```

### 3.3 数据模型 — CallLog 埋点表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, AUTO) | 主键 |
| api_name | VARCHAR(100) | 调用的接口名 (hello/hash/bubble) |
| caller_name | VARCHAR(100) | 调用人姓名 |
| person_type | VARCHAR(50) | 人员类型 (研发/测试/产品等) |
| person_level | VARCHAR(50) | 人员层级 (初级/中级/高级/专家) |
| department | VARCHAR(100) | 人员部门 |
| call_time | DATETIME | 调用时间 |
| duration_ms | BIGINT | 耗时(毫秒) |
| status | VARCHAR(20) | 调用状态 (success/fail) |

### 3.4 埋点实现（AOP + 自定义注解）

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {
    String apiName() default "";
    // 调用人、人员类型、层级、部门可从请求头或 Token 中获取
}

@Aspect
@Component
public class TraceableAspect {
    // 环绕通知拦截 @Traceable 注解的方法
    // 记录调用时间、参数、结果状态
    // 异步保存到 CallLogRepository
    // 调用人信息从请求头 X-Caller-Name, X-Person-Type, X-Person-Level, X-Department 获取
}
```

### 3.5 导出实现

- **CSV 导出**: 使用 OpenCSV，逐行写入，流式响应
- **Excel 导出**: 使用 Apache POI，生成 .xlsx 文件
- 从各个 Service 获取当前数据，统一格式后输出

---

## 4. 前端详细设计

### 4.1 项目结构

```
frontend/
├── src/
│   ├── App.jsx
│   ├── App.css
│   ├── main.jsx
│   ├── pages/
│   │   └── Dashboard.jsx          # 主页面（含 Tab + 报表）
│   ├── components/
│   │   ├── HelloTab.jsx            # HelloWorld Tab
│   │   ├── HashTab.jsx             # 哈希算法 Tab
│   │   ├── BubbleTab.jsx           # 冒泡排序 Tab
│   │   ├── ExportButton.jsx        # 导出按钮组件
│   │   └── AnalyticsChart.jsx      # 报表图表组件
│   ├── services/
│   │   └── api.js                  # API 请求封装
│   └── utils/
│       └── constants.js            # 常量定义
├── package.json
├── vite.config.js
└── index.html
```

### 4.2 页面布局

```
┌─────────────────────────────────────────────────────┐
│  [HelloWorld Tab]  [Hash Tab]  [BubbleSort Tab]     │
│                                          [导出按钮]  │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Tab 内容区域：                                       │
│  - 输入表单（名称/输入文本/数组）                      │
│  - 调用按钮                                           │
│  - 结果展示区                                         │
│                                                      │
├─────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐│
│  │  调用分析报表                                      ││
│  │  ┌──────────────┐ ┌──────────────┐ ┌───────────┐││
│  │  │  折线图       │ │  饼图        │ │  柱状图   │││
│  │  │  (按时间趋势) │ │  (人员类型)  │ │ (人员部门)│││
│  │  └──────────────┘ └──────────────┘ └───────────┘││
│  │  维度切换: [人员类型] [人员层级] [人员部门]        ││
│  └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

### 4.3 图表设计

| 图表类型 | 展示维度 | 数据说明 |
|----------|----------|----------|
| 折线图 | 时间趋势 | 按天/小时展示各接口调用量变化曲线 |
| 饼图 | 人员类型 | 各类人员（研发/测试/产品）调用占比 |
| 柱状图 | 人员部门 | 各部门调用次数对比 |

### 4.4 前端调用埋点传参

前端在调用 API 时，通过请求头传递调用人信息：
```
X-Caller-Name: 张三
X-Person-Type: 研发
X-Person-Level: 高级
X-Department: 技术部
```

---

## 5. 数据流

```
用户操作 → 前端 Tab → 调用后端 API → AOP 切面拦截
                                         ↓
                                   记录 CallLog
                                         ↓
                                   返回结果给前端
                                         ↓
                                   前端展示结果
                                   
用户查看报表 → 前端调用 /api/analytics/summary
                                    → 后端聚合查询 CallLog
                                    → 返回聚合数据给前端
                                    → ECharts 渲染图表
```

---

## 6. 开放问题（待澄清）

1. **导出格式**: CSV / Excel / PDF / 全部支持？
2. **调用人身份**: 是否需要登录认证，还是前端模拟传入？
3. **数据库**: 开发阶段使用 H2 内存数据库？生产是否迁移 MySQL？
4. **前端 UI 框架**: Ant Design / Material UI / 其他？
5. **图表库**: ECharts / Recharts / Chart.js？
6. **部署方式**: 前后端分离部署还是打成一个 jar 包？
7. **哈希算法**: 支持的算法列表（SHA-256, MD5, SHA-512 等）？

---

## 7. 附录

### 7.1 API 路由汇总

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/hello | HelloWorld 接口 |
| POST | /api/hash | 哈希计算 |
| POST | /api/bubble-sort | 冒泡排序 |
| GET | /api/export | 导出结果 |
| GET | /api/analytics/summary | 分析报表数据 |

### 7.2 依赖清单

**后端 (pom.xml)**:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-aop
- h2database (dev)
- mysql-connector-j (prod)
- opencsv / apache-poi

**前端 (package.json)**:
- react, react-dom
- react-router-dom
- echarts, echarts-for-react
- axios
- antd (Ant Design)
- @vitejs/plugin-react
