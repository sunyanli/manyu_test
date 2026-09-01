# 三接口 + 前端 + 导出 + 埋点可视化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现三个后端接口（HelloWorld、哈希算法、冒泡排序），前端单页面三个Tab展示，导出功能，埋点统计及可视化报表（折线图/饼图/柱状图）。

**Architecture:** 后端 Python Flask 提供 RESTful API，前端原生 HTML/JS/CSS（ECharts 图表）单页面应用。两个仓库独立部署，跨域通信。后端内置内存存储埋点数据，提供统计聚合与导出接口。

**Tech Stack:**
- 后端：Python 3 + Flask + flask-cors
- 前端：原生 HTML5 + CSS3 + JavaScript (ES6) + ECharts (CDN)
- 数据格式：JSON（API 通信）/ CSV（导出）
- 存储：Python 内存数据结构（list/dict）

---

## Global Constraints

- 后端 Python ≥ 3.8，Flask ≥ 2.0
- 前端兼容 Chrome/Firefox/Edge 最新版本
- 所有 API 接口返回 JSON，Content-Type: application/json
- 跨域支持：Flask-CORS 全开（开发阶段）
- 冒泡排序复用已有 `bubble_sort.py` 中的 `bubble_sort` 函数
- 哈希算法支持 SHA256 和 MD5 双算法
- 埋点数据存储于内存（重启丢失可接受）
- 导出格式为 CSV
- 调用人识别：页面输入用户名（简单方案，无登录认证）
- 可视化库：ECharts 5.x CDN 引入

---

## 文件结构

### 后端仓库 `manyu_test`（物理路径：`manyu_test-cred-test-20260716022903/`）

| 文件 | 操作 | 职责 |
|------|------|------|
| `app.py` | 创建 | Flask 应用入口，CORS 配置，注册所有路由蓝图 |
| `hello_world.py` | 创建 | HelloWorld 接口逻辑：`GET /api/hello` |
| `hash_algo.py` | 创建 | 哈希算法接口逻辑：`POST /api/hash`，支持 SHA256/MD5 |
| `bubble_sort.py` | 已有（复用） | 冒泡排序函数（已存在，无需修改） |
| `sort_api.py` | 创建 | 冒泡排序接口包装：`POST /api/sort`，调用 `bubble_sort.py` |
| `tracking.py` | 创建 | 埋点数据模型 + 记录逻辑 + 统计聚合查询 |
| `export.py` | 创建 | 导出接口：`GET /api/export?tab=xxx`，生成 CSV |
| `requirements.txt` | 创建 | 项目依赖声明 |

### 前端仓库 `manyu_test1`（物理路径：`manyu_test1-main/`）

| 文件 | 操作 | 职责 |
|------|------|------|
| `index.html` | 创建 | 主页面：Tab 布局 + 导出按钮 + 报表区域，ECharts CDN 引入 |
| `style.css` | 创建 | 页面样式：Tab 切换、按钮、表格、图表布局 |
| `app.js` | 创建 | 前端逻辑：API 调用、Tab 切换、用户名输入、ECharts 图表渲染 |

---

## Task 1: 后端 Flask 应用骨架 + HelloWorld 接口

**Files:**
- Create: `manyu_test/app.py`
- Create: `manyu_test/hello_world.py`
- Create: `manyu_test/requirements.txt`

**Interfaces:**
- Produces: `GET /api/hello` → `{ "message": "Hello World!", "timestamp": "2026-09-01T12:00:00Z" }`

- [ ] **Step 1: 创建 requirements.txt**

```txt
flask>=2.0.0
flask-cors>=3.0.10
```

- [ ] **Step 2: 创建 hello_world.py**

```python
import datetime

def hello_world():
    """HelloWorld 接口逻辑"""
    return {
        "message": "Hello World!",
        "timestamp": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    }
```

- [ ] **Step 3: 创建 app.py（骨架，含 /api/hello 路由）**

