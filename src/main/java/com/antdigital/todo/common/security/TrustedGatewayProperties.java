package com.antdigital.todo.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 受信登录网关白名单配置。列表为空表示不校验来源 IP（由网络层保证只有网关可达）。
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
