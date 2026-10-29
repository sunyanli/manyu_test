package com.antdigital.todo.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TodoTextUtil} 文本规范化工具单元测试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoTextUtilTest {

    private static final String EMOJI = "\uD83D\uDE00";

    @Test
    @DisplayName("规范化剔除控制字符，保留换行与制表符")
    void should_keepNewlineAndTab_when_normalizeControlChars() {
        // Act
        String normalized = TodoTextUtil.normalize("a\u0000b\u001Fc\n\td");

        // Assert
        assertThat(normalized).isEqualTo("abc\n\td");
    }

    @Test
    @DisplayName("规范化将 null 视为空串")
    void should_returnEmpty_when_normalizeNull() {
        // Act & Assert
        assertThat(TodoTextUtil.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("去除首尾全角空格与不换行空格")
    void should_trimFullWidthSpace_when_trimWhitespace() {
        // Act & Assert
        assertThat(TodoTextUtil.trimWhitespace("　\u00A0 周报 \u3000")).isEqualTo("周报");
    }

    @Test
    @DisplayName("长度按字符计：emoji 计为 1 个字符")
    void should_countCodePoint_when_lengthOfEmoji() {
        // Act & Assert
        assertThat(TodoTextUtil.lengthOf(EMOJI + EMOJI)).isEqualTo(2);
    }

    @Test
    @DisplayName("长度统计对 null 返回 0")
    void should_returnZero_when_lengthOfNull() {
        // Act & Assert
        assertThat(TodoTextUtil.lengthOf(null)).isZero();
    }

    @Test
    @DisplayName("截断按字符切分，不破坏代理对")
    void should_truncateByCodePoint_when_textContainsEmoji() {
        // Act
        String truncated = TodoTextUtil.truncate(EMOJI + EMOJI + EMOJI, 2);

        // Assert
        assertThat(truncated).isEqualTo(EMOJI + EMOJI);
        assertThat(TodoTextUtil.lengthOf(truncated)).isEqualTo(2);
    }

    @Test
    @DisplayName("截断长度不足时不修改原文")
    void should_returnOriginal_when_textShorterThanLimit() {
        // Act & Assert
        assertThat(TodoTextUtil.truncate("周报", 10)).isEqualTo("周报");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "　"})
    @DisplayName("空白判定覆盖 null、空串与全角空格")
    void should_beBlank_when_textIsBlank(String text) {
        // Act & Assert
        assertThat(TodoTextUtil.isBlank(text)).isTrue();
    }

    @Test
    @DisplayName("非空白文本判定为不空白")
    void should_notBeBlank_when_textHasContent() {
        // Act & Assert
        assertThat(TodoTextUtil.isBlank(" 周报 ")).isFalse();
    }
}
