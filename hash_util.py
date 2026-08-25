#!/usr/bin/env python3
"""
Hash Utility — DJB2 String Hash Implementation

DJB2 哈希算法由 Daniel J. Bernstein 提出，是一种简单高效的
字符串哈希算法，广泛用于哈希表等场景。

算法特点：
- 简单：仅需乘法和异或操作
- 分布均匀：在大多数输入上产生良好的散列分布
- 确定性：相同输入始终产生相同输出

时间复杂度：O(n)，n 为字符串长度
空间复杂度：O(1)
"""


def hash_string(s: str) -> int:
    """
    DJB2 字符串哈希函数

    使用 DJB2 算法对输入字符串计算哈希值，返回 64 位无符号整数。

    Args:
        s: 待哈希的字符串

    Returns:
        64 位范围内的整数哈希值（0 到 2^64-1 之间）

    Examples:
        >>> hash_string("hello")
        210714636441
        >>> hash_string("")
        5381
        >>> hash_string("Hello, World!")
        5904905660241445518
        >>> hash_string("a") == hash_string("a")
        True
        >>> hash_string("a") != hash_string("b")
        True
    """
    # DJB2 初始值：5381
    hash_value = 5381

    for char in s:
        # hash = hash * 33 + ord(char)
        # 等价于 hash = ((hash << 5) + hash) + ord(char)
        hash_value = ((hash_value << 5) + hash_value) + ord(char)

    # 保持在 64 位无符号整数范围内
    return hash_value & 0xFFFFFFFFFFFFFFFF


def hash_string_32(s: str) -> int:
    """
    DJB2 字符串哈希函数（32 位版本）

    与 hash_string 算法相同，但将结果截断为 32 位无符号整数，
    适用于需要较小哈希值范围的场景。

    Args:
        s: 待哈希的字符串

    Returns:
        32 位范围内的整数哈希值（0 到 2^32-1 之间）

    Examples:
        >>> hash_string_32("hello")
        261238937
        >>> hash_string_32("")
        5381
        >>> hash_string_32("world")
        279393645
    """
    hash_value = 5381
    for char in s:
        hash_value = ((hash_value << 5) + hash_value) + ord(char)
    return hash_value & 0xFFFFFFFF


# ==================== 测试用例 ====================

if __name__ == "__main__":
    import doctest

    # 运行文档测试
    print("运行 doctest...")
    fail_count, test_count = doctest.testmod()
    print(f"doctest: {test_count} 个测试, {fail_count} 个失败")

    # 额外测试用例
    test_cases = [
        # (输入, 期望输出 — 由 64 位 DJB2 算法确定)
        ("hello", 210714636441),
        ("", 5381),
        ("Hello, World!", 5904905660241445518),
        ("test", 6385723493),
        ("abc", 193485963),
        ("123", 193432059),
        ("a" * 100, 2969978410614338601),
    ]

    all_passed = fail_count == 0
    for s, expected in test_cases:
        result = hash_string(s)
        if result != expected:
            print(f"FAIL: hash_string({s!r}) -> {result}, expected {expected}")
            all_passed = False

    # 验证确定性
    if hash_string("hello") != hash_string("hello"):
        print("FAIL: 确定性检查失败")
        all_passed = False

    # 验证不同输入产生不同哈希（碰撞概率极低）
    if hash_string("abc") == hash_string("xyz"):
        print("FAIL: 碰撞检查失败（极不可能）")
        all_passed = False

    if all_passed:
        print("所有测试用例通过！")
    else:
        print("部分测试用例失败！")
        exit(1)