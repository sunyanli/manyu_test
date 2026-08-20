package com.manyu.algodemo.export.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV 导出工具：UTF-8 BOM、CRLF 换行、字段转义与公式注入防护。
 */
public final class CsvExportUtil {

    /** CSV 行分隔符。 */
    private static final String CRLF = "\r\n";
    /** 公式注入风险前缀。 */
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private CsvExportUtil() {
    }

    /**
     * 将表格数据序列化为 CSV 字节流（带 UTF-8 BOM）。
     *
     * @param header 表头
     * @param rows   数据行
     * @return CSV 字节数组
     */
    public static byte[] toCsv(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header);
        for (List<String> row : rows) {
            appendRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append(CRLF);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        // R04 防 CSV 公式注入：以 = + - @ 或制表符/回车开头的单元格前置单引号
        if (!value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
