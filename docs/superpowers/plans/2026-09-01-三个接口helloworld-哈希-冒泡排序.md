# 三个接口（HelloWorld / 哈希算法 / 冒泡排序）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 HelloWorld、哈希算法、冒泡排序三个模块定义统一的 Python Protocol 接口，并提供至少一个实现类，同时保持现有 `bubble_sort.py` 不变。

**Architecture:** 基于 `typing.Protocol`（PEP 544）定义结构性子类型接口，实现类无需显式继承接口，实现松耦合。每个模块独立为包（`hello_world/`、`hash_algo/`、`sorting/`），包含 `_interface.py`（Protocol 定义）和对应的实现文件。

**Tech Stack:** Python 3.10+, `typing.Protocol`, `hashlib`（标准库）, `doctest`, `mypy`（类型检查）

---

## Global Constraints

- Python 3.10+ 语法（`list[str]` 等泛型写法）
- 使用 `typing.Protocol`，不使用 ABC
- 所有接口方法必须包含完整的文档字符串和 doctest 示例
- 现有 `bubble_sort.py` 根目录文件不得修改
- 每个包必须包含 `__init__.py`
- 类型标注必须完整，通过 `mypy --strict` 检查

---

## File Structure

```
project/
├── hello_world/
│   ├── __init__.py
│   ├── _interface.py              # HelloWorldInterface Protocol
│   └── impl.py                    # SimpleHelloWorld 实现
├── hash_algo/
│   ├── __init__.py
│   ├── _interface.py              # HashAlgorithmInterface Protocol
│   └── sha256_impl.py             # SHA256Hasher 实现
├── sorting/
│   ├── __init__.py
│   ├── _interface.py              # SortInterface Protocol
│   └── bubble_sort.py             # BubbleSorter 类（适配接口，新文件）
├── bubble_sort.py                 # 不变（根目录旧文件）
└── tests/
    ├── test_hello_world.py
    ├── test_hash_algo.py
    └── test_sorting.py
```

---

## Task 1: 创建 hello_world 包（接口 + 实现）

**Files:**
- Create: `hello_world/__init__.py`
- Create: `hello_world/_interface.py`
- Create: `hello_world/impl.py`
- Test: `tests/test_hello_world.py`

**Interfaces:**
- Produces: `HelloWorldInterface`（Protocol）— `greet(name: str) -> str` 和 `greet_many(names: list[str]) -> list[str]`
- Produces: `SimpleHelloWorld` 类（符合 `HelloWorldInterface`）

- [ ] **Step 1: 创建包目录和 `__init__.py`**

```bash
mkdir -p hello_world
```

```python
# hello_world/__init__.py
from hello_world._interface import HelloWorldInterface
from hello_world.impl import SimpleHelloWorld

__all__ = ["HelloWorldInterface", "SimpleHelloWorld"]
```

- [ ] **Step 2: 创建接口文件 `hello_world/_interface.py`**

```python
"""HelloWorld 接口定义"""

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
            >>> SimpleHelloWorld().greet("World")
            'Hello, World!'
            >>> SimpleHelloWorld().greet("Python")
            'Hello, Python!'
        """
        ...

    def greet_many(self, names: list[str]) -> list[str]:
        """批量生成问候语

        Args:
            names: 名字列表

        Returns:
            问候语列表

        Examples:
            >>> SimpleHelloWorld().greet_many(["Alice", "Bob"])
            ['Hello, Alice!', 'Hello, Bob!']
        """
        ...
```

- [ ] **Step 3: 创建实现文件 `hello_world/impl.py`**

```python
"""HelloWorld 接口实现"""

from hello_world._interface import HelloWorldInterface


class SimpleHelloWorld:
    """HelloWorld 的简单实现

    符合 HelloWorldInterface Protocol。

    Examples:
        >>> greeter = SimpleHelloWorld()
        >>> greeter.greet("World")
        'Hello, World!'
        >>> greeter.greet_many(["Alice", "Bob"])
        ['Hello, Alice!', 'Hello, Bob!']
    """

    def greet(self, name: str) -> str:
        return f"Hello, {name}!"

    def greet_many(self, names: list[str]) -> list[str]:
        return [self.greet(name) for name in names]
```

- [ ] **Step 4: 创建测试文件 `tests/test_hello_world.py`**

