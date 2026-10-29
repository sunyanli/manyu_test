package com.antdigital.todo.service.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoNoGeneratorImpl} 与 {@link SnowflakeSequenceProvider} 单元测试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoNoGeneratorImplTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 10, 29);

    @Test
    @DisplayName("编号格式：TD + yyyyMMdd + 6 位序号")
    void should_generateTodoNo_when_sequenceGiven() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(dateKey -> 42L, () -> FIXED_DATE);

        // Act
        String todoNo = generator.nextTodoNo();

        // Assert
        assertThat(todoNo).isEqualTo("TD20261029000042");
        assertThat(todoNo).hasSize(16);
    }

    @Test
    @DisplayName("序号不足 6 位时左补零")
    void should_padSequence_when_sequenceIsSmall() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(dateKey -> 1L, () -> FIXED_DATE);

        // Act
        String todoNo = generator.nextTodoNo();

        // Assert
        assertThat(todoNo).isEqualTo("TD20261029000001");
    }

    @Test
    @DisplayName("序号超过 6 位时按 6 位取模，回绕后仍为定长")
    void should_wrapSequence_when_sequenceExceedsSixDigits() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(dateKey -> 1_000_001L, () -> FIXED_DATE);

        // Act
        String todoNo = generator.nextTodoNo();

        // Assert
        assertThat(todoNo).isEqualTo("TD20261029000001");
    }

    @Test
    @DisplayName("生产构造使用雪花取号，连续生成不重复")
    void should_generateDistinctTodoNo_when_useSnowflakeProvider() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(new SnowflakeSequenceProvider(1L, 1L));

        // Act
        String first = generator.nextTodoNo();
        String second = generator.nextTodoNo();

        // Assert
        assertThat(first).hasSize(16).startsWith("TD");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("雪花节点参数越界时快速失败")
    void should_throwIllegalArgument_when_workerIdOutOfRange() {
        // Act & Assert
        assertThatThrownBy(() -> new SnowflakeSequenceProvider(32L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workerId");
    }
}
