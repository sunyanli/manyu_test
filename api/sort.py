"""冒泡排序接口"""

import time

from flask import Blueprint, request, jsonify

sort_bp = Blueprint("sort", __name__)


def bubble_sort(arr, order="asc"):
    """执行冒泡排序，返回排序结果和交换次数"""
    n = len(arr)
    arr = list(arr)  # 复制一份，避免修改原数组
    swaps = 0
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if (order == "asc" and arr[j] > arr[j + 1]) or (order == "desc" and arr[j] < arr[j + 1]):
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
                swaps += 1
        if not swapped:
            break
    return arr, swaps


@sort_bp.route("/api/sort/bubble", methods=["POST"])
def bubble_sort_api():
    """冒泡排序接口

    对输入数组执行冒泡排序，支持升序/降序
    """
    body = request.get_json(silent=True)
    if not body:
        return jsonify({"code": 400, "msg": "PARAM_ERROR", "data": None}), 400

    arr = body.get("arr")
    if not isinstance(arr, list) or len(arr) == 0:
        return jsonify({"code": 400, "msg": "PARAM_ERROR: arr must be a non-empty array", "data": None}), 400

    if len(arr) > 10000:
        return jsonify({"code": 400, "msg": "PARAM_ERROR: array too large (max 10000)", "data": None}), 400

    # 确保所有元素都是数字
    try:
        arr = [float(x) if isinstance(x, (int, float)) else float(x) for x in arr]
    except (ValueError, TypeError):
        return jsonify({"code": 400, "msg": "PARAM_ERROR: all elements must be numbers", "data": None}), 400

    order = body.get("order", "asc")
    if order not in ("asc", "desc"):
        return jsonify({"code": 400, "msg": "PARAM_ERROR: order must be 'asc' or 'desc'", "data": None}), 400

    original = list(arr)
    start_time = time.perf_counter()
    sorted_arr, swaps = bubble_sort(arr, order)
    duration_ms = int((time.perf_counter() - start_time) * 1000)

    return jsonify({
        "code": 200,
        "msg": "SUCCESS",
        "data": {
            "original": original,
            "sorted": sorted_arr,
            "order": order,
            "swaps": swaps,
            "duration_ms": duration_ms
        }
    })