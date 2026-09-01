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