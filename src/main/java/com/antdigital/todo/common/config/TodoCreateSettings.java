package com.antdigital.todo.common.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 新增待办运行期配置读取器：每次调用实时解析，
 * 实现配置中心改值后无需重启即生效（G17.1）。
 *
 * <p>解析优先级：{@link Environment} 中存在的键优先，
 * 绑定缺失时回落到 {@link TodoCreateProperties}。未接入 Spring 容器（如纯单元测试）时
 * {@code environment} 为 null，等价于直接读取 properties。</p>
 *
 * @author AiWork
 */
@Component
public class TodoCreateSettings {

    /** 总开关配置键。 */
    public static final String KEY_ENABLED = "todo.create.enabled";

    /** 灰度开关配置键。 */
    public static final String KEY_GRAY_ENABLED = "todo.create.gray-enabled";

    /** 灰度租户尾号配置键。 */
    public static final String KEY_GRAY_TENANT_SUFFIX = "todo.create.gray-tenant-suffix";

    /** 单用户限流阈值配置键。 */
    public static final String KEY_RATE_LIMIT_QPS = "todo.create.rate-limit-qps";

    private final TodoCreateProperties properties;

    private final Binder binder;

    public TodoCreateSettings(TodoCreateProperties properties, Environment environment) {
        this.properties = properties;
        this.binder = environment == null ? null : Binder.get(environment);
    }

    /**
     * 总开关是否开启。
     *
     * @return true 表示功能可用
     */
    public boolean isCreateEnabled() {
        if (binder == null) {
            return properties.isEnabled();
        }
        return binder.bind(KEY_ENABLED, Boolean.class).orElseGet(properties::isEnabled);
    }

    /**
     * 是否启用按租户尾号灰度。
     *
     * @return true 表示启用灰度
     */
    public boolean isGrayEnabled() {
        if (binder == null) {
            return properties.isGrayEnabled();
        }
        return binder.bind(KEY_GRAY_ENABLED, Boolean.class).orElseGet(properties::isGrayEnabled);
    }

    /**
     * 灰度命中的租户尾号列表。
     *
     * @return 尾号列表，可能为空
     */
    public List<String> grayTenantSuffix() {
        if (binder == null) {
            return safeList(properties.getGrayTenantSuffix());
        }
        return safeList(binder.bind(KEY_GRAY_TENANT_SUFFIX, Bindable.listOf(String.class))
                .orElseGet(properties::getGrayTenantSuffix));
    }

    /**
     * 单用户限流阈值（QPS）。
     *
     * @return 每秒令牌数，非正数表示不限流
     */
    public int rateLimitQps() {
        if (binder == null) {
            return properties.getRateLimitQps();
        }
        return binder.bind(KEY_RATE_LIMIT_QPS, Integer.class).orElseGet(properties::getRateLimitQps);
    }

    private List<String> safeList(List<String> source) {
        return source == null ? Collections.<String>emptyList() : source;
    }
}
