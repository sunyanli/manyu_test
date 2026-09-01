# Task 2: 创建 hash_algo 包（接口 + SHA-256 实现）

**Files to create:**
- `hash_algo/__init__.py`
- `hash_algo/_interface.py`
- `hash_algo/sha256_impl.py`
- `tests/test_hash_algo.py`

## Requirements

### hash_algo/__init__.py
```python
from hash_algo._interface import HashAlgorithmInterface
from hash_algo.sha256_impl import SHA256Hasher

__all__ = ["HashAlgorithmInterface", "SHA256Hasher"]
```

### hash_algo/_interface.py
```python
"""哈希算法接口定义"""

from typing import Protocol


class HashAlgorithmInterface(Protocol):
    """哈希算法接口：提供数据哈希功能"""

    def hash(self, data: bytes) -> str:
        """计算数据的哈希值

        Args:
            data: 输入字节数据

        Returns:
            十六进制哈希字符串

        Examples:
            >>> SHA256Hasher().hash(b"hello")
            '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824'
        """
        ...

    def hash_file(self, filepath: str, chunk_size: int = 8192) -> str:
        """计算文件的哈希值

        Args:
            filepath: 文件路径
            chunk_size: 读取块大小（字节），默认 8192

        Returns:
            十六进制哈希字符串
        """
        ...

    @property
    def algorithm_name(self) -> str:
        """返回算法名称，如 'sha256', 'md5' 等"""
        ...
```

### hash_algo/sha256_impl.py
```python
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
```

### tests/test_hash_algo.py
```python
"""哈希算法单元测试"""

import doctest
import tempfile
import unittest

import hash_algo.sha256_impl
import hash_algo._interface


class TestHashAlgoDoctest(unittest.TestCase):
    def test_doctests(self):
        """运行模块的 doctest"""
        results = doctest.testmod(hash_algo.sha256_impl)
        self.assertEqual(results.failed, 0, f"doctest 失败: {results.failed}")


class TestSHA256Hasher(unittest.TestCase):
    def setUp(self):
        self.hasher = hash_algo.sha256_impl.SHA256Hasher()

    def test_algorithm_name(self):
        self.assertEqual(self.hasher.algorithm_name, "sha256")

    def test_hash_known_value(self):
        """验证已知的 SHA-256 哈希值"""
        result = self.hasher.hash(b"hello")
        self.assertEqual(
            result,
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        )

    def test_hash_empty_bytes(self):
        result = self.hasher.hash(b"")
        self.assertEqual(
            result,
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        )

    def test_hash_unicode(self):
        result = self.hasher.hash("你好".encode("utf-8"))
        # 验证结果为 64 位十六进制字符串（SHA-256 长度）
        self.assertEqual(len(result), 64)
        self.assertTrue(all(c in "0123456789abcdef" for c in result))

    def test_hash_file(self):
        with tempfile.NamedTemporaryFile(mode="wb", delete=False) as f:
            f.write(b"hello")
            f.flush()
            filepath = f.name

        try:
            result = self.hasher.hash_file(filepath)
            self.assertEqual(
                result,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            )
        finally:
            import os
            os.unlink(filepath)

    def test_hash_file_large_chunk(self):
        """测试大块读取是否正常工作"""
        content = b"a" * 20000  # 大于默认 chunk_size
        with tempfile.NamedTemporaryFile(mode="wb", delete=False) as f:
            f.write(content)
            f.flush()
            filepath = f.name

        try:
            # 使用大 chunk 和小 chunk 应该得到相同结果
            result_large = self.hasher.hash_file(filepath, chunk_size=16384)
            result_small = self.hasher.hash_file(filepath, chunk_size=1024)
            self.assertEqual(result_large, result_small)
        finally:
            import os
            os.unlink(filepath)

    def test_protocol_compatibility(self):
        """验证 SHA256Hasher 符合 HashAlgorithmInterface"""
        hasher: hash_algo._interface.HashAlgorithmInterface = self.hasher
        self.assertIsNotNone(hasher)


if __name__ == "__main__":
    unittest.main()
```

## Global Constraints
- Python 3.10+ 语法（`list[str]` 等泛型写法）
- 使用 `typing.Protocol`，不使用 ABC
- 所有接口方法必须包含完整的文档字符串和 doctest 示例
- 现有 `bubble_sort.py` 根目录文件不得修改
- 每个包必须包含 `__init__.py`
- 类型标注必须完整，通过 `mypy --strict` 检查