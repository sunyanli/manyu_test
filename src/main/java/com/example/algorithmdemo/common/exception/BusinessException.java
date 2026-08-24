package com.example.algorithmdemo.common.exception;

import com.example.algorithmdemo.common.constant.ErrorCodeConstant;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static BusinessException paramInvalid(String message) {
        return new BusinessException(ErrorCodeConstant.PARAM_001, message);
    }

    public static BusinessException unsupportedAlgorithm() {
        return new BusinessException(ErrorCodeConstant.ALGO_001, "不支持的哈希算法，仅支持 MD5/SHA-256/SHA-512");
    }

    public static BusinessException exportDataEmpty() {
        return new BusinessException(ErrorCodeConstant.EXPORT_001, "导出数据为空");
    }

    public static BusinessException unsupportedExportFormat() {
        return new BusinessException(ErrorCodeConstant.EXPORT_002, "不支持的导出格式，仅支持 xlsx/csv");
    }

    public static BusinessException unsupportedDimension() {
        return new BusinessException(ErrorCodeConstant.TRACK_001, "不支持的统计维度");
    }
}