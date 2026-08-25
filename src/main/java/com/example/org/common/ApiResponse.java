package com.example.org.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper.
 *
 * @param <T> the type of the data payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String msg;
    private T data;

    /**
     * Create a successful response with the given data.
     *
     * @param <T>  the type of the data
     * @param data the response payload
     * @return ApiResponse with code 200 and msg "success"
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * Create a successful response with a custom message and data.
     *
     * @param <T>  the type of the data
     * @param msg  the success message
     * @param data the response payload
     * @return ApiResponse with code 200
     */
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>(200, msg, data);
    }

    /**
     * Create an error response with the given code and message.
     *
     * @param <T>  the type of the data (always null)
     * @param code the error code
     * @param msg  the error message
     * @return ApiResponse with the given code and msg, data set to null
     */
    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}