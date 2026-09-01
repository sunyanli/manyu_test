"""哈希算法接口"""

import hashlib
from flask import Blueprint, jsonify, request

hash_bp = Blueprint("hash", __name__)

SUPPORTED_ALGORITHMS = {"sha256", "md5", "sha1"}


def compute_hash(algorithm: str, text: str) -> str:
    h = hashlib.new(algorithm)
    h.update(text.encode("utf-8"))
    return h.hexdigest()


@hash_bp.route("/api/hash", methods=["POST"])
def hash_endpoint():
    data = request.get_json(silent=True) or {}
    algorithm = data.get("algorithm", "").lower()
    text = data.get("text", "")

    if algorithm not in SUPPORTED_ALGORITHMS:
        return jsonify({"error": f"Unsupported algorithm: {algorithm}. Supported: {', '.join(sorted(SUPPORTED_ALGORITHMS))}"}), 400

    return jsonify({
        "algorithm": algorithm,
        "input": text,
        "hash": compute_hash(algorithm, text),
    })