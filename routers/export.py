import json
from datetime import datetime, timezone
from typing import Any, Literal

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

router = APIRouter()


class ExportRequest(BaseModel):
    tab: Literal["helloworld", "hash", "bubble_sort"] = Field(..., description="来源 Tab")
    data: Any = Field(..., description="待导出的数据")


@router.post("/export")
def export_data(req: ExportRequest):
    """POST /api/export — 导出当前 Tab 结果为 JSON 文件下载"""
    payload = {
        "tab": req.tab,
        "data": req.data,
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    json_str = json.dumps(payload, ensure_ascii=False, indent=2)

    return StreamingResponse(
        iter([json_str]),
        media_type="application/json",
        headers={
            "Content-Disposition": f'attachment; filename="export_{req.tab}.json"'
        },
    )