```python
from flask import Flask, jsonify
from flask_cors import CORS
from hello_world import hello_world

app = Flask(__name__)
CORS(app)

@app.route('/api/hello', methods=['GET'])
def api_hello():
    result = hello_world()
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

- [ ] **Step 4: 验证 Flask 应用启动**

Run: `cd manyu_test-cred-test-20260716022903 && pip install -r requirements.txt -q && python app.py &`
Expected: 服务启动在 `http://0.0.0.0:5000`

- [ ] **Step 5: 验证 HelloWorld 接口**

Run: `curl http://localhost:5000/api/hello`
Expected: `{"message":"Hello World!","timestamp":"2026-09-01T..."}`

---

## Task 2: 哈希算法接口

**Files:**
- Create: `manyu_test/hash_algo.py`
- Modify: `manyu_test/app.py`（添加 /api/hash 路由）

**Interfaces:**
- Consumes: `POST /api/hash` 请求体 `{ "input": "str", "algorithm": "sha256|md5" }`
- Produces: `{ "input": "...", "algorithm": "sha256", "hash": "abc123..." }`

- [ ] **Step 1: 创建 hash_algo.py**

```python
import hashlib

def compute_hash(input_str: str, algorithm: str = "sha256"):
    """计算哈希值，支持 sha256 和 md5"""
    if algorithm not in ("sha256", "md5"):
        raise ValueError(f"Unsupported algorithm: {algorithm}")
    
    if algorithm == "sha256":
        hash_obj = hashlib.sha256(input_str.encode('utf-8'))
    else:
        hash_obj = hashlib.md5(input_str.encode('utf-8'))
    
    return {
        "input": input_str,
        "algorithm": algorithm,
        "hash": hash_obj.hexdigest()
    }
```

- [ ] **Step 2: 在 app.py 中添加 /api/hash 路由**

```python
from flask import Flask, jsonify, request
from flask_cors import CORS
from hello_world import hello_world
from hash_algo import compute_hash

app = Flask(__name__)
CORS(app)

@app.route('/api/hello', methods=['GET'])
def api_hello():
    result = hello_world()
    return jsonify(result)

@app.route('/api/hash', methods=['POST'])
def api_hash():
    data = request.get_json()
    if not data or 'input' not in data:
        return jsonify({"error": "Missing 'input' field"}), 400
    input_str = data['input']
    algorithm = data.get('algorithm', 'sha256')
    try:
        result = compute_hash(input_str, algorithm)
        return jsonify(result)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

- [ ] **Step 3: 验证哈希接口**

Run: `curl -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"input":"hello","algorithm":"sha256"}'`
Expected: `{"input":"hello","algorithm":"sha256","hash":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"}`

Run: `curl -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"input":"hello","algorithm":"md5"}'`
Expected: `{"input":"hello","algorithm":"md5","hash":"5d41402abc4b2a76b9719d911017c592"}`

---

## Task 3: 冒泡排序接口（复用已有 bubble_sort.py）

**Files:**
- Create: `manyu_test/sort_api.py`
- Modify: `manyu_test/app.py`（添加 /api/sort 路由）

**Interfaces:**
- Consumes: `POST /api/sort` 请求体 `{ "data": [5, 3, 8, 4, 2] }`
- Produces: `{ "original": [5,3,8,4,2], "sorted": [2,3,4,5,8], "algorithm": "bubble_sort" }`

- [ ] **Step 1: 创建 sort_api.py**

```python
from bubble_sort import bubble_sort

def sort_data(data: list):
    """包装冒泡排序为接口调用"""
    original = list(data)
    sorted_data = bubble_sort(list(data))
    return {
        "original": original,
        "sorted": sorted_data,
        "algorithm": "bubble_sort"
    }
```

- [ ] **Step 2: 在 app.py 中添加 /api/sort 路由**

