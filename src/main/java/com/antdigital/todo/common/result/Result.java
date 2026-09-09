package com.antdigital.todo.common.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 统一API响应结果
 *
 * @param <T> 数据类型
 * @author AiWork
 * @since 2026-09-09
 */
public class Result<T> implements Serializable {

    /**
     * 成功结果码
     */
    public static final String SUCCESS_CODE = "SUCCESS";

    /**
     * 成功提示信息
     */
    public static final String SUCCESS_MSG = "操作成功";

    /**
     * 结果码
     */
    @JsonProperty("code")
    private String code;

    /**
     * 提示信息
     */
    @JsonProperty("msg")
    private String msg;

    /**
     * 业务数据
     */
    @JsonProperty("data")
    private T data;

    /**
     * 私有构造
     */
    private Result() {
    }

    /**
     * 构建成功结果
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.msg = SUCCESS_MSG;
        result.data = data;
        return result;
    }

    /**
     * 构建成功结果（自定义提示信息）
     *
     * @param msg  提示信息
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.msg = msg;
        result.data = data;
        return result;
    }

    /**
     * 构建失败结果
     *
     * @param code 错误码
     * @param msg  提示信息
     * @param <T>  数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
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
