package com.antdigital.todo.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TodoCreateSettings} 单元测试：验证运行期实时读取配置（热更新）与绑定回落。
 *
 * @author AiWork
 */
class TodoCreateSettingsTest {

    @Test
    @DisplayName("无 Environment：直接读取绑定对象，兼容纯单元测试")
    void should_readFromProperties_when_environmentAbsent() {
        // Arrange
        TodoCreateProperties properties = new TodoCreateProperties();
        properties.setEnabled(false);
        properties.setGrayEnabled(true);
        properties.setGrayTenantSuffix(Arrays.asList("3"));
        properties.setRateLimitQps(5);
        TodoCreateSettings settings = new TodoCreateSettings(properties, null);

        // Assert
        assertThat(settings.isCreateEnabled()).isFalse();
        assertThat(settings.isGrayEnabled()).isTrue();
        assertThat(settings.grayTenantSuffix()).containsExactly("3");
        assertThat(settings.rateLimitQps()).isEqualTo(5);
    }

    @Test
    @DisplayName("配置变更后无需重启：再次读取即返回新值（G17.1 热更新）")
    void should_reflectChangedConfiguration_withoutRestart() {
        // Arrange
        MockEnvironment environment = new MockEnvironment();
        TodoCreateProperties properties = new TodoCreateProperties();
        TodoCreateSettings settings = new TodoCreateSettings(properties, environment);

        // Act & Assert：初始态
        assertThat(settings.isCreateEnabled()).isTrue();
        assertThat(settings.rateLimitQps()).isEqualTo(10);

        // Act：模拟配置中心推送新值
        environment.setProperty(TodoCreateSettings.KEY_ENABLED, "false");
        environment.setProperty(TodoCreateSettings.KEY_RATE_LIMIT_QPS, "3");

        // Assert：无需重建 Bean 即生效
        assertThat(settings.isCreateEnabled()).isFalse();
        assertThat(settings.rateLimitQps()).isEqualTo(3);
    }

    @Test
    @DisplayName("Environment 未配置该项：回落到绑定对象默认值")
    void should_fallBackToProperties_when_keyMissing() {
        // Arrange
        MockEnvironment environment = new MockEnvironment();
        TodoCreateProperties properties = new TodoCreateProperties();
        properties.setGrayEnabled(false);
        TodoCreateSettings settings = new TodoCreateSettings(properties, environment);

        // Assert
        assertThat(settings.isGrayEnabled()).isFalse();
    }
}