```python
from flask import Flask, jsonify, request
from flask_cors import CORS
from hello_world import hello_world
from hash_algo import compute_hash
from sort_api import sort_data

app = Flask(__name__)
CORS(app)

@app.route('/api/hello', methods=['GET'])
def api_hello():
    result = hello_world()
    return jsonify(result)

@app.route('/api/hash', methods=['POST'])
def api_hash():
    data = request.get_json()
    if not data or 'input' not in data:
        return jsonify({"error": "Missing 'input' field"}), 400
    input_str = data['input']
    algorithm = data.get('algorithm', 'sha256')
    try:
        result = compute_hash(input_str, algorithm)
        return jsonify(result)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400

@app.route('/api/sort', methods=['POST'])
def api_sort():
    data = request.get_json()
    if not data or 'data' not in data:
        return jsonify({"error": "Missing 'data' field"}), 400
    if not isinstance(data['data'], list):
        return jsonify({"error": "'data' must be an array"}), 400
    result = sort_data(data['data'])
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

- [ ] **Step 3: 验证排序接口**

Run: `curl -X POST http://localhost:5000/api/sort -H "Content-Type: application/json" -d '{"data":[5,3,8,4,2]}'`
Expected: `{"original":[5,3,8,4,2],"sorted":[2,3,4,5,8],"algorithm":"bubble_sort"}`

---

## Task 4: 埋点模块（Tracking）

**Files:**
- Create: `manyu_test/tracking.py`
- Modify: `manyu_test/app.py`（集成埋点中间件 + 统计接口）

**Interfaces:**
- Produces:
  - `POST /api/track` → `{ "status": "ok", "id": "uuid" }`
  - `GET /api/stats/overview` → `{ "total_calls": N, "by_api": {...}, "by_user": {...} }`
  - `GET /api/stats/chart?dimension=user_type&chart_type=pie` → `{ "labels": [...], "values": [...], "type": "pie" }`
  - 后端自动埋点函数 `track_call(api_name, caller, user_type, user_level, department)`

- [ ] **Step 1: 创建 tracking.py**

```python
import uuid
import datetime
from collections import defaultdict

# 内存存储
_tracking_records = []
_api_call_count = 0

def track_call(api_name: str, caller: str = "anonymous",
               user_type: str = "developer", user_level: str = "mid",
               department: str = "engineering"):
    """记录一次 API 调用埋点"""
    global _api_call_count
    _api_call_count += 1
    record = {
        "id": str(uuid.uuid4()),
        "timestamp": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        "api": api_name,
        "caller": caller,
        "user_type": user_type,
        "user_level": user_level,
        "department": department
    }
    _tracking_records.append(record)
    return record

def get_overview():
    """获取统计数据概览"""
    global _api_call_count
    by_api = defaultdict(int)
    by_user = defaultdict(int)
    for r in _tracking_records:
        by_api[r["api"]] += 1
        by_user[r["caller"]] += 1
    return {
        "total_calls": _api_call_count,
        "by_api": dict(by_api),
        "by_user": dict(by_user)
    }

def get_chart_data(dimension: str = "user_type", chart_type: str = "pie"):
    """按维度获取图表数据"""
    if dimension not in ("user_type", "user_level", "department"):
        raise ValueError(f"Unsupported dimension: {dimension}")
    if chart_type not in ("pie", "line", "bar"):
        raise ValueError(f"Unsupported chart_type: {chart_type}")
    
    dim_counter = defaultdict(int)
    for r in _tracking_records:
        dim_counter[r[dimension]] += 1
    
    labels = list(dim_counter.keys())
    values = list(dim_counter.values())
    
    return {
        "labels": labels,
        "values": values,
        "dimension": dimension,
        "chart_type": chart_type
    }

def get_tab_data(tab_name: str):
    """获取指定 Tab 的展示数据（用于导出）"""
    tab_api_map = { "hello": "/api/hello", "hash": "/api/hash", "sort": "/api/sort" }
    api_name = tab_api_map.get(tab_name)
    if not api_name:
        return []
    return [r for r in _tracking_records if r["api"] == api_name]

def get_all_records():
    """获取所有埋点记录"""
    return list(_tracking_records)
```

- [ ] **Step 2: 在 app.py 中添加埋点路由**

