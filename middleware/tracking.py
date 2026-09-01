"""埋点中间件：记录每次 API 调用的用户信息和接口详情"""

from datetime import datetime, timezone
from flask import Flask, request

# 全局内存存储
tracking_store: list[dict] = []


def init_tracking(app: Flask) -> None:
    """注册 before_request 钩子，自动记录 API 调用"""

    @app.before_request
    def record_tracking():
        # 只记录 /api/ 开头的请求，排除 /api/tracking 自身
        if not request.path.startswith("/api/"):
            return
        if request.path.startswith("/api/tracking"):
            return

        record = {
            "name": request.headers.get("X-User-Name", "anonymous"),
            "type": request.headers.get("X-User-Type", "unknown"),
            "level": request.headers.get("X-User-Level", "unknown"),
            "dept": request.headers.get("X-User-Dept", "unknown"),
            "endpoint": request.path,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "params": str(request.get_json(silent=True) or request.args.to_dict()),
        }
        tracking_store.append(record)