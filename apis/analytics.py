import sqlite3
from fastapi import APIRouter, HTTPException, Query
from models.tracking import get_db_path

VALID_DIMENSIONS = {"dept", "level", "user_type"}
VALID_API_NAMES = {"helloworld", "hash", "bubble-sort"}

router = APIRouter()


@router.get("/api/analytics")
async def analytics(
    dimension: str = Query(..., description="聚合维度: dept, level, user_type"),
    api_name: str | None = Query(None, description="可选，筛选特定接口"),
):
    if dimension not in VALID_DIMENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"非法维度: {dimension}，可选值: {', '.join(sorted(VALID_DIMENSIONS))}",
        )

    conn = sqlite3.connect(get_db_path())

    query = f"SELECT {dimension}, COUNT(*) as cnt FROM api_call_logs"
    params = []
    conditions = []

    if api_name:
        if api_name not in VALID_API_NAMES:
            conn.close()
            raise HTTPException(
                status_code=400,
                detail=f"非法接口名: {api_name}，可选值: {', '.join(sorted(VALID_API_NAMES))}",
            )
        conditions.append("api_name = ?")
        params.append(api_name)

    if conditions:
        query += " WHERE " + " AND ".join(conditions)

    query += f" GROUP BY {dimension} ORDER BY cnt DESC"

    cursor = conn.execute(query, params)
    rows = cursor.fetchall()
    conn.close()

    data = [
        {"label": row[0] if row[0] is not None else "(未设置)", "count": row[1]}
        for row in rows
    ]

    return {"dimension": dimension, "data": data}