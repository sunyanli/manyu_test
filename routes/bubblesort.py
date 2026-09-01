"""冒泡排序接口"""

from flask import Blueprint, jsonify, request
from bubble_sort import bubble_sort_optimized

bubblesort_bp = Blueprint("bubblesort", __name__)


@bubblesort_bp.route("/api/bubblesort", methods=["POST"])
def bubblesort_endpoint():
    data = request.get_json(silent=True) or {}
    arr = data.get("array")

    if not isinstance(arr, list):
        return jsonify({"error": "array must be a list of numbers"}), 400

    n = len(arr)
    sorted_arr = bubble_sort_optimized(arr.copy())

    return jsonify({
        "input": arr,
        "sorted": sorted_arr,
        "steps": n * (n - 1) // 2,
    })