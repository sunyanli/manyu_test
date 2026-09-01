import threading
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from models.tracking import insert_log, get_db_path

TRACKED_PATHS = {"/api/helloworld", "/api/hash", "/api/bubble-sort"}


class TrackingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)

        if request.url.path in TRACKED_PATHS:
            api_name = request.url.path.replace("/api/", "")
            caller_id = request.headers.get("X-User-Id")
            caller_name = request.headers.get("X-User-Name")
            dept = request.headers.get("X-User-Dept")
            level = request.headers.get("X-User-Level")
            user_type = request.headers.get("X-User-Type")

            threading.Thread(
                target=insert_log,
                args=(get_db_path(), api_name, caller_id, caller_name, dept, level, user_type),
                daemon=True,
            ).start()

        return response