package com.example.algorithm.model.dto;

/**
 * Generic API response wrapper.
 *
 * @param <T> the type of data
 */
public class ApiResponse<T> {

    private String result;
    private String msg;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String result, String msg, T data) {
        this.result = result;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "SUCCESS", data);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>("ERROR", msg, null);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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
