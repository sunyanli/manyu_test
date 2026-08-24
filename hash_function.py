#!/usr/bin/env python3
"""
Hash Function Implementation

提供多种简单字符串哈希函数的实现，适用于哈希表、数据分片等场景。

支持的算法：
- DJB2：Daniel J. Bernstein 提出的经典字符串哈希，简单高效，分布均匀
- DJB2a：DJB2 的变体，使用 XOR 替代加法，进一步改善分布
- FNV-1a：Fowler-Noll-Vo 哈希函数，广泛用于哈希表实现

注意：这些是**非密码学**哈希函数，不适用于安全场景。
"""

from typing import Optional


def djb2_hash(s: str) -> int:
    """
    DJB2 哈希函数

    Daniel J. Bernstein 提出的字符串哈希算法。
    使用 hash * 33 + c 的递推公式，生成 64 位无符号整数哈希值。

    Args:
        s: 输入字符串

    Returns:
        64 位无符号整数哈希值

    Examples:
        >>> djb2_hash("")
        5381
        >>> djb2_hash("hello") == djb2_hash("hello")
        True
        >>> djb2_hash("abc") != djb2_hash("cba")
        True
        >>> isinstance(djb2_hash("test"), int)
        True
        >>> djb2_hash("a")  # 5381 * 33 + 97
        177670
    """
    h: int = 5381
    for c in s:
        # hash = hash * 33 + ord(c)
        # 使用左移 5 位加自身实现乘以 33 的优化
        h = ((h << 5) + h) + ord(c)
    # 确保返回 64 位无符号整数
    return h & 0xFFFFFFFFFFFFFFFF


def djb2a_hash(s: str) -> int:
    """
    DJB2a 哈希函数（DJB2 的 XOR 变体）

    使用 XOR 替代加法，进一步改善哈希分布。

    Args:
        s: 输入字符串

    Returns:
        64 位无符号整数哈希值

    Examples:
        >>> djb2a_hash("")
        5381
        >>> djb2a_hash("hello") == djb2a_hash("hello")
        True
        >>> djb2a_hash("abc") != djb2a_hash("cba")
        True
        >>> isinstance(djb2a_hash("test"), int)
        True
    """
    h: int = 5381
    for c in s:
        # hash = hash * 33 ^ ord(c)
        h = ((h << 5) + h) ^ ord(c)
    return h & 0xFFFFFFFFFFFFFFFF


def fnv1a_hash(s: str) -> int:
    """
    FNV-1a 哈希函数

    Fowler-Noll-Vo 哈希算法，以良好的分布性和快速计算著称。
    使用 XOR 后乘质数的策略。

    Args:
        s: 输入字符串

    Returns:
        64 位无符号整数哈希值

    Examples:
        >>> fnv1a_hash("")
        14695981039346656037
        >>> fnv1a_hash("hello") == fnv1a_hash("hello")
        True
        >>> fnv1a_hash("abc") != fnv1a_hash("cba")
        True
        >>> isinstance(fnv1a_hash("test"), int)
        True
    """
    # FNV-1a 64 位偏移基数和质数
    FNV_OFFSET_BASIS: int = 14695981039346656037
    FNV_PRIME: int = 1099511628211

    h: int = FNV_OFFSET_BASIS
    for c in s:
        h = h ^ ord(c)
        h = (h * FNV_PRIME) & 0xFFFFFFFFFFFFFFFF
    return h


def hash_string(s: str, algorithm: str = "djb2") -> int:
    """
    统一的字符串哈希接口

    Args:
        s: 输入字符串
        algorithm: 哈希算法名称，支持 "djb2"、"djb2a"、"fnv1a"，默认为 "djb2"

    Returns:
        64 位无符号整数哈希值

    Raises:
        ValueError: 不支持的算法名称

    Examples:
        >>> hash_string("hello", "djb2") == djb2_hash("hello")
        True
        >>> hash_string("hello", "fnv1a") == fnv1a_hash("hello")
        True
        >>> hash_string("hello", "djb2a") == djb2a_hash("hello")
        True
        >>> hash_string("test", "unknown")
        Traceback (most recent call last):
            ...
        ValueError: 不支持的哈希算法: unknown
    """
    algo_map = {
        "djb2": djb2_hash,
        "djb2a": djb2a_hash,
        "fnv1a": fnv1a_hash,
    }
    func = algo_map.get(algorithm)
    if func is None:
        raise ValueError(f"不支持的哈希算法: {algorithm}")
    return func(s)


# ==================== 测试用例 ====================

if __name__ == "__main__":
    import doctest

    # 运行文档测试
    print("运行 doctest...")
    doctest_results = doctest.testmod()
    if doctest_results.failed > 0:
        print(f"doctest 失败 {doctest_results.failed} 个！")
        exit(1)
    print("doctest 全部通过！")

    # 额外测试用例
    all_passed = True

    # 1. 确定性测试：相同输入必须产生相同输出
    test_inputs = ["", "a", "hello", "你好", "Hello, World!", "abc123", " " * 10, "a" * 100]
    for algo_name in ("djb2", "djb2a", "fnv1a"):
        for s in test_inputs:
            h1 = hash_string(s, algo_name)
            h2 = hash_string(s, algo_name)
            if h1 != h2:
                print(f"FAIL [确定性 {algo_name}]: '{s}' -> {h1} != {h2}")
                all_passed = False

    # 2. 不同输入通常产生不同哈希值（碰撞极小概率，但允许）
    # 只检查明显不同的输入
    distinct_inputs = ["a", "b", "c", "abc", "cba", "ABC", "你好", "好你"]
    for algo_name in ("djb2", "djb2a", "fnv1a"):
        hashes = set()
        for s in distinct_inputs:
            hashes.add(hash_string(s, algo_name))
        if len(hashes) < len(distinct_inputs):
            # 这里只做警告，因为碰撞在理论上是可能的
            print(f"WARN [碰撞 {algo_name}]: 在 {distinct_inputs} 中检测到哈希碰撞")

    # 3. 空字符串测试
    for algo_name in ("djb2", "djb2a", "fnv1a"):
        h = hash_string("", algo_name)
        if not isinstance(h, int):
            print(f"FAIL [类型 {algo_name}]: 空字符串哈希值类型应为 int，得到 {type(h)}")
            all_passed = False

    # 4. 哈希值范围测试（应为 64 位无符号整数）
    for algo_name in ("djb2", "djb2a", "fnv1a"):
        h = hash_string("test", algo_name)
        if not (0 <= h <= 0xFFFFFFFFFFFFFFFF):
            print(f"FAIL [范围 {algo_name}]: 哈希值 {h} 超出 64 位无符号整数范围")
            all_passed = False

    # 5. 统一接口 vs 独立函数一致性
    for s in test_inputs:
        if djb2_hash(s) != hash_string(s, "djb2"):
            print(f"FAIL [接口一致性 djb2]: '{s}'")
            all_passed = False
        if djb2a_hash(s) != hash_string(s, "djb2a"):
            print(f"FAIL [接口一致性 djb2a]: '{s}'")
            all_passed = False
        if fnv1a_hash(s) != hash_string(s, "fnv1a"):
            print(f"FAIL [接口一致性 fnv1a]: '{s}'")
            all_passed = False

    # 6. 异常测试
    try:
        hash_string("test", "unknown")
        print("FAIL [异常]: 预期 ValueError 未抛出")
        all_passed = False
    except ValueError:
        pass

    if all_passed:
        print("所有测试用例通过！")
    else:
        print("部分测试用例失败！")
        exit(1)