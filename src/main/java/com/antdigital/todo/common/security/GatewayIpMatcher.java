package com.antdigital.todo.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 受信网关 IP 匹配器，支持精确 IP 与 IPv4 CIDR（如 {@code 10.0.0.0/24}）两种写法。
 *
 * <p>白名单为空或表达式非法时一律视为不匹配，
 * 调用方据此采取 fail-closed 策略（S8.1）。</p>
 *
 * @author AiWork
 */
public final class GatewayIpMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayIpMatcher.class);

    private static final int IPV4_BITS = 32;

    private static final long IPV4_MASK = 0xFFFFFFFFL;

    private GatewayIpMatcher() {
    }

    /**
     * 判断来源 IP 是否命中白名单。
     *
     * @param patterns 白名单，支持精确 IP 与 IPv4 CIDR
     * @param ip       来源 IP
     * @return 命中返回 true；白名单为空、IP 为空或表达式非法返回 false
     */
    public static boolean matches(List<String> patterns, String ip) {
        if (patterns == null || patterns.isEmpty() || ip == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (matchesSingle(pattern, ip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesSingle(String pattern, String ip) {
        if (pattern == null) {
            return false;
        }
        String candidate = pattern.trim();
        if (candidate.isEmpty()) {
            return false;
        }
        int cidrIndex = candidate.indexOf('/');
        if (cidrIndex < 0) {
            return candidate.equals(ip.trim());
        }
        return matchesCidr(candidate, cidrIndex, ip);
    }

    private static boolean matchesCidr(String candidate, int cidrIndex, String ip) {
        String network = candidate.substring(0, cidrIndex);
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(candidate.substring(cidrIndex + 1).trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("受信网关白名单 CIDR 前缀非法, pattern={}", candidate);
            return false;
        }
        if (prefixLength < 0 || prefixLength > IPV4_BITS) {
            LOGGER.warn("受信网关白名单 CIDR 前缀越界, pattern={}", candidate);
            return false;
        }
        long ipValue = toIpv4Value(ip);
        long networkValue = toIpv4Value(network);
        if (ipValue < 0 || networkValue < 0) {
            return false;
        }
        if (prefixLength == 0) {
            return true;
        }
        long mask = IPV4_MASK << (IPV4_BITS - prefixLength) & IPV4_MASK;
        return (ipValue & mask) == (networkValue & mask);
    }

    /**
     * 将点分十进制 IPv4 转为长整型。
     *
     * @param value IPv4 文本
     * @return 转换失败返回 -1
     */
    private static long toIpv4Value(String value) {
        if (value == null) {
            return -1L;
        }
        String[] segments = value.trim().split("\\.");
        if (segments.length != 4) {
            return -1L;
        }
        long result = 0L;
        for (String segment : segments) {
            int octet;
            try {
                octet = Integer.parseInt(segment);
            } catch (NumberFormatException ex) {
                return -1L;
            }
            if (octet < 0 || octet > 255) {
                return -1L;
            }
            result = result << 8 | octet;
        }
        return result;
    }
}