```python
from flask import Flask, jsonify, request
from flask_cors import CORS
from hello_world import hello_world
from hash_algo import compute_hash
from sort_api import sort_data
from tracking import track_call, get_overview, get_chart_data, get_tab_data, get_all_records

app = Flask(__name__)
CORS(app)

@app.route('/api/hello', methods=['GET'])
def api_hello():
    caller = request.args.get('caller', 'anonymous')
    user_type = request.args.get('user_type', 'developer')
    user_level = request.args.get('user_level', 'mid')
    department = request.args.get('department', 'engineering')
    track_call('/api/hello', caller, user_type, user_level, department)
    result = hello_world()
    return jsonify(result)

@app.route('/api/hash', methods=['POST'])
def api_hash():
    data = request.get_json()
    if not data or 'input' not in data:
        return jsonify({"error": "Missing 'input' field"}), 400
    input_str = data['input']
    algorithm = data.get('algorithm', 'sha256')
    caller = data.get('caller', 'anonymous')
    user_type = data.get('user_type', 'developer')
    user_level = data.get('user_level', 'mid')
    department = data.get('department', 'engineering')
    try:
        result = compute_hash(input_str, algorithm)
        track_call('/api/hash', caller, user_type, user_level, department)
        return jsonify(result)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400

@app.route('/api/sort', methods=['POST'])
def api_sort():
    data = request.get_json()
    if not data or 'data' not in data:
        return jsonify({"error": "Missing 'data' field"}), 400
    if not isinstance(data['data'], list):
        return jsonify({"error": "'data' must be an array"}), 400
    caller = data.get('caller', 'anonymous')
    user_type = data.get('user_type', 'developer')
    user_level = data.get('user_level', 'mid')
    department = data.get('department', 'engineering')
    result = sort_data(data['data'])
    track_call('/api/sort', caller, user_type, user_level, department)
    return jsonify(result)

@app.route('/api/track', methods=['POST'])
def api_track():
    data = request.get_json()
    if not data:
        return jsonify({"error": "Missing request body"}), 400
    record = track_call(
        data.get('api', 'unknown'),
        data.get('caller', 'anonymous'),
        data.get('user_type', 'developer'),
        data.get('user_level', 'mid'),
        data.get('department', 'engineering')
    )
    return jsonify({"status": "ok", "id": record["id"]})

@app.route('/api/stats/overview', methods=['GET'])
def api_stats_overview():
    return jsonify(get_overview())

@app.route('/api/stats/chart', methods=['GET'])
def api_stats_chart():
    dimension = request.args.get('dimension', 'user_type')
    chart_type = request.args.get('chart_type', 'pie')
    try:
        result = get_chart_data(dimension, chart_type)
        return jsonify(result)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

- [ ] **Step 3: 验证埋点接口**

Run: `curl -X POST http://localhost:5000/api/track -H "Content-Type: application/json" -d '{"api":"/api/hello","caller":"zhangsan","user_type":"developer","user_level":"senior","department":"engineering"}'`
Expected: `{"status":"ok","id":"<uuid>"}`

Run: `curl http://localhost:5000/api/stats/overview`
Expected: `{"total_calls":1,"by_api":{"/api/hello":1},"by_user":{"zhangsan":1}}`

Run: `curl "http://localhost:5000/api/stats/chart?dimension=user_type&chart_type=pie"`
Expected: `{"labels":["developer"],"values":[1],"dimension":"user_type","chart_type":"pie"}`

---

## Task 5: 导出接口

**Files:**
- Create: `manyu_test/export.py`
- Modify: `manyu_test/app.py`（添加 /api/export 路由）

**Interfaces:**
- Produces: `GET /api/export?tab=hello` → CSV 文件下载（Content-Type: text/csv）

- [ ] **Step 1: 创建 export.py**

```python
import csv
import io
from tracking import get_all_records, get_tab_data

def export_to_csv(tab: str = None):
    """导出埋点数据为 CSV 格式"""
    if tab and tab in ("hello", "hash", "sort"):
        records = get_tab_data(tab)
    else:
        records = get_all_records()
    
    output = io.StringIO()
    if not records:
        return ""
    
    fieldnames = ["id", "timestamp", "api", "caller", "user_type", "user_level", "department"]
    writer = csv.DictWriter(output, fieldnames=fieldnames)
    writer.writeheader()
    for record in records:
        writer.writerow(record)
    
    return output.getvalue()
```

