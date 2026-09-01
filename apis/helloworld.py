from fastapi import APIRouter
from datetime import datetime, timezone

router = APIRouter()


@router.post("/api/helloworld")
async def helloworld():
    return {
        "message": "Hello, World!",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }