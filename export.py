import csv
import io
from tracking import get_all_records, get_tab_data


def export_to_csv(tab: str = None):
    """导出埋点数据为 CSV 格式"""
    if tab and tab in ("hello", "hash", "sort"):
        records = get_tab_data(tab)
    else:
        records = get_all_records()

    output = io.StringIO()
    if not records:
        return ""

    fieldnames = ["id", "timestamp", "api", "caller", "user_type", "user_level", "department"]
    writer = csv.DictWriter(output, fieldnames=fieldnames)
    writer.writeheader()
    for record in records:
        writer.writerow(record)

    return output.getvalue()