- [ ] **Step 2: 在 app.py 中添加导出路由**

```python
@app.route('/api/export', methods=['GET'])
def api_export():
    from flask import Response
    tab = request.args.get('tab')
    csv_data = export_to_csv(tab)
    if not csv_data:
        return jsonify({"error": "No data to export"}), 404
    filename = f"tracking_{tab or 'all'}.csv"
    return Response(
        csv_data,
        mimetype="text/csv",
        headers={"Content-Disposition": f"attachment; filename={filename}"}
    )
```

（在 app.py 顶部添加 `from export import export_to_csv`）

- [ ] **Step 3: 验证导出接口**

Run: `curl "http://localhost:5000/api/export?tab=hello" -o /tmp/export_test.csv`
Expected: 下载 CSV 文件，包含埋点数据表头和数据行

---

## Task 6: 前端页面 — index.html + style.css

**Files:**
- Create: `manyu_test1/index.html`
- Create: `manyu_test1/style.css`

**Interfaces:**
- Consumes: 后端 API（`/api/hello`, `/api/hash`, `/api/sort`, `/api/export`, `/api/stats/overview`, `/api/stats/chart`）
- Produces: 用户可交互的 Web 页面

- [ ] **Step 1: 创建 style.css**

```css
/* 页面基础样式 */
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f7fa; color: #333; padding: 20px; }

.container { max-width: 1200px; margin: 0 auto; background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); padding: 24px; }

/* 用户信息栏 */
.user-info { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.user-info label { font-weight: 600; }
.user-info input, .user-info select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }

/* Tab 导航 */
.tab-nav { display: flex; border-bottom: 2px solid #e0e0e0; margin-bottom: 20px; }
.tab-btn { padding: 12px 24px; cursor: pointer; background: none; border: none; font-size: 15px; color: #666; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.tab-btn:hover { color: #1890ff; }
.tab-btn.active { color: #1890ff; border-bottom-color: #1890ff; font-weight: 600; }

/* Tab 内容 */
.tab-content { display: none; padding: 16px 0; }
.tab-content.active { display: block; }

/* 操作区 */
.action-area { margin-bottom: 16px; }
.action-area label { display: block; margin-bottom: 6px; font-weight: 500; }
.action-area input, .action-area select, .action-area textarea { width: 100%; max-width: 400px; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
.action-area textarea { min-height: 80px; resize: vertical; }
.btn { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn:hover { background: #40a9ff; }
.btn:disabled { background: #ccc; cursor: not-allowed; }

/* 结果展示 */
.result-area { background: #fafafa; border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px; margin-top: 12px; min-height: 60px; white-space: pre-wrap; font-family: 'Courier New', monospace; font-size: 13px; }

/* 导出按钮区 */
.export-bar { display: flex; align-items: center; gap: 12px; margin: 20px 0; padding: 12px 0; border-top: 1px solid #e8e8e8; border-bottom: 1px solid #e8e8e8; }
.export-bar select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
.btn-export { background: #52c41a; }
.btn-export:hover { background: #73d13d; }

/* 图表区域 */
.chart-section { margin-top: 24px; }
.chart-section h3 { margin-bottom: 16px; }
.chart-controls { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
.chart-controls select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 12px; min-height: 320px; }
.chart-box h4 { text-align: center; margin-bottom: 8px; color: #555; }
.chart-canvas { width: 100%; height: 280px; }
```

