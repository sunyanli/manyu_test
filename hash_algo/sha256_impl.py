"""SHA-256 哈希算法实现"""

import hashlib


class SHA256Hasher:
    """SHA-256 哈希实现

    符合 HashAlgorithmInterface Protocol。

    Examples:
        >>> hasher = SHA256Hasher()
        >>> hasher.algorithm_name
        'sha256'
        >>> hasher.hash(b"hello")
        '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824'
        >>> hasher.hash(b"")
        'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
    """

    @property
    def algorithm_name(self) -> str:
        return "sha256"

    def hash(self, data: bytes) -> str:
        return hashlib.sha256(data).hexdigest()

    def hash_file(self, filepath: str, chunk_size: int = 8192) -> str:
        h = hashlib.sha256()
        with open(filepath, "rb") as f:
            while chunk := f.read(chunk_size):
                h.update(chunk)
        return h.hexdigest()