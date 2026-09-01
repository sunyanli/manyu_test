package com.example.demo.common.model;

import java.io.Serializable;

/**
 * 通用 API 响应封装
 *
 * @param <T> 响应数据类型
 * @author AiWork
 */
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结果码 */
    private String code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "SUCCESS", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("OK", "SUCCESS", null);
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
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

    @Override
    public String toString() {
        return "ApiResponse{code='" + code + "', msg='" + msg + "', data=" + data + "}";
    }
}