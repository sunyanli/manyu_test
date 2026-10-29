package com.antdigital.todo.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 受信登录网关白名单配置，支持精确 IP 与 IPv4 CIDR。
 *
 * <p>列表为空时按 fail-closed 拒绝全部 {@code /api/**} 请求，
 * 必须显式配置网关地址后才可对外服务。</p>
 *
 * @author AiWork
 */
@ConfigurationProperties(prefix = "todo.security")
public class TrustedGatewayProperties {

    /** 受信网关 IP 列表。 */
    private List<String> trustedGatewayIps = new ArrayList<String>();

    public List<String> getTrustedGatewayIps() {
        return trustedGatewayIps;
    }

    public void setTrustedGatewayIps(List<String> trustedGatewayIps) {
        this.trustedGatewayIps = trustedGatewayIps;
    }
}
