"""埋点切面模块

使用 Flask 的 before_request / after_request 钩子实现 API 调用埋点。
记录每次 API 调用的调用人、时间、耗时、结果等。
"""

import time
import logging

from flask import request, g, current_app

from models import InvokeLog
from database import db

logger = logging.getLogger(__name__)

# 需要排除埋点的路径
EXCLUDE_PATHS = {"/api/stats", "/api/export"}


def register_tracking(app):
    """注册埋点钩子"""

    @app.before_request
    def before_request():
        """请求前：记录开始时间"""
        if _should_track(request.path):
            g._track_start_time = time.perf_counter()

    @app.after_request
    def after_request(response):
        """请求后：记录埋点"""
        if not _should_track(request.path):
            return response

        try:
            start_time = getattr(g, "_track_start_time", None)
            duration_ms = 0
            if start_time:
                duration_ms = int((time.perf_counter() - start_time) * 1000)

            # 从请求头中提取调用人信息
            caller_id = request.headers.get("X-User-Id", "anonymous")
            caller_name = request.headers.get("X-User-Name", "Unknown")
            caller_type = request.headers.get("X-User-Type", None)
            caller_level = request.headers.get("X-User-Level", None)
            caller_dept = request.headers.get("X-User-Dept", None)

            # 构建请求参数字符串（限制长度，避免过大的 body）
            params = None
            if request.data:
                data_str = request.data.decode("utf-8", errors="replace")
                params = data_str[:500] if len(data_str) > 500 else data_str
            elif request.args:
                params = str(dict(request.args))[:500]

            response_code = response.status_code
            result = "SUCCESS" if response_code < 400 else "FAIL"

            log = InvokeLog(
                api_name=request.path,
                caller_id=caller_id,
                caller_name=caller_name,
                caller_type=caller_type,
                caller_level=caller_level,
                caller_dept=caller_dept,
                request_params=params,
                response_code=response_code,
                result=result,
                duration_ms=duration_ms,
            )
            db.session.add(log)
            db.session.commit()
        except Exception as e:
            db.session.rollback()
            logger.error("Failed to write invoke log: %s", str(e))

        return response

    @app.teardown_request
    def teardown_request(exception=None):
        """请求结束时清理"""
        if exception:
            logger.warning("Request error: %s", str(exception))


def _should_track(path):
    """判断是否需要对该路径进行埋点"""
    if not current_app.config.get("ENABLE_TRACKING", True):
        return False
    if not path.startswith("/api/"):
        return False
    if path in EXCLUDE_PATHS:
        return False
    return True