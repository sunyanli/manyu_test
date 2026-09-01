"""HelloWorld 单元测试"""

import doctest
import unittest

import hello_world.impl
import hello_world._interface


class TestHelloWorldDoctest(unittest.TestCase):
    def test_doctests(self) -> None:
        """运行模块的 doctest"""
        results = doctest.testmod(hello_world.impl)
        self.assertEqual(results.failed, 0, f"doctest 失败: {results.failed}")


class TestSimpleHelloWorld(unittest.TestCase):
    def setUp(self) -> None:
        self.greeter = hello_world.impl.SimpleHelloWorld()

    def test_greet(self) -> None:
        self.assertEqual(self.greeter.greet("World"), "Hello, World!")
        self.assertEqual(self.greeter.greet("Python"), "Hello, Python!")
        self.assertEqual(self.greeter.greet(""), "Hello, !")

    def test_greet_many(self) -> None:
        result = self.greeter.greet_many(["Alice", "Bob", "Charlie"])
        self.assertEqual(result, [
            "Hello, Alice!",
            "Hello, Bob!",
            "Hello, Charlie!",
        ])

    def test_greet_many_empty(self) -> None:
        self.assertEqual(self.greeter.greet_many([]), [])

    def test_protocol_compatibility(self) -> None:
        """验证 SimpleHelloWorld 符合 HelloWorldInterface"""
        greeter: hello_world._interface.HelloWorldInterface = self.greeter
        self.assertIsNotNone(greeter)


if __name__ == "__main__":
    unittest.main()