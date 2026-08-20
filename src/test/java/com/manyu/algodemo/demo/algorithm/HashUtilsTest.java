package com.manyu.algodemo.demo.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 哈希工具测试：覆盖 SHA-256/MD5/SM3 已知向量与非法算法。
 */
class HashUtilsTest {

    @Test
    @DisplayName("SHA-256 已知向量")
    void should_hashWithSha256() {
        String hash = HashUtils.hash("hello world", "SHA256");
        assertThat(hash).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }

    @Test
    @DisplayName("MD5 已知向量")
    void should_hashWithMd5() {
        String hash = HashUtils.hash("hello world", "MD5");
        assertThat(hash).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");
    }

    @Test
    @DisplayName("SM3 输出 64 位十六进制")
    void should_hashWithSm3() {
        String hash = HashUtils.hash("abc", "SM3");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("非法算法抛出异常")
    void should_throw_whenUnsupportedAlgorithm() {
        assertThatThrownBy(() -> HashUtils.hash("hello", "CRC32"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }
}
