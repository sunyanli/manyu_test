package com.manyu.todo.common.model;

/**
 * 统一响应包装对象。
 *
 * <p>code 为 "SUCCESS" 表示成功，失败时为 5 位错误码（来源字母 + 4 位数字）。
 * 错误码仅用于程序比对与溯源，用户可读信息放在 message 字段。
 *
 * @param <T> 业务数据类型
 * @author AiWork
 */
public class Result<T> {

    private String code;

    private String message;

    private T data;

    public Result() {
    }

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("SUCCESS", "操作成功", data);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{code='" + code + "', message='" + message + "', data=" + data + '}';
    }
}
