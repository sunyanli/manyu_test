package com.antdigital.video.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TaskStatusEnum} 全量测试。
 */
class TaskStatusEnumTest {

    @Test
    @DisplayName("枚举成员完整")
    void should_haveAllMembers() {
        assertThat(TaskStatusEnum.values()).containsExactly(
                TaskStatusEnum.CREATED,
                TaskStatusEnum.SUBMITTED,
                TaskStatusEnum.PROCESSING,
                TaskStatusEnum.SUCCEEDED,
                TaskStatusEnum.FAILED,
                TaskStatusEnum.CANCELLED);
    }

    @Test
    @DisplayName("终态判定正确")
    void should_markTerminalStatus() {
        assertThat(TaskStatusEnum.SUCCEEDED.isTerminal()).isTrue();
        assertThat(TaskStatusEnum.FAILED.isTerminal()).isTrue();
        assertThat(TaskStatusEnum.CANCELLED.isTerminal()).isTrue();
        assertThat(TaskStatusEnum.CREATED.isTerminal()).isFalse();
        assertThat(TaskStatusEnum.SUBMITTED.isTerminal()).isFalse();
        assertThat(TaskStatusEnum.PROCESSING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("可取消判定正确")
    void should_markCancellableStatus() {
        assertThat(TaskStatusEnum.CREATED.isCancellable()).isTrue();
        assertThat(TaskStatusEnum.SUBMITTED.isCancellable()).isTrue();
        assertThat(TaskStatusEnum.PROCESSING.isCancellable()).isTrue();
        assertThat(TaskStatusEnum.SUCCEEDED.isCancellable()).isFalse();
        assertThat(TaskStatusEnum.FAILED.isCancellable()).isFalse();
        assertThat(TaskStatusEnum.CANCELLED.isCancellable()).isFalse();
    }
}