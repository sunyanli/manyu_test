"""哈希算法接口"""

import hashlib

from flask import Blueprint, request, jsonify

hash_bp = Blueprint("hash", __name__)

SUPPORTED_ALGORITHMS = {"md5", "sha1", "sha256", "sha512"}


@hash_bp.route("/api/hash", methods=["POST"])
def hash_data():
    """哈希算法接口

    对输入数据进行哈希计算，支持 md5/sha1/sha256/sha512
    """
    body = request.get_json(silent=True)
    if not body:
        return jsonify({"code": 400, "msg": "PARAM_ERROR", "data": None}), 400

    data = body.get("data")
    if not data or not isinstance(data, str):
        return jsonify({"code": 400, "msg": "PARAM_ERROR: data is required", "data": None}), 400

    algorithm = body.get("algorithm", "sha256").lower()
    if algorithm not in SUPPORTED_ALGORITHMS:
        return jsonify({
            "code": 400,
            "msg": f"PARAM_ERROR: unsupported algorithm '{algorithm}', supported: {','.join(sorted(SUPPORTED_ALGORITHMS))}",
            "data": None
        }), 400

    try:
        h = hashlib.new(algorithm)
        h.update(data.encode("utf-8"))
        output = h.hexdigest()
    except Exception as e:
        return jsonify({"code": 500, "msg": f"SERVER_ERROR: {str(e)}", "data": None}), 500

    return jsonify({
        "code": 200,
        "msg": "SUCCESS",
        "data": {
            "algorithm": algorithm,
            "input": data,
            "output": output
        }
    })