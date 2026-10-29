package com.antdigital.todo.service.component;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoCreateGate} 功能开关与灰度单元测试（系分 §7.2/§7.3）。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoCreateGateTest {

    @Test
    @DisplayName("总开关打开且未配置灰度时报文全量开放")
    void should_pass_when_enabledAndNoGrayConfig() {
        // Arrange
        TodoCreateGate gate = new TodoCreateGate(true, "");

        // Act & Assert
        assertThatCode(() -> gate.check("12345")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("总开关关闭时返回 TODO_0010")
    void should_throwFeatureDisabled_when_switchOff() {
        // Arrange
        TodoCreateGate gate = new TodoCreateGate(false, "");

        // Act & Assert
        assertThatThrownBy(() -> gate.check("1"))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getCode())
                                .isEqualTo(TodoErrorCodeEnum.FEATURE_DISABLED.getCode()));
    }

    @Test
    @DisplayName("灰度尾号命中时可创建，未命中返回 TODO_0010")
    void should_filterByTenantTail_when_grayConfigured() {
        // Arrange
        TodoCreateGate gate = new TodoCreateGate(true, "1, 3 ,5");

        // Act & Assert
        assertThatCode(() -> gate.check("1001")).doesNotThrowAnyException();
        assertThatCode(() -> gate.check("1003")).doesNotThrowAnyException();
        assertThatThrownBy(() -> gate.check("1002")).isInstanceOf(TodoBizException.class);
    }

    @Test
    @DisplayName("灰度配置非法时按全量开放并记录 ERROR 日志")
    void should_treatAsFullRelease_when_grayConfigInvalid() {
        // Arrange
        TodoCreateGate gate = new TodoCreateGate(true, "abc");

        // Act & Assert
        assertThatCode(() -> gate.check("1002")).doesNotThrowAnyException();
    }
}
