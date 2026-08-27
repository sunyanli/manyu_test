#!/usr/bin/env python3
"""
三接口演示系统 — FastAPI 后端应用

提供核心接口（helloworld、哈希、冒泡排序）、埋点追踪、统计查询、导出功能。
"""

import csv
import hashlib
import io
import json
import os
import sqlite3
import uuid
from datetime import datetime, timezone
from typing import List, Optional

from fastapi import FastAPI, Query, Request
from fastapi.exceptions import HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel

from bubble_sort import bubble_sort

# ==================== 应用初始化 ====================

app = FastAPI(title="三接口演示服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==================== 数据库初始化 ====================

DB_PATH = "tracking.db"
EXPORT_DIR = "exports"


def init_db():
    os.makedirs(EXPORT_DIR, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS track_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_id TEXT UNIQUE,
            api_name TEXT,
            caller TEXT,
            person_type TEXT,
            person_level TEXT,
            person_department TEXT,
            timestamp TEXT
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS todos (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT,
            created_at TEXT
        )
    """)
    conn.commit()
    conn.close()


init_db()

# ==================== 请求模型 ====================


class BubbleSortRequest(BaseModel):
    array: List[float]


class TrackEventRequest(BaseModel):
    api_name: str
    caller: str = "anonymous"
    person_type: str = "unknown"
    person_level: str = "unknown"
    person_department: str = "unknown"


class CreateTodoRequest(BaseModel):
    name: str
    description: Optional[str] = None


# ==================== 辅助函数 ====================


def parse_caller_info(request: Request) -> dict:
    """从请求头解析调用人信息，用于埋点"""
    caller_header = request.headers.get("X-Caller-Info", "{}")
    try:
        return json.loads(caller_header)
    except json.JSONDecodeError:
        return {}


# ==================== 核心接口 ====================


@app.get("/api/helloworld")
async def helloworld(request: Request):
    try:
        return {
            "success": True,
            "data": {
                "message": "Hello World!",
                "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
            }
        }
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_HELLO_001",
            "message": "问候服务暂时不可用，请稍后重试",
            "detail": None
        })


@app.get("/api/hash")
async def hash_text(request: Request, text: str = Query(None, description="待计算哈希的文本")):
    if not text:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_HASH_001",
            "message": "请提供待计算哈希的文本内容",
            "detail": None
        })
    try:
        hash_value = hashlib.sha256(text.encode()).hexdigest()
        return {
            "success": True,
            "data": {
                "algorithm": "SHA256",
                "input": text,
                "hash": hash_value
            }
        }
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_HASH_002",
            "message": "哈希计算服务异常，请稍后重试",
            "detail": None
        })


@app.post("/api/bubble-sort")
async def bubble_sort_api(request: Request, body: BubbleSortRequest):
    if not body.array:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_SORT_001",
            "message": "请输入有效的数值数组进行排序",
            "detail": None
        })
    try:
        original = list(body.array)
        sorted_arr = bubble_sort(list(body.array))
        return {
            "success": True,
            "data": {
                "original": original,
                "sorted": sorted_arr
            }
        }
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_SORT_002",
            "message": "排序服务暂时不可用，请稍后重试",
            "detail": None
        })


# ==================== 埋点接口 ====================


@app.post("/api/track/event")
async def track_event(request: Request, event: TrackEventRequest):
    if not event.api_name:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_TRK_001",
            "message": "上报事件数据不完整，请检查后重试",
            "detail": None
        })
    try:
        event_id = str(uuid.uuid4())
        ts = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
        with sqlite3.connect(DB_PATH) as conn:
            conn.execute(
                "INSERT INTO track_events (event_id, api_name, caller, person_type, person_level, person_department, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (event_id, event.api_name, event.caller, event.person_type, event.person_level, event.person_department, ts)
            )
            conn.commit()
        return {"success": True, "data": {"event_id": event_id, "timestamp": ts}}
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_TRK_002",
            "message": "上报事件数据不完整，请检查后重试",
            "detail": None
        })


# ==================== 待办接口 ====================


@app.post("/api/todos", status_code=201)
async def create_todo(request: Request, body: CreateTodoRequest):
    if not body.name or len(body.name) > 100:
        raise HTTPException(
            status_code=400,
            detail={
                "success": False,
                "error_code": "ERR_TODO_001",
                "message": "待办名称不能为空或超过 100 字符",
                "detail": None,
            },
        )
    if body.description is not None and len(body.description) > 500:
        raise HTTPException(
            status_code=400,
            detail={
                "success": False,
                "error_code": "ERR_TODO_002",
                "message": "待办描述不能超过 500 字符",
                "detail": None,
            },
        )
    try:
        todo_id = str(uuid.uuid4())
        ts = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
        with sqlite3.connect(DB_PATH) as conn:
            conn.execute(
                "INSERT INTO todos (id, name, description, created_at) VALUES (?, ?, ?, ?)",
                (todo_id, body.name, body.description, ts),
            )
            conn.commit()
        return {
            "success": True,
            "data": {
                "id": todo_id,
                "name": body.name,
                "description": body.description,
                "created_at": ts,
            },
        }
    except Exception:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "error_code": "ERR_TODO_003",
                "message": "待办创建失败，请稍后重试",
                "detail": None,
            },
        )


# ==================== 统计接口 ====================


@app.get("/api/track/stats")
async def track_stats(request: Request, dimension: str = Query("type", description="聚合维度: type|level|department|time")):
    try:
        with sqlite3.connect(DB_PATH) as conn:
            conn.row_factory = sqlite3.Row
            dim_map = {
                "type": "person_type",
                "level": "person_level",
                "department": "person_department"
            }
            if dimension == "time":
                rows = conn.execute(
                    "SELECT DATE(timestamp) as date, COUNT(*) as count FROM track_events GROUP BY DATE(timestamp) ORDER BY date"
                ).fetchall()
                entries = [{"name": r["date"], "count": r["count"]} for r in rows]
            elif dimension in dim_map:
                col = dim_map[dimension]
                rows = conn.execute(
                    f"SELECT {col} as name, COUNT(*) as count FROM track_events GROUP BY {col} ORDER BY count DESC"
                ).fetchall()
                entries = [{"name": r["name"], "count": r["count"]} for r in rows]
            else:
                entries = []
        return {"success": True, "data": {"dimension": dimension, "entries": entries}}
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_TRK_002",
            "message": "统计数据获取失败，请稍后重试",
            "detail": None
        })


# ==================== 导出接口 ====================


@app.get("/api/export")
async def export_data(request: Request, tab: str = Query(None, description="导出页面: helloworld|hash|bubble-sort"), format: str = Query("csv")):
    if not tab:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_EXP_001",
            "message": "请指定要导出的页面类型",
            "detail": None
        })
    try:
        output = io.StringIO()
        writer = csv.writer(output)

        if tab == "helloworld":
            writer.writerow(["接口", "消息", "时间戳"])
            writer.writerow(["helloworld", "Hello World!", datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")])
        elif tab == "hash":
            writer.writerow(["接口", "算法", "输入", "哈希值"])
            writer.writerow(["hash", "SHA256", "示例文本", hashlib.sha256("示例文本".encode()).hexdigest()])
        elif tab == "bubble-sort":
            writer.writerow(["接口", "原始数组", "排序后数组"])
            writer.writerow(["bubble-sort", "[3, 1, 4, 1, 5]", str(bubble_sort([3, 1, 4, 1, 5]))])
        else:
            raise HTTPException(status_code=400, detail={
                "success": False,
                "error_code": "ERR_EXP_001",
                "message": "请指定要导出的页面类型",
                "detail": None
            })

        output.seek(0)
        filename = f"{tab}_export_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S')}.csv"
        return StreamingResponse(
            iter([output.getvalue()]),
            media_type="text/csv",
            headers={"Content-Disposition": f"attachment; filename={filename}"}
        )
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_EXP_003",
            "message": "导出文件生成失败，请稍后重试",
            "detail": None
        })


# ==================== 异常处理器 ====================


@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content=exc.detail if isinstance(exc.detail, dict) else {
            "success": False,
            "error_code": f"ERR_SYS_{exc.status_code}",
            "message": str(exc.detail),
            "detail": None
        }
    )


@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error_code": "ERR_SYS_500",
            "message": "系统异常，请稍后重试；如持续出现请联系管理员",
            "detail": None
        }
    )


# ==================== 入口 ====================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