```python
"""HelloWorld 单元测试"""

import doctest
import unittest

import hello_world.impl
import hello_world._interface


class TestHelloWorldDoctest(unittest.TestCase):
    def test_doctests(self):
        """运行模块的 doctest"""
        results = doctest.testmod(hello_world.impl)
        self.assertEqual(results.failed, 0, f"doctest 失败: {results.failed}")


class TestSimpleHelloWorld(unittest.TestCase):
    def setUp(self):
        self.greeter = hello_world.impl.SimpleHelloWorld()

    def test_greet(self):
        self.assertEqual(self.greeter.greet("World"), "Hello, World!")
        self.assertEqual(self.greeter.greet("Python"), "Hello, Python!")
        self.assertEqual(self.greeter.greet(""), "Hello, !")

    def test_greet_many(self):
        result = self.greeter.greet_many(["Alice", "Bob", "Charlie"])
        self.assertEqual(result, [
            "Hello, Alice!",
            "Hello, Bob!",
            "Hello, Charlie!",
        ])

    def test_greet_many_empty(self):
        self.assertEqual(self.greeter.greet_many([]), [])

    def test_protocol_compatibility(self):
        """验证 SimpleHelloWorld 符合 HelloWorldInterface"""
        greeter: hello_world._interface.HelloWorldInterface = self.greeter
        self.assertIsNotNone(greeter)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 5: 运行测试验证**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
python -m pytest tests/test_hello_world.py -v
```

Expected: 4 tests passed

- [ ] **Step 6: Commit**

```bash
git add hello_world/ tests/test_hello_world.py
git commit -m "feat: add HelloWorld interface and SimpleHelloWorld implementation"
```

---

## Task 2: 创建 hash_algo 包（接口 + SHA-256 实现）

**Files:**
- Create: `hash_algo/__init__.py`
- Create: `hash_algo/_interface.py`
- Create: `hash_algo/sha256_impl.py`
- Test: `tests/test_hash_algo.py`

**Interfaces:**
- Consumes: 无（独立任务）
- Produces: `HashAlgorithmInterface`（Protocol）— `hash(data: bytes) -> str`, `hash_file(filepath: str, chunk_size: int = 8192) -> str`, `algorithm_name: str`（property）
- Produces: `SHA256Hasher` 类（符合 `HashAlgorithmInterface`）

- [ ] **Step 1: 创建包目录和 `__init__.py`**

```bash
mkdir -p hash_algo
```

```python
# hash_algo/__init__.py
from hash_algo._interface import HashAlgorithmInterface
from hash_algo.sha256_impl import SHA256Hasher

__all__ = ["HashAlgorithmInterface", "SHA256Hasher"]
```

- [ ] **Step 2: 创建接口文件 `hash_algo/_interface.py`**

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

- [ ] **Step 3: 创建 SHA-256 实现 `hash_algo/sha256_impl.py`**

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

- [ ] **Step 4: 创建测试文件 `tests/test_hash_algo.py`**

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

- [ ] **Step 5: 运行测试验证**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
python -m pytest tests/test_hash_algo.py -v
```

Expected: 7 tests passed

- [ ] **Step 6: Commit**

```bash
git add hash_algo/ tests/test_hash_algo.py
git commit -m "feat: add HashAlgorithm interface and SHA256Hasher implementation"
```

---

## Task 3: 创建 sorting 包（接口 + 冒泡排序类适配）

**Files:**
- Create: `sorting/__init__.py`
- Create: `sorting/_interface.py`
- Create: `sorting/bubble_sort.py`（新类，非根目录旧文件）
- Test: `tests/test_sorting.py`

**Interfaces:**
- Consumes: 无（独立任务）
- Produces: `SortInterface`（Protocol）— `sort(items: list[T]) -> list[T]`, `algorithm_name: str`, `time_complexity: str`, `space_complexity: str`
- Produces: `BubbleSorter` 类（符合 `SortInterface`）

- [ ] **Step 1: 创建包目录和 `__init__.py`**

```bash
mkdir -p sorting
```

```python
# sorting/__init__.py
from sorting._interface import SortInterface
from sorting.bubble_sort import BubbleSorter

__all__ = ["SortInterface", "BubbleSorter"]
```

- [ ] **Step 2: 创建接口文件 `sorting/_interface.py`**

```python
"""排序算法接口定义"""

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
            >>> BubbleSorter().sort([3, 1, 2])
            [1, 2, 3]
            >>> BubbleSorter().sort([5, 3, 8, 4, 2])
            [2, 3, 4, 5, 8]
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

- [ ] **Step 3: 创建冒泡排序类 `sorting/bubble_sort.py`**

```python
"""冒泡排序实现（面向接口版本）"""

from typing import TypeVar

T = TypeVar("T")


class BubbleSorter:
    """冒泡排序实现，适配 SortInterface

    标准冒泡排序算法，每次遍历将最大的元素"冒泡"到数组末尾。

    Examples:
        >>> sorter = BubbleSorter()
        >>> sorter.algorithm_name
        'bubble_sort'
        >>> sorter.time_complexity
        'O(n²)'
        >>> sorter.space_complexity
        'O(1)'
        >>> sorter.sort([3, 1, 2])
        [1, 2, 3]
        >>> sorter.sort([5, 3, 8, 4, 2])
        [2, 3, 4, 5, 8]
        >>> sorter.sort([])
        []
        >>> sorter.sort([42])
        [42]
    """

    @property
    def algorithm_name(self) -> str:
        return "bubble_sort"

    @property
    def time_complexity(self) -> str:
        return "O(n²)"

    @property
    def space_complexity(self) -> str:
        return "O(1)"

    def sort(self, items: list[T]) -> list[T]:
        n = len(items)
        for i in range(n):
            for j in range(0, n - i - 1):
                if items[j] > items[j + 1]:
                    items[j], items[j + 1] = items[j + 1], items[j]
        return items
```