- [ ] **Step 2: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>三接口演示平台</title>
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
</head>
<body>
    <div class="container">
        <h1>三接口演示平台</h1>

        <!-- 用户信息 -->
        <div class="user-info">
            <label>调用人：</label>
            <input type="text" id="callerName" value="zhangsan" placeholder="输入用户名">
            <label>人员类型：</label>
            <select id="userType">
                <option value="developer">开发者</option>
                <option value="manager">管理者</option>
                <option value="tester">测试人员</option>
                <option value="admin">管理员</option>
            </select>
            <label>人员层级：</label>
            <select id="userLevel">
                <option value="junior">初级</option>
                <option value="mid" selected>中级</option>
                <option value="senior">高级</option>
                <option value="principal">资深</option>
            </select>
            <label>部门：</label>
            <select id="department">
                <option value="engineering">研发部</option>
                <option value="product">产品部</option>
                <option value="qa">测试部</option>
                <option value="operations">运维部</option>
            </select>
        </div>

        <!-- Tab 导航 -->
        <div class="tab-nav">
            <button class="tab-btn active" data-tab="hello">HelloWorld</button>
            <button class="tab-btn" data-tab="hash">哈希算法</button>
            <button class="tab-btn" data-tab="sort">冒泡排序</button>
        </div>

        <!-- Tab: HelloWorld -->
        <div class="tab-content active" id="tab-hello">
            <div class="action-area">
                <button class="btn" onclick="callHello()">调用 HelloWorld 接口</button>
            </div>
            <div class="result-area" id="result-hello">点击按钮查看结果</div>
        </div>

        <!-- Tab: 哈希算法 -->
        <div class="tab-content" id="tab-hash">
            <div class="action-area">
                <label>输入文本：</label>
                <input type="text" id="hashInput" value="Hello World" placeholder="输入要哈希的文本">
                <label>算法选择：</label>
                <select id="hashAlgorithm">
                    <option value="sha256">SHA256</option>
                    <option value="md5">MD5</option>
                </select>
                <button class="btn" onclick="callHash()" style="margin-top:8px;">调用哈希接口</button>
            </div>
            <div class="result-area" id="result-hash">输入文本并选择算法后点击按钮</div>
        </div>

        <!-- Tab: 冒泡排序 -->
        <div class="tab-content" id="tab-sort">
            <div class="action-area">
                <label>输入数组（JSON格式，如 [5,3,8,4,2]）：</label>
                <input type="text" id="sortInput" value="[5,3,8,4,2]" placeholder="输入数组">
                <button class="btn" onclick="callSort()" style="margin-top:8px;">调用排序接口</button>
            </div>
            <div class="result-area" id="result-sort">输入数组后点击按钮</div>
        </div>

        <!-- 导出栏 -->
        <div class="export-bar">
            <label>导出数据：</label>
            <select id="exportTab">
                <option value="all">全部</option>
                <option value="hello">HelloWorld</option>
                <option value="hash">哈希算法</option>
                <option value="sort">冒泡排序</option>
            </select>
            <button class="btn btn-export" onclick="exportData()">导出 CSV</button>
        </div>

        <!-- 图表区域 -->
        <div class="chart-section">
            <h3>📊 调用统计报表</h3>
            <div class="chart-controls">
                <label>图表维度：</label>
                <select id="chartDimension">
                    <option value="user_type">人员类型</option>
                    <option value="user_level">人员层级</option>
                    <option value="department">人员部门</option>
                </select>
                <button class="btn" onclick="refreshCharts()">刷新图表</button>
            </div>
            <div class="chart-grid">
                <div class="chart-box">
                    <h4>折线图（趋势）</h4>
                    <div id="chart-line" class="chart-canvas"></div>
                </div>
                <div class="chart-box">
                    <h4>饼图（分布）</h4>
                    <div id="chart-pie" class="chart-canvas"></div>
                </div>
                <div class="chart-box">
                    <h4>柱状图（对比）</h4>
                    <div id="chart-bar" class="chart-canvas"></div>
                </div>
            </div>
        </div>
    </div>

    <script src="app.js"></script>
</body>
</html>
```

---

## Task 7: 前端 JavaScript 逻辑（app.js）

**Files:**
- Create: `manyu_test1/app.js`

**Interfaces:**
- Consumes: 所有后端 API 接口
- Produces: Tab 交互、API 调用、ECharts 图表渲染

- [ ] **Step 1: 创建 app.js**

```javascript
// 后端 API 基础地址
const API_BASE = 'http://localhost:5000';

// 获取用户信息
function getUserInfo() {
    return {
        caller: document.getElementById('callerName').value || 'anonymous',
        user_type: document.getElementById('userType').value,
        user_level: document.getElementById('userLevel').value,
        department: document.getElementById('department').value
    };
}

