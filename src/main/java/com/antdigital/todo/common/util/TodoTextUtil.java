package com.antdigital.todo.common.util;

import java.text.Normalizer;

/**
 * 文本处理工具：Unicode 规范化、控制字符剔除、全角空白剔除与按字符计长。
 *
 * <p>统一按字符（code point）计长，保证 emoji 等代理对字符计为 1 个字符，
 * 与系分 A05「按字符计，非字节」一致。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public final class TodoTextUtil {

    /**
     * 换行符（规范化后保留）
     */
    private static final char LINE_FEED = '\n';

    /**
     * 制表符（规范化后保留）
     */
    private static final char TAB = '\t';

    private TodoTextUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 判断字符串是否为 null 或仅含空白字符（含全角空格）。
     *
     * @param text 待判断文本，可为 null
     * @return true 表示 null 或纯空白
     */
    public static boolean isBlank(String text) {
        return text == null || trimWhitespace(text).isEmpty();
    }

    /**
     * 文本规范化：Unicode NFC 规范化 + 剔除控制字符（保留换行符与制表符）。
     *
     * @param text 原始文本，null 视为空串
     * @return 规范化后的文本；null 入参返回空串
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); ) {
            int codePoint = normalized.codePointAt(index);
            index += Character.charCount(codePoint);
            if (isRemovableControl(codePoint)) {
                continue;
            }
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }

    /**
     * 去除首尾空白字符（含全角空格 U+3000、不换行空格 U+00A0）。
     *
     * @param text 原始文本，null 视为空串
     * @return 去除首尾空白后的文本
     */
    public static String trimWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = text.length();
        while (start < end) {
            int codePoint = text.codePointAt(start);
            if (!isWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (end > start) {
            int codePoint = text.codePointBefore(end);
            if (!isWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return text.substring(start, end);
    }

    /**
     * 按字符（code point）统计文本长度。
     *
     * @param text 待统计文本，null 计为 0
     * @return 字符个数
     */
    public static int lengthOf(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.codePointCount(0, text.length());
    }

    /**
     * 按字符截断文本，用于日志输出，避免超长入参污染日志。
     *
     * @param text      原始文本，null 返回空串
     * @param maxLength 最大字符数，需大于 0
     * @return 截断后的文本
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (maxLength <= 0) {
            return "";
        }
        if (lengthOf(text) <= maxLength) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, maxLength));
    }

    /**
     * 判断是否为需要剔除的控制字符（换行符与制表符保留）。
     *
     * @param codePoint 字符编码
     * @return true 表示需要剔除
     */
    private static boolean isRemovableControl(int codePoint) {
        if (codePoint == LINE_FEED || codePoint == TAB) {
            return false;
        }
        return Character.getType(codePoint) == Character.CONTROL;
    }

    /**
     * 判断是否为空白字符（含全角空格等分隔符）。
     *
     * @param codePoint 字符编码
     * @return true 表示空白字符
     */
    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
