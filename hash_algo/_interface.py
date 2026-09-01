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