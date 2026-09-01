import csv
import io
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from models.tracking import get_db_path
import sqlite3

VALID_EXPORT_TYPES = {"helloworld", "hash", "bubble-sort"}

router = APIRouter()


@router.get("/api/export/{type}")
async def export_csv(type: str):
    if type not in VALID_EXPORT_TYPES:
        raise HTTPException(status_code=400, detail=f"非法导出类型: {type}，可选值: {', '.join(sorted(VALID_EXPORT_TYPES))}")

    conn = sqlite3.connect(get_db_path())
    cursor = conn.execute(
        "SELECT caller_name, dept, level, user_type, api_name, called_at FROM api_call_logs WHERE api_name = ? ORDER BY called_at DESC",
        (type,),
    )
    rows = cursor.fetchall()
    conn.close()

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["caller_name", "dept", "level", "user_type", "api_name", "timestamp"])
    for row in rows:
        writer.writerow(row)

    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": f'attachment; filename="{type}_export.csv"'},
    )