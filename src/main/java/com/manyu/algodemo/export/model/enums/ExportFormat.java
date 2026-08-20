package com.manyu.algodemo.export.model.enums;

/**
 * 导出格式。
 */
public enum ExportFormat {

    /** CSV（默认，零依赖轻量格式）。 */
    CSV;

    /**
     * 解析导出格式，非法返回默认 CSV。
     *
     * @param format 格式标识
     * @return 导出格式
     */
    public static ExportFormat parse(String format) {
        if (format == null || format.isBlank()) {
            return CSV;
        }
        try {
            return valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CSV;
        }
    }
}
