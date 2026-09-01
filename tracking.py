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
        "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
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
    tab_api_map = {"hello": "/api/hello", "hash": "/api/hash", "sort": "/api/sort"}
    api_name = tab_api_map.get(tab_name)
    if not api_name:
        return []
    return [r for r in _tracking_records if r["api"] == api_name]


def get_all_records():
    """获取所有埋点记录"""
    return list(_tracking_records)