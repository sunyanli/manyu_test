package com.alipay.xchain.web.demo.common;

/**
 * 统一响应体，格式 {code, msg, data}
 *
 * @param <T> 数据泛型
 * @author AiWork
 * @since 2026-10-30
 */
public class CommonResponse<T> {

    /** 结果码，成功 OK */
    private String code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    private CommonResponse() {}

    public static <T> CommonResponse<T> ok(T data) {
        CommonResponse<T> resp = new CommonResponse<>();
        resp.code = "OK";
        resp.msg = "SUCCESS";
        resp.data = data;
        return resp;
    }

    public static <T> CommonResponse<T> ok(String msg, T data) {
        CommonResponse<T> resp = new CommonResponse<>();
        resp.code = "OK";
        resp.msg = msg;
        resp.data = data;
        return resp;
    }

    public static <T> CommonResponse<T> fail(String code, String msg) {
        CommonResponse<T> resp = new CommonResponse<>();
        resp.code = code;
        resp.msg = msg;
        resp.data = null;
        return resp;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "CommonResponse{code='" + code + "', msg='" + msg + "'}";
    }
}