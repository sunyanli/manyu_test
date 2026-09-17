package com.antdigital.video.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一响应构造工具。
 */
final class Responses {

    private Responses() {
    }

    static Map<String, Object> createSuccess(Long taskId, String taskNo, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("taskNo", taskNo);
        data.put("status", status);
        return wrap("OK", "SUCCESS", data);
    }

    static Map<String, Object> wrap(String code, String msg, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("msg", msg);
        body.put("data", data);
        return body;
    }
}