// Tab 切换
document.addEventListener('DOMContentLoaded', function() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            tabBtns.forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            document.getElementById('tab-' + this.dataset.tab).classList.add('active');
        });
    });
    // 初始加载图表
    setTimeout(initCharts, 500);
});

// ==================== HelloWorld ====================
async function callHello() {
    const userInfo = getUserInfo();
    const params = new URLSearchParams(userInfo);
    const resultDiv = document.getElementById('result-hello');
    resultDiv.textContent = '调用中...';
    try {
        const resp = await fetch(`${API_BASE}/api/hello?${params}`);
        const data = await resp.json();
        resultDiv.textContent = JSON.stringify(data, null, 2);
        refreshCharts();
    } catch (err) {
        resultDiv.textContent = '错误：' + err.message;
    }
}

// ==================== 哈希算法 ====================
async function callHash() {
    const userInfo = getUserInfo();
    const input = document.getElementById('hashInput').value;
    const algorithm = document.getElementById('hashAlgorithm').value;
    const resultDiv = document.getElementById('result-hash');
    resultDiv.textContent = '调用中...';
    try {
        const resp = await fetch(`${API_BASE}/api/hash`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ input, algorithm, ...userInfo })
        });
        const data = await resp.json();
        resultDiv.textContent = JSON.stringify(data, null, 2);
        refreshCharts();
    } catch (err) {
        resultDiv.textContent = '错误：' + err.message;
    }
}

// ==================== 冒泡排序 ====================
async function callSort() {
    const userInfo = getUserInfo();
    const inputStr = document.getElementById('sortInput').value;
    const resultDiv = document.getElementById('result-sort');
    resultDiv.textContent = '调用中...';
    try {
        let dataArr;
        try {
            dataArr = JSON.parse(inputStr);
            if (!Array.isArray(dataArr)) throw new Error('不是数组');
        } catch (e) {
            resultDiv.textContent = '错误：请输入有效的 JSON 数组，如 [5,3,8,4,2]';
            return;
        }
        const resp = await fetch(`${API_BASE}/api/sort`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ data: dataArr, ...userInfo })
        });
        const data = await resp.json();
        resultDiv.textContent = JSON.stringify(data, null, 2);
        refreshCharts();
    } catch (err) {
        resultDiv.textContent = '错误：' + err.message;
    }
}

// ==================== 导出 ====================
function exportData() {
    const tab = document.getElementById('exportTab').value;
    const url = tab === 'all'
        ? `${API_BASE}/api/export`
        : `${API_BASE}/api/export?tab=${tab}`;
    window.open(url, '_blank');
}

// ==================== 图表 ====================
let lineChart = null, pieChart = null, barChart = null;

function initCharts() {
    lineChart = echarts.init(document.getElementById('chart-line'));
    pieChart = echarts.init(document.getElementById('chart-pie'));
    barChart = echarts.init(document.getElementById('chart-bar'));
    refreshCharts();
}

