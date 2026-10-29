package com.antdigital.todo.common.api;

/**
 * 统一出参结构 {@code {result, msg, data}}。
 *
 * @param <T> 业务数据类型
 * @author AiWork
 */
public class ApiResponse<T> {

    private final String result;

    private final String msg;

    private final T data;

    private ApiResponse(String result, String msg, T data) {
        this.result = result;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构造成功响应，result 固定为 {@code OK}、msg 固定为 {@code SUCCESS}。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>(ResultCode.OK.getCode(), ResultCode.OK.getMessage(), data);
    }

    /**
     * 构造失败响应。
     *
     * @param resultCode 结果码
     * @return 失败响应，data 为 null
     */
    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 构造失败响应。
     *
     * @param result 结果码
     * @param msg    提示文案
     * @param <T>    业务数据类型
     * @return 失败响应，data 为 null
     */
    public static <T> ApiResponse<T> fail(String result, String msg) {
        return new ApiResponse<T>(result, msg, null);
    }

    public String getResult() {
        return result;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
