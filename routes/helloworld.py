"""HelloWorld 接口"""

from flask import Blueprint, jsonify

helloworld_bp = Blueprint("helloworld", __name__)


@helloworld_bp.route("/api/helloworld", methods=["GET"])
def helloworld():
    return jsonify({"result": "Hello, World!"})