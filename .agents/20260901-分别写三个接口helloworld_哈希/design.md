# 三个接口设计文档：HelloWorld、哈希算法、冒泡排序

- **日期**: 2026-09-01
- **状态**: 设计稿 v1

---

## 1. 概述

本项目为三个基础功能模块定义统一的 **Python 接口契约**，采用 `typing.Protocol`（PEP 544）实现结构性子类型（structural subtyping），遵循「鸭子类型」原则。现有 `bubble_sort.py` 实现将适配到新接口下。

## 2. 技术选型

| 项目 | 选择 |
|------|------|
| 语言 | Python 3.10+ |
| 接口方式 | `typing.Protocol`（结构性子类型） |
| 类型检查 | 兼容 mypy / pyright |
| 测试 | `doctest` + `pytest`（可选） |

### 为什么选择 Protocol 而非 ABC

- **松耦合**：实现类无需显式继承接口，降低依赖
- **现有兼容**：已有的 `bubble_sort.py` 函数可直接适配，无需修改继承关系
- **类型安全**：在静态类型检查下提供编译时验证
- **Pythonic**：符合 Python 的鸭子类型哲学

## 3. 接口定义

### 3.1 HelloWorldInterface

```python
"""hello_world/_interface.py"""

from typing import Protocol


class HelloWorldInterface(Protocol):
    """HelloWorld 接口：提供问候功能"""

    def greet(self, name: str) -> str:
        """生成问候语

        Args:
            name: 被问候者的名字

        Returns:
            问候语字符串

        Examples:
            >>> greet("World")
            'Hello, World!'
            >>> greet("Python")
            'Hello, Python!'
        """
        ...

    def greet_many(self, names: list[str]) -> list[str]:
        """批量生成问候语

        Args:
            names: 名字列表

        Returns:
            问候语列表
        """
        ...
```

### 3.2 HashAlgorithmInterface

```python
"""hash_algo/_interface.py"""

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
            >>> hash(b"hello")
            '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824'
        """
        ...

    def hash_file(self, filepath: str, chunk_size: int = 8192) -> str:
        """计算文件的哈希值

        Args:
            filepath: 文件路径
            chunk_size: 读取块大小（字节）

        Returns:
            十六进制哈希字符串
        """
        ...

    @property
    def algorithm_name(self) -> str:
        """返回算法名称，如 'sha256', 'md5' 等"""
        ...
```

### 3.3 SortInterface

```python
"""sorting/_interface.py"""

from typing import Protocol, TypeVar


T = TypeVar("T")


class SortInterface(Protocol):
    """排序算法接口：提供排序功能"""

    def sort(self, items: list[T]) -> list[T]:
        """对列表进行原地排序

        Args:
            items: 待排序列表

        Returns:
            排序后的列表（原地排序，同时返回引用）

        Examples:
            >>> sort([3, 1, 2])
            [1, 2, 3]
        """
        ...

    @property
    def algorithm_name(self) -> str:
        """返回算法名称，如 'bubble_sort', 'quick_sort' 等"""
        ...

    @property
    def time_complexity(self) -> str:
        """返回时间复杂度描述，如 'O(n²)'"""
        ...

    @property
    def space_complexity(self) -> str:
        """返回空间复杂度描述，如 'O(1)'"""
        ...
```

## 4. 目录结构

```
project/
├── hello_world/
│   ├── __init__.py
│   ├── _interface.py          # HelloWorldInterface Protocol
│   └── impl.py                # 实现（如 SimpleHelloWorld）
├── hash_algo/
│   ├── __init__.py
│   ├── _interface.py          # HashAlgorithmInterface Protocol
│   ├── sha256_impl.py         # SHA-256 实现
│   └── md5_impl.py            # MD5 实现（可选）
├── sorting/
│   ├── __init__.py
│   ├── _interface.py          # SortInterface Protocol
│   ├── bubble_sort.py         # 现有冒泡排序（适配接口）
│   └── quick_sort.py          # 快速排序（可选）
└── bubble_sort.py             # 保持现有文件不破坏兼容
```

## 5. 实现示例

### HelloWorld 实现

```python
# hello_world/impl.py

class SimpleHelloWorld:
    """HelloWorld 的简单实现"""

    def greet(self, name: str) -> str:
        return f"Hello, {name}!"

    def greet_many(self, names: list[str]) -> list[str]:
        return [self.greet(name) for name in names]
```

### 哈希算法实现

```python
# hash_algo/sha256_impl.py
import hashlib

class SHA256Hasher:
    """SHA-256 哈希实现"""

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

### 冒泡排序适配

```python
# sorting/bubble_sort.py
from typing import List, TypeVar

T = TypeVar("T")

class BubbleSorter:
    """冒泡排序实现，适配 SortInterface"""

    @property
    def algorithm_name(self) -> str:
        return "bubble_sort"

    @property
    def time_complexity(self) -> str:
        return "O(n²)"

    @property
    def space_complexity(self) -> str:
        return "O(1)"

    def sort(self, items: List[T]) -> List[T]:
        n = len(items)
        for i in range(n):
            for j in range(0, n - i - 1):
                if items[j] > items[j + 1]:
                    items[j], items[j + 1] = items[j + 1], items[j]
        return items
```

## 6. 使用方式（类型检查示例）

```python
# 通过 Protocol 实现静态类型检查
from hello_world._interface import HelloWorldInterface
from hello_world.impl import SimpleHelloWorld

def process_greeting(greeter: HelloWorldInterface, name: str) -> str:
    return greeter.greet(name)

# 无需显式继承，SimpleHelloWorld 自动满足 HelloWorldInterface
result = process_greeting(SimpleHelloWorld(), "World")
```

## 7. 与现有代码的兼容性

- 现有 `bubble_sort.py` 保持不变，**不破坏已有代码**
- 新增 `sorting/` 包提供新的面向接口的封装
- 现有 `bubble_sort()` 函数仍可直接调用

## 8. 验收标准

- [ ] 三个接口文件（`_interface.py`）定义完毕，使用 `typing.Protocol`
- [ ] 每个接口至少有一个实现类
- [ ] `mypy --strict` 通过，无类型错误
- [ ] 所有 `doctest` 示例通过
- [ ] 现有 `bubble_sort.py` 未被修改