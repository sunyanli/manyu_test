package com.antdigital.todo.item.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TodoTextNormalizer} 单元测试。
 *
 * @author AiWork
 */
class TodoTextNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "'  整理材料  ', '整理材料'",
            "'a   b', 'a b'",
            "'　全角　空格　', '全角 空格'",
            "'正常文本', '正常文本'"
    })
    @DisplayName("规范化：裁剪首尾空白、全角空格归一、连续空白折叠")
    void should_normalizeText(String raw, String expected) {
        assertThat(TodoTextNormalizer.normalize(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("规范化：换行与制表符折叠为单个空格")
    void should_collapseLineFeedAndTab_toSingleSpace() {
        assertThat(TodoTextNormalizer.normalize("第一行\n第二行\t第三行")).isEqualTo("第一行 第二行 第三行");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "　"})
    @DisplayName("规范化：null 与纯空白均归为空串")
    void should_returnEmptyString_when_textIsBlank(String raw) {
        assertThat(TodoTextNormalizer.normalize(raw)).isEmpty();
    }

    @Test
    @DisplayName("非法字符检查：制表符与换行符允许")
    void should_allowTabAndLineFeed() {
        assertThat(TodoTextNormalizer.containsIllegalChar("a\tb\nc")).isFalse();
    }

    @Test
    @DisplayName("非法字符检查：NUL 字符被识别")
    void should_detectControlChar() {
        assertThat(TodoTextNormalizer.containsIllegalChar("a\u0000b")).isTrue();
    }

    @Test
    @DisplayName("字符计数：emoji 按 1 个字符计")
    void should_countEmojiAsOneChar() {
        String emoji = new String(Character.toChars(0x1F600));
        assertThat(TodoTextNormalizer.charLength(emoji)).isEqualTo(1);
        assertThat(emoji.length()).isEqualTo(2);
    }

    @Test
    @DisplayName("字符计数：null 与空串为 0")
    void should_returnZero_when_textIsEmpty() {
        assertThat(TodoTextNormalizer.charLength(null)).isZero();
        assertThat(TodoTextNormalizer.charLength("")).isZero();
    }
}
