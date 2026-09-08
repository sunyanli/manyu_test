"""统计报表接口"""

from datetime import datetime, timedelta

from flask import Blueprint, request, jsonify
from sqlalchemy import func

from models import InvokeLog
from database import db

stats_bp = Blueprint("stats", __name__)

VALID_DIMENSIONS = {"api_name", "caller_type", "caller_level", "caller_dept"}
VALID_CHART_TYPES = {"line", "pie", "bar"}
VALID_PERIODS = {"24h", "7d", "30d", "all"}

DIMENSION_COLUMN_MAP = {
    "api_name": InvokeLog.api_name,
    "caller_type": InvokeLog.caller_type,
    "caller_level": InvokeLog.caller_level,
    "caller_dept": InvokeLog.caller_dept,
}


def _get_period_filter(period):
    """根据 period 生成时间过滤条件"""
    now = datetime.utcnow()
    if period == "24h":
        return InvokeLog.gmt_create >= now - timedelta(hours=24)
    elif period == "7d":
        return InvokeLog.gmt_create >= now - timedelta(days=7)
    elif period == "30d":
        return InvokeLog.gmt_create >= now - timedelta(days=30)
    else:  # all
        return None


@stats_bp.route("/api/stats", methods=["GET"])
def get_stats():
    """统计报表接口

    获取调用统计报表数据，支持多维度过滤和图表类型
    """
    dimension = request.args.get("dimension", "api_name")
    period = request.args.get("period", "7d")
    chart_type = request.args.get("chart_type", "bar")

    if dimension not in VALID_DIMENSIONS:
        return jsonify({"code": 400, "msg": f"PARAM_ERROR: invalid dimension '{dimension}'", "data": None}), 400
    if chart_type not in VALID_CHART_TYPES:
        return jsonify({"code": 400, "msg": f"PARAM_ERROR: invalid chart_type '{chart_type}'", "data": None}), 400
    if period not in VALID_PERIODS:
        return jsonify({"code": 400, "msg": f"PARAM_ERROR: invalid period '{period}'", "data": None}), 400

    column = DIMENSION_COLUMN_MAP[dimension]
    period_filter = _get_period_filter(period)

    query = db.session.query(column, func.count(InvokeLog.id)).group_by(column)
    if period_filter is not None:
        query = query.filter(period_filter)

    rows = query.all()
    labels = [str(row[0]) if row[0] is not None else "未知" for row in rows]
    values = [row[1] for row in rows]
    total = sum(values)

    return jsonify({
        "code": 200,
        "msg": "SUCCESS",
        "data": {
            "dimension": dimension,
            "chart_type": chart_type,
            "labels": labels,
            "values": values,
            "total": total
        }
    })