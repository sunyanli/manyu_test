package com.antdigital.todo.common.model;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;

/**
 * 统一响应体：{@code code} 为结果码，成功为 OK，失败为业务错误码。
 *
 * @param <T> 业务数据类型
 * @author AiWork
 * @date 2026/10/29
 */
public class ApiResult<T> {

    /**
     * 结果码，OK 表示成功
     */
    private String code;

    /**
     * 提示信息，成功为 SUCCESS
     */
    private String msg;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 构造空响应体，供反序列化使用。
     */
    public ApiResult() {
    }

    /**
     * 构造响应体。
     *
     * @param code 结果码
     * @param msg  提示信息
     * @param data 业务数据
     */
    public ApiResult(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功响应体
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(TodoConstants.RESULT_CODE_OK, TodoConstants.RESULT_MSG_SUCCESS, data);
    }

    /**
     * 构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       业务数据类型
     * @return 失败响应体
     */
    public static <T> ApiResult<T> failure(TodoErrorCodeEnum errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMsg(), null);
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
