"""导出接口：CSV 格式导出调用记录"""

import csv
import io
from flask import Blueprint, request, Response
from middleware.tracking import tracking_store

export_bp = Blueprint("export", __name__)


@export_bp.route("/api/export", methods=["GET"])
def export_csv():
    export_type = request.args.get("type", "")

    type_path_map = {
        "helloworld": "/api/helloworld",
        "hash": "/api/hash",
        "bubblesort": "/api/bubblesort",
    }
    target_path = type_path_map.get(export_type, "")
    records = [r for r in tracking_store if r["endpoint"] == target_path] if target_path else tracking_store

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["姓名", "人员类型", "人员层级", "人员部门", "接口", "时间", "参数"])
    for r in records:
        writer.writerow([r["name"], r["type"], r["level"], r["dept"], r["endpoint"], r["timestamp"], r["params"]])

    csv_content = output.getvalue()
    output.close()

    return Response(
        csv_content,
        mimetype="text/csv",
        headers={"Content-Disposition": f"attachment; filename={export_type}_export.csv"}
    )