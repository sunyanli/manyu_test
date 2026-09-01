# manyu_test 后端项目 Review Profile

## 项目描述
Python Flask 后端 API 服务，提供三个业务接口（HelloWorld、哈希算法、冒泡排序）、埋点统计、数据导出功能。

## 技术栈
- Python 3.8+ / Flask 2.0+
- flask-cors 跨域支持
- 内存存储（list/dict）

## 项目特定检查门禁
1. **API 接口规范**：所有接口返回统一 `{code, msg, data}` 包裹格式
2. **错误码规范**：使用 `{MODULE}_{SEQ3}` 格式错误码
3. **埋点完整性**：每个业务接口调用后必须调用 `track_call()` 记录埋点
4. **模块分离**：各业务逻辑与路由分离，路由在 app.py，业务逻辑在独立模块
5. **冒泡排序复用**：必须复用 `bubble_sort.py` 中的 `bubble_sort` 函数
6. **哈希算法**：支持 SHA256 和 MD5 双算法
7. **导出格式**：CSV 格式，表头字段为 `id,timestamp,api,caller,user_type,user_level,department`