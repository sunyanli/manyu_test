import hashlib


def compute_hash(input_str: str, algorithm: str = "sha256"):
    """计算哈希值，支持 sha256 和 md5"""
    if algorithm not in ("sha256", "md5"):
        raise ValueError(f"Unsupported algorithm: {algorithm}")

    if algorithm == "sha256":
        hash_obj = hashlib.sha256(input_str.encode('utf-8'))
    else:
        hash_obj = hashlib.md5(input_str.encode('utf-8'))

    return {
        "input": input_str,
        "algorithm": algorithm,
        "hash": hash_obj.hexdigest()
    }