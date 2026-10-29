package com.antdigital.todo.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 新增待办能力的开关、灰度与限流配置，支持配置中心热更新。
 *
 * @author AiWork
 */
@ConfigurationProperties(prefix = "todo.create")
public class TodoCreateProperties {

    /** 总开关，关闭后接口直接返回 TODO_007。 */
    private boolean enabled = true;

    /** 是否按租户尾号灰度，关闭表示全量放开；上线初期默认开启灰度。 */
    private boolean grayEnabled = true;

    /** 灰度命中的租户 ID 尾号，如 0,1,2。 */
    private List<String> grayTenantSuffix = new ArrayList<String>();

    /** 单用户限流阈值（QPS），实例级令牌桶。 */
    private int rateLimitQps = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGrayEnabled() {
        return grayEnabled;
    }

    public void setGrayEnabled(boolean grayEnabled) {
        this.grayEnabled = grayEnabled;
    }

    public List<String> getGrayTenantSuffix() {
        return grayTenantSuffix;
    }

    public void setGrayTenantSuffix(List<String> grayTenantSuffix) {
        this.grayTenantSuffix = grayTenantSuffix;
    }

    public int getRateLimitQps() {
        return rateLimitQps;
    }

    public void setRateLimitQps(int rateLimitQps) {
        this.rateLimitQps = rateLimitQps;
    }
}
