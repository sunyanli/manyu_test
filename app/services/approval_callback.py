"""审批系统 HTTP 回调 — 异步通知，失败不阻塞主流程。"""

import logging
from datetime import datetime, timezone

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


async def notify_approval_system(
    employee_id: int,
    old_dept_id: int,
    new_dept_id: int,
    new_position: str,
) -> None:
    """异步通知审批系统刷新审批节点。"""
    if not settings.approval_service_url:
        logger.info("未配置审批系统回调 URL，跳过通知")
        return

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                f"{settings.approval_service_url}/api/approval/nodes/refresh",
                json={
                    "employeeId": employee_id,
                    "oldDeptId": old_dept_id,
                    "newDeptId": new_dept_id,
                    "newPosition": new_position,
                    "triggerTime": datetime.now(timezone.utc).isoformat(),
                },
            )
            if resp.status_code >= 400:
                logger.warning(
                    "审批系统回调失败: status=%s, body=%s", resp.status_code, resp.text
                )
    except Exception as exc:
        logger.error("审批系统回调异常: %s", exc)