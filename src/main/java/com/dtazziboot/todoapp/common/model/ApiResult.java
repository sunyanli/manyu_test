package com.dtazziboot.todoapp.common.model;

import com.dtazziboot.todoapp.common.constant.TodoConstants;
import com.dtazziboot.todoapp.common.enums.ErrorCodeEnum;

/**
 * 统一出参结构：{result, msg, data}
 *
 * @param <T> 业务数据类型
 * @author AiWork
 * @date 2026/09/09
 */
public class ApiResult<T> {

    /**
     * 结果码：成功为 OK，失败为具体错误码（如 TODO_001）
     */
    private String result;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 无参构造
     */
    public ApiResult() {
    }

    /**
     * 全参构造
     *
     * @param result 结果码
     * @param msg    提示信息
     * @param data   业务数据
     */
    public ApiResult(String result, String msg, T data) {
        this.result = result;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构建成功响应
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(TodoConstants.RESULT_OK, TodoConstants.MSG_SUCCESS, data);
    }

    /**
     * 构建失败响应
     *
     * @param errorCode 错误码枚举
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> ApiResult<T> fail(ErrorCodeEnum errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 构建失败响应
     *
     * @param code 结果码
     * @param msg  提示信息
     * @param <T>  业务数据类型
     * @return 失败响应
     */
    public static <T> ApiResult<T> fail(String code, String msg) {
        return new ApiResult<>(code, msg, null);
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

    @Override
    public String toString() {
        return "ApiResult{result=" + result + ", msg=" + msg + ", data=" + data + '}';
    }
}
