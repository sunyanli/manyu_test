"""埋点报表接口"""

from collections import Counter
from datetime import datetime
from flask import Blueprint, jsonify, request
from middleware.tracking import tracking_store

tracking_bp = Blueprint("tracking", __name__)


def _aggregate_by_dimension(records: list[dict], dimension: str) -> list[dict]:
    counter = Counter()
    for r in records:
        key = r.get(dimension, "unknown")
        counter[key] += 1
    return [{"key": k, "count": v} for k, v in counter.most_common()]


def _aggregate_by_time(records: list[dict]) -> list[dict]:
    """按小时聚合时间序列"""
    counter = Counter()
    for r in records:
        ts = r.get("timestamp", "")
        try:
            dt = datetime.fromisoformat(ts)
            hour_key = dt.strftime("%Y-%m-%dT%H:00:00")
        except (ValueError, TypeError):
            hour_key = "unknown"
        counter[hour_key] += 1
    return [{"time": k, "count": v} for k, v in sorted(counter.items())]


@tracking_bp.route("/api/tracking", methods=["GET"])
def tracking_report():
    dimension = request.args.get("dimension", "type")

    if dimension == "time":
        data = _aggregate_by_time(tracking_store)
    elif dimension in ("type", "level", "dept"):
        data = _aggregate_by_dimension(tracking_store, dimension)
    else:
        data = _aggregate_by_dimension(tracking_store, "type")
        dimension = "type"

    return jsonify({
        "dimension": dimension,
        "data": data,
        "summary": {"total": len(tracking_store)},
    })