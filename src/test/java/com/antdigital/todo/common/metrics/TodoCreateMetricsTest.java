package com.antdigital.todo.common.metrics;

import com.antdigital.todo.common.api.ResultCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TodoCreateMetrics} 单元测试。
 *
 * @author AiWork
 */
class TodoCreateMetricsTest {

    private SimpleMeterRegistry meterRegistry;

    private TodoCreateMetrics todoCreateMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        todoCreateMetrics = new TodoCreateMetrics(meterRegistry);
    }

    @Test
    @DisplayName("成功：累加 OK 调用量并记录耗时")
    void should_recordSuccessCounterAndCost() {
        // Act
        todoCreateMetrics.recordSuccess(2_000_000L);

        // Assert
        assertThat(todoCreateMetrics.countOf(ResultCode.OK.getCode())).isEqualTo(1.0d);
        assertThat(todoCreateMetrics.costTimer().count()).isEqualTo(1L);
        assertThat(meterRegistry.find(TodoCreateMetrics.METRIC_COST).timer()).isNotNull();
    }

    @Test
    @DisplayName("失败：按结果码分桶，互不干扰")
    void should_recordFailureByResultCode() {
        // Act
        todoCreateMetrics.recordFailure(ResultCode.TODO_001.getCode());
        todoCreateMetrics.recordFailure(ResultCode.TODO_001.getCode());
        todoCreateMetrics.recordFailure(ResultCode.TODO_004.getCode());

        // Assert
        assertThat(todoCreateMetrics.countOf(ResultCode.TODO_001.getCode())).isEqualTo(2.0d);
        assertThat(todoCreateMetrics.countOf(ResultCode.TODO_004.getCode())).isEqualTo(1.0d);
        assertThat(todoCreateMetrics.countOf(ResultCode.TODO_006.getCode())).isZero();
    }
}
