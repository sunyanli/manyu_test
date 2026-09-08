"""导出接口"""

import csv
import io
import json

from flask import Blueprint, request, jsonify, Response

from models import InvokeLog
from database import db

export_bp = Blueprint("export", __name__)

VALID_TABS = {"hello", "hash", "sort", "stats"}
VALID_FORMATS = {"csv", "json"}


def _query_logs_by_tab(tab):
    """根据 tab 查询对应的日志数据"""
    api_name_map = {
        "hello": "/api/hello",
        "hash": "/api/hash",
        "sort": "/api/sort/bubble",
    }
    if tab in api_name_map:
        api_name = api_name_map[tab]
        logs = InvokeLog.query.filter_by(api_name=api_name).order_by(InvokeLog.gmt_create.desc()).limit(1000).all()
        return [log.to_dict() for log in logs]
    elif tab == "stats":
        # 统计页导出所有日志
        logs = InvokeLog.query.order_by(InvokeLog.gmt_create.desc()).limit(1000).all()
        return [log.to_dict() for log in logs]
    return []


@export_bp.route("/api/export", methods=["GET"])
def export_data():
    """导出接口

    导出指定 Tab 页的数据，支持 CSV 和 JSON 格式
    """
    tab = request.args.get("tab", "")
    fmt = request.args.get("format", "csv").lower()

    if tab not in VALID_TABS:
        return jsonify({"code": 400, "msg": f"PARAM_ERROR: invalid tab '{tab}'", "data": None}), 400
    if fmt not in VALID_FORMATS:
        return jsonify({"code": 400, "msg": f"PARAM_ERROR: invalid format '{fmt}'", "data": None}), 400

    data = _query_logs_by_tab(tab)
    if not data:
        return jsonify({"code": 404, "msg": "NOT_FOUND: no data to export", "data": None}), 404

    if fmt == "json":
        return Response(
            json.dumps(data, ensure_ascii=False, indent=2),
            mimetype="application/json",
            headers={"Content-Disposition": f"attachment; filename={tab}_export.json"}
        )
    else:
        # CSV 格式
        if not data:
            return jsonify({"code": 404, "msg": "NOT_FOUND: no data to export", "data": None}), 404
        output = io.StringIO()
        writer = csv.DictWriter(output, fieldnames=data[0].keys())
        writer.writeheader()
        writer.writerows(data)
        csv_content = output.getvalue()
        return Response(
            csv_content,
            mimetype="text/csv",
            headers={"Content-Disposition": f"attachment; filename={tab}_export.csv"}
        )