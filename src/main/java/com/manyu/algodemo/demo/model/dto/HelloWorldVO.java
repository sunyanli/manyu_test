package com.manyu.algodemo.demo.model.dto;

/**
 * helloworld 接口出参。
 */
public class HelloWorldVO {

    /** 问候文案，如 "Hello, World!"。 */
    private String message;
    /** 服务端时间（ISO-8601）。 */
    private String serverTime;
    /** 请求链路 ID。 */
    private String requestId;
    /** 处理耗时（毫秒）。 */
    private long costTimeMs;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getCostTimeMs() {
        return costTimeMs;
    }

    public void setCostTimeMs(long costTimeMs) {
        this.costTimeMs = costTimeMs;
    }
}