- [ ] **Step 4: 创建测试文件 `tests/test_sorting.py`**

```python
"""排序算法单元测试"""

import doctest
import unittest

import sorting.bubble_sort
import sorting._interface


class TestSortingDoctest(unittest.TestCase):
    def test_doctests(self):
        """运行模块的 doctest"""
        results = doctest.testmod(sorting.bubble_sort)
        self.assertEqual(results.failed, 0, f"doctest 失败: {results.failed}")


class TestBubbleSorter(unittest.TestCase):
    def setUp(self):
        self.sorter = sorting.bubble_sort.BubbleSorter()

    def test_algorithm_name(self):
        self.assertEqual(self.sorter.algorithm_name, "bubble_sort")

    def test_time_complexity(self):
        self.assertEqual(self.sorter.time_complexity, "O(n²)")

    def test_space_complexity(self):
        self.assertEqual(self.sorter.space_complexity, "O(1)")

    def test_sort_unsorted(self):
        self.assertEqual(self.sorter.sort([3, 1, 2]), [1, 2, 3])

    def test_sort_already_sorted(self):
        self.assertEqual(self.sorter.sort([1, 2, 3, 4, 5]), [1, 2, 3, 4, 5])

    def test_sort_reverse_sorted(self):
        self.assertEqual(self.sorter.sort([5, 4, 3, 2, 1]), [1, 2, 3, 4, 5])

    def test_sort_empty(self):
        self.assertEqual(self.sorter.sort([]), [])

    def test_sort_single_element(self):
        self.assertEqual(self.sorter.sort([42]), [42])

    def test_sort_duplicates(self):
        self.assertEqual(self.sorter.sort([2, 2, 2, 2]), [2, 2, 2, 2])

    def test_sort_negative_numbers(self):
        self.assertEqual(
            self.sorter.sort([9, -3, 0, 7, -1]), [-3, -1, 0, 7, 9]
        )

    def test_sort_strings(self):
        self.assertEqual(
            self.sorter.sort(["banana", "apple", "cherry"]),
            ["apple", "banana", "cherry"],
        )

    def test_sort_returns_same_reference(self):
        """验证 sort 返回的是同一个列表引用（原地排序）"""
        items = [3, 1, 2]
        result = self.sorter.sort(items)
        self.assertIs(result, items)

    def test_protocol_compatibility(self):
        """验证 BubbleSorter 符合 SortInterface"""
        sorter: sorting._interface.SortInterface = self.sorter
        self.assertIsNotNone(sorter)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 5: 运行测试验证**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
python -m pytest tests/test_sorting.py -v
```

Expected: 11 tests passed

- [ ] **Step 6: 确认根目录 `bubble_sort.py` 未被修改**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
git diff bubble_sort.py
```

Expected: 无输出（文件未被修改）

- [ ] **Step 7: Commit**

```bash
git add sorting/ tests/test_sorting.py
git commit -m "feat: add SortInterface and BubbleSorter class adapter"
```

---

## Task 4: 类型检查与集成验证

**Files:**
- 无新文件创建，仅运行验证

- [ ] **Step 1: 安装 mypy（如未安装）**

```bash
pip install mypy
```

- [ ] **Step 2: 运行 mypy 严格模式类型检查**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
mypy --strict hello_world/ hash_algo/ sorting/ tests/
```

Expected: Success — no issues found

- [ ] **Step 3: 运行全部测试**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
python -m pytest tests/ -v
```

Expected: 22 tests passed (4 + 7 + 11)

- [ ] **Step 4: 验证根目录 `bubble_sort.py` 仍可独立运行**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-567b2203-d929-4c7e-ad55-e0df6cacb436/worktree
python bubble_sort.py
```

Expected: "运行 doctest..." 和 "所有测试用例通过！"

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "chore: add type checking and integration verification"
```

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] HelloWorld 接口定义（Task 1, Step 2）— `hello_world/_interface.py`
   - [x] 哈希算法接口定义（Task 2, Step 2）— `hash_algo/_interface.py`
   - [x] 冒泡排序接口定义（Task 3, Step 2）— `sorting/_interface.py`
   - [x] 每个接口至少一个实现类（Task 1-3, Step 3）
   - [x] 使用 `typing.Protocol`（所有接口文件）
   - [x] 现有 `bubble_sort.py` 未修改（Task 3, Step 6；Task 4, Step 4）
   - [x] 完整的 doctest 和单元测试
   - [x] mypy 严格模式检查

2. **Placeholder scan:** 无 "TBD"、"TODO"、"implement later" 等占位符。所有代码块包含完整实现。

3. **Type consistency:** 所有接口签名在接口定义、实现类和测试中保持一致。`SortInterface.sort` 始终使用 `list[T] -> list[T]`，`HashAlgorithmInterface.hash` 始终使用 `bytes -> str`。