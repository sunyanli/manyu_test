#!/usr/bin/env python3
"""
Hello World — 经典入门程序

最简单的 Python 程序，向世界问好。
"""


def hello() -> str:
    """
    返回经典的 "Hello, World!" 问候语。

    Returns:
        问候字符串

    Examples:
        >>> hello()
        'Hello, World!'
    """
    return "Hello, World!"


# ==================== 入口 ====================

if __name__ == "__main__":
    print(hello())