async function refreshCharts() {
    const dimension = document.getElementById('chartDimension').value;
    try {
        // 三个图表并行请求，每个使用不同的 chart_type
        const [lineData, pieData, barData] = await Promise.all([
            fetch(`${API_BASE}/api/stats/chart?dimension=${dimension}&chart_type=line`).then(r => r.json()),
            fetch(`${API_BASE}/api/stats/chart?dimension=${dimension}&chart_type=pie`).then(r => r.json()),
            fetch(`${API_BASE}/api/stats/chart?dimension=${dimension}&chart_type=bar`).then(r => r.json())
        ]);

        // 折线图
        if (lineChart) {
            lineChart.setOption({
                title: { text: '调用趋势', textStyle: { fontSize: 13 } },
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: lineData.labels || [] },
                yAxis: { type: 'value' },
                series: [{ type: 'line', data: lineData.values || [], smooth: true, lineStyle: { width: 3 }, itemStyle: { color: '#1890ff' } }]
            });
        }

        // 饼图
        if (pieChart) {
            pieChart.setOption({
                title: { text: '分布情况', textStyle: { fontSize: 13 }, left: 'center' },
                tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
                series: [{
                    type: 'pie', radius: ['30%', '60%'], center: ['50%', '55%'],
                    data: (pieData.labels || []).map((label, i) => ({ name: label, value: (pieData.values || [])[i] })),
                    label: { formatter: '{b}\n{d}%' }
                }]
            });
        }

        // 柱状图
        if (barChart) {
            barChart.setOption({
                title: { text: '对比统计', textStyle: { fontSize: 13 } },
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: barData.labels || [] },
                yAxis: { type: 'value' },
                series: [{ type: 'bar', data: barData.values || [], itemStyle: { color: '#52c41a' } }]
            });
        }
    } catch (err) {
        console.error('图表刷新失败：', err);
    }
}
```

---

## Task 8: 集成测试与联调验证

**Files:**
- No new files. Run integration tests on the full stack.

- [ ] **Step 1: 启动后端服务**

Run: `cd manyu_test-cred-test-20260716022903 && python app.py &`
Expected: 服务启动在端口 5000

- [ ] **Step 2: 验证所有业务接口**

Run: 
```bash
# HelloWorld
curl http://localhost:5000/api/hello?caller=zhangsan&user_type=developer&user_level=senior&department=engineering
# 哈希
curl -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"input":"test","algorithm":"sha256","caller":"lisi","user_type":"tester","user_level":"mid","department":"qa"}'
# 冒泡排序
curl -X POST http://localhost:5000/api/sort -H "Content-Type: application/json" -d '{"data":[9,3,7,1,5],"caller":"wangwu","user_type":"manager","user_level":"senior","department":"product"}'
```
Expected: 每个接口返回正确 JSON 响应

- [ ] **Step 3: 验证统计接口**

Run:
```bash
curl http://localhost:5000/api/stats/overview
curl "http://localhost:5000/api/stats/chart?dimension=user_type&chart_type=pie"
curl "http://localhost:5000/api/stats/chart?dimension=user_level&chart_type=bar"
curl "http://localhost:5000/api/stats/chart?dimension=department&chart_type=line"
```
Expected: 返回包含上述调用数据的统计结果

- [ ] **Step 4: 验证导出接口**

Run: `curl "http://localhost:5000/api/export?tab=hello" -o /tmp/verify_export.csv && cat /tmp/verify_export.csv`
Expected: CSV 格式数据，包含埋点记录表头和数据行

- [ ] **Step 5: 前端页面验证**

Open `manyu_test1-main/index.html` in browser.
Expected: 页面正常渲染，三个 Tab 可切换，各接口调用正常，图表可刷新，导出按钮可下载 CSV

---

## 自检清单

### 1. 需求覆盖
| 需求 | 对应 Task | 覆盖 |
|------|-----------|------|
| HelloWorld 接口 | Task 1 | ✅ |
| 哈希算法接口（SHA256 + MD5） | Task 2 | ✅ |
| 冒泡排序接口（复用已有代码） | Task 3 | ✅ |
| 前端页面（3个Tab展示） | Task 6 + 7 | ✅ |
| 导出按钮 + 后台导出接口 | Task 5 | ✅ |
| 埋点统计（调用次数+调用人） | Task 4 | ✅ |
| 可视化报表（折线图/饼图/柱状图） | Task 7 | ✅ |
| 多维度统计（人员类型/层级/部门） | Task 4 + 7 | ✅ |

### 2. 占位符检查
- 无 TBD/TODO/implement later 等占位符 ✅
- 所有代码步骤包含完整代码 ✅
- 所有接口签名一致，跨任务引用类型匹配 ✅

### 3. 类型一致性
- `track_call()` 参数顺序在 Task 4 定义，Task 4 Step 2 的 app.py 调用一致 ✅
- `get_chart_data(dimension, chart_type)` 返回 `{labels, values, dimension, chart_type}`，前端 JS 解析一致 ✅
- `export_to_csv(tab)` 在 Task 5 定义，路由调用一致 ✅