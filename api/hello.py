"""HelloWorld 接口"""

from flask import Blueprint, request, jsonify

hello_bp = Blueprint("hello", __name__)


@hello_bp.route("/api/hello", methods=["GET"])
def hello():
    """HelloWorld 接口

    返回问候语，支持可选参数 name
    """
    name = request.args.get("name", "World")
    if not isinstance(name, str) or len(name) > 100:
        return jsonify({"code": 400, "msg": "PARAM_ERROR", "data": None}), 400

    return jsonify({
        "code": 200,
        "msg": "SUCCESS",
        "data": {"greeting": f"Hello, {name}!"}
    })