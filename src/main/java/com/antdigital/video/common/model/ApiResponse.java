package com.antdigital.video.common.model;

/**
 * 统一响应包装。code=OK 表示成功，其余为业务错误码。
 *
 * @param <T> 数据负载类型
 */
public class ApiResponse<T> {

    private String code;
    private String msg;
    private T data;
    private String errorCode;
    private String errorMessage;
    private String userTip;

    public ApiResponse() {
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = "OK";
        response.msg = "SUCCESS";
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, String userTip) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = errorCode;
        response.msg = "FAIL";
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        response.userTip = userTip;
        return response;
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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUserTip() {
        return userTip;
    }

    public void setUserTip(String userTip) {
        this.userTip = userTip;
    }

    @Override
    public String toString() {
        return "ApiResponse{code='" + code + "', msg='" + msg + "', errorCode='" + errorCode + "'}";
    }
}