# Task 1: 创建 hello_world 包（接口 + 实现）

**Files to create:**
- `hello_world/__init__.py`
- `hello_world/_interface.py`
- `hello_world/impl.py`
- `tests/test_hello_world.py`

## Requirements

### hello_world/__init__.py
```python
from hello_world._interface import HelloWorldInterface
from hello_world.impl import SimpleHelloWorld

__all__ = ["HelloWorldInterface", "SimpleHelloWorld"]
```

### hello_world/_interface.py
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

### hello_world/impl.py
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

### tests/test_hello_world.py
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

## Global Constraints
- Python 3.10+ 语法（`list[str]` 等泛型写法）
- 使用 `typing.Protocol`，不使用 ABC
- 所有接口方法必须包含完整的文档字符串和 doctest 示例
- 现有 `bubble_sort.py` 根目录文件不得修改
- 每个包必须包含 `__init__.py`
- 类型标注必须完整，通过 `mypy --strict` 检查