package com.manyu.algodemo.common.web;

/**
 * 统一出参结构：{@code {code, msg, data}}。
 *
 * @param <T> 业务数据类型
 */
public class CommonResponse<T> {

    /** 结果码，成功为 OK。 */
    private String code;
    /** 提示信息。 */
    private String msg;
    /** 业务数据。 */
    private T data;

    public CommonResponse() {
    }

    private CommonResponse(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>("OK", "SUCCESS", data);
    }

    /**
     * 构造失败响应。
     *
     * @param code 错误码
     * @param msg  错误信息
     * @param <T>  数据类型
     * @return 失败响应
     */
    public static <T> CommonResponse<T> fail(String code, String msg) {
        return new CommonResponse<>(code, msg, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
