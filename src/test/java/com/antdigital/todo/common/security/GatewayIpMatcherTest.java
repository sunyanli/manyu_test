package com.antdigital.todo.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayIpMatcher} 单元测试。
 *
 * @author AiWork
 */
class GatewayIpMatcherTest {

    @Test
    @DisplayName("精确 IP：命中与不命中")
    void should_matchExactIp() {
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.1"), "10.0.0.1")).isTrue();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.1"), "10.0.0.2")).isFalse();
    }

    @Test
    @DisplayName("CIDR：网段内命中，网段外不命中")
    void should_matchCidrRange() {
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.0/24"), "10.0.0.37")).isTrue();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.0/24"), "10.0.1.1")).isFalse();
    }

    @Test
    @DisplayName("CIDR：/32 等价单 IP，/0 命中全部 IPv4")
    void should_handleBoundaryPrefix() {
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.1/32"), "10.0.0.1")).isTrue();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.1/32"), "10.0.0.2")).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("0.0.0.0/0"), "192.168.1.9")).isTrue();
    }

    @Test
    @DisplayName("多表达式：任意命中即放行")
    void should_matchAnyPattern() {
        assertThat(GatewayIpMatcher.matches(Arrays.asList("10.0.0.1", "172.16.0.0/16"), "172.16.9.9")).isTrue();
    }

    @Test
    @DisplayName("白名单为空 / IP 为空 / 表达式非法：一律不匹配（fail-closed）")
    void should_notMatch_whenWhiteListInvalid() {
        assertThat(GatewayIpMatcher.matches(Collections.<String>emptyList(), "10.0.0.1")).isFalse();
        assertThat(GatewayIpMatcher.matches(null, "10.0.0.1")).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.1"), null)).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.0/33"), "10.0.0.1")).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("10.0.0.0/abc"), "10.0.0.1")).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList("not-an-ip"), "10.0.0.1")).isFalse();
        assertThat(GatewayIpMatcher.matches(Collections.singletonList(""), "10.0.0.1")).isFalse();
    }
}
