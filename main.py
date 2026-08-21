"""
FastAPI 应用入口 — 三接口 API 服务
"""
import hashlib
import io
import json
import time
from datetime import datetime, timezone
from typing import List, Union

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, Field

from bubble_sort import bubble_sort
from tracking import init_db, TrackingMiddleware, get_report

app = FastAPI(title="算法演示平台 API", version="1.0.0")

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册埋点中间件（必须在 CORS 之后、业务路由之前）
app.add_middleware(TrackingMiddleware)

SUPPORTED_ALGORITHMS = {"sha256", "md5", "sha1"}
MAX_INPUT_LENGTH = 1_048_576       # 1 MB
MAX_ARRAY_LENGTH = 10_000


class HashRequest(BaseModel):
    input: str
    algorithm: str = "sha256"


class BubbleSortRequest(BaseModel):
    array: List[Union[int, float]] = Field(..., min_length=0, max_length=MAX_ARRAY_LENGTH)


@app.on_event("startup")
async def startup():
    init_db()


# ==================== 业务 API ====================

@app.get("/api/helloworld")
async def helloworld():
    return {"message": "Hello, World!"}


@app.post("/api/hash")
async def compute_hash(req: HashRequest):
    if req.algorithm not in SUPPORTED_ALGORITHMS:
        return JSONResponse(
            status_code=400,
            content={
                "error": f"unsupported algorithm: {req.algorithm}",
                "supported": sorted(SUPPORTED_ALGORITHMS),
            },
        )
    if len(req.input) > MAX_INPUT_LENGTH:
        return JSONResponse(
            status_code=413,
            content={
                "error": "payload too large",
                "limit": MAX_INPUT_LENGTH,
            },
        )
    h = hashlib.new(req.algorithm)
    h.update(req.input.encode("utf-8"))
    return {"hash": h.hexdigest(), "algorithm": req.algorithm}


@app.post("/api/bubble_sort")
async def sort_bubble(req: BubbleSortRequest):
    arr_copy = list(req.array)
    n = len(arr_copy)
    steps = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            steps += 1
            if arr_copy[j] > arr_copy[j + 1]:
                arr_copy[j], arr_copy[j + 1] = arr_copy[j + 1], arr_copy[j]
    return {"sorted": arr_copy, "steps": steps}


# ==================== 埋点报表 API ====================

@app.get("/api/tracking/report")
async def tracking_report(dimension: str = "user_type"):
    result = get_report(dimension)
    if "error" in result:
        return JSONResponse(status_code=503, content=result)
    return result


# ==================== 导出 API ====================

@app.get("/api/export/helloworld")
async def export_helloworld():
    data = {
        "message": "Hello, World!",
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=helloworld_export.json"},
    )


@app.get("/api/export/hash")
async def export_hash(input: str, algorithm: str = "sha256"):
    if algorithm not in SUPPORTED_ALGORITHMS:
        return JSONResponse(
            status_code=400,
            content={
                "error": f"unsupported algorithm: {algorithm}",
                "supported": sorted(SUPPORTED_ALGORITHMS),
            },
        )
    h = hashlib.new(algorithm)
    h.update(input.encode("utf-8"))
    data = {
        "input": input,
        "algorithm": algorithm,
        "hash": h.hexdigest(),
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=hash_export.json"},
    )


@app.get("/api/export/bubble_sort")
async def export_bubble_sort(array: str):
    try:
        arr = [float(x.strip()) for x in array.split(",") if x.strip()]
    except ValueError:
        return JSONResponse(
            status_code=422,
            content={"error": "invalid array format, use comma-separated numbers"},
        )
    if len(arr) > MAX_ARRAY_LENGTH:
        return JSONResponse(
            status_code=413,
            content={"error": "payload too large", "limit": MAX_ARRAY_LENGTH},
        )
    arr_copy = list(arr)
    n = len(arr_copy)
    steps = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            steps += 1
            if arr_copy[j] > arr_copy[j + 1]:
                arr_copy[j], arr_copy[j + 1] = arr_copy[j + 1], arr_copy[j]
    data = {
        "original": arr,
        "sorted": arr_copy,
        "steps": steps,
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=bubble_sort_export.json"},
    )


# ==================== 全局异常处理器 ====================

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    import uuid
    request_id = str(uuid.uuid4())[:8]
    return JSONResponse(
        status_code=500,
        content={
            "error": "internal server error",
            "request_id": request_id,
        },
    )