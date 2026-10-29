package com.antdigital.todo.service.component;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    @DisplayName("编号格式：TD + yyyyMMdd + 6 位序号")
    void should_generateTodoNo_when_sequenceGiven() {
        // Arrange
        TodoNoGeneratorImpl generator = buildGenerator(dateKey -> 42L, () -> FIXED_DATE);

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
        TodoNoGeneratorImpl generator = buildGenerator(dateKey -> 1L, () -> FIXED_DATE);

        // Act
        String todoNo = generator.nextTodoNo();

        // Assert
        assertThat(todoNo).isEqualTo("TD20261029000001");
    }

    @Test
    @DisplayName("序号超过 6 位时按 6 位取模，回绕后仍为定长")
    void should_wrapSequence_when_sequenceExceedsSixDigits() {
        // Arrange
        TodoNoGeneratorImpl generator = buildGenerator(dateKey -> 1_000_001L, () -> FIXED_DATE);

        // Act
        String todoNo = generator.nextTodoNo();

        // Assert
        assertThat(todoNo).isEqualTo("TD20261029000001");
    }

    @Test
    @DisplayName("生产构造使用雪花取号，连续生成不重复")
    void should_generateDistinctTodoNo_when_useSnowflakeProvider() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(new SnowflakeSequenceProvider(1L, 1L),
                new TodoMetrics(meterRegistry));

        // Act
        String first = generator.nextTodoNo();
        String second = generator.nextTodoNo();

        // Assert
        assertThat(first).hasSize(16).startsWith("TD");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("雪花取号属降级路径：上报 todo_no_degrade_total")
    void should_recordDegradeMetric_when_snowflakeUsed() {
        // Arrange
        TodoNoGeneratorImpl generator = new TodoNoGeneratorImpl(new SnowflakeSequenceProvider(2L, 2L),
                new TodoMetrics(meterRegistry));

        // Act
        generator.nextTodoNo();

        // Assert
        assertThat(meterRegistry.get("todo_no_degrade_total").counter().count()).isEqualTo(1.0D);
    }

    @Test
    @DisplayName("非降级取号不产生降级指标")
    void should_notRecordDegradeMetric_when_providerNotDegraded() {
        // Arrange
        TodoNoGeneratorImpl generator = buildGenerator(dateKey -> 7L, () -> FIXED_DATE);

        // Act
        generator.nextTodoNo();

        // Assert：计数器已注册但计数保持为 0
        assertThat(meterRegistry.get("todo_no_degrade_total").counter().count()).isZero();
    }

    @Test
    @DisplayName("雪花节点参数越界时快速失败")
    void should_throwIllegalArgument_when_workerIdOutOfRange() {
        // Act & Assert
        assertThatThrownBy(() -> new SnowflakeSequenceProvider(32L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workerId");
    }

    @Test
    @DisplayName("雪花数据中心参数越界时快速失败")
    void should_throwIllegalArgument_when_datacenterIdOutOfRange() {
        // Act & Assert
        assertThatThrownBy(() -> new SnowflakeSequenceProvider(0L, 32L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datacenterId");
    }

    @Test
    @DisplayName("不同节点生成的编号互不相同")
    void should_generateDifferentNo_when_nodeIdDiffers() {
        // Arrange
        long first = new SnowflakeSequenceProvider(1L, 1L).nextSequence("20261029");
        long second = new SnowflakeSequenceProvider(2L, 2L).nextSequence("20261029");

        // Act & Assert
        assertThat(first).isNotEqualTo(second);
    }

    private TodoNoGeneratorImpl buildGenerator(TodoSequenceProvider provider,
                                               java.util.function.Supplier<LocalDate> dateSupplier) {
        return new TodoNoGeneratorImpl(provider, new TodoMetrics(meterRegistry), dateSupplier);
    }
}
