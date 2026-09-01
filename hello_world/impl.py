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
        return f"Hello, {name}!"""

    def greet_many(self, names: list[str]) -> list[str]:
        return [self.greet(name) for name in names]