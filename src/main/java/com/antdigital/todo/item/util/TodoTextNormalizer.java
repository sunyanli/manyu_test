package com.antdigital.todo.item.util;

/**
 * 待办文本规范化与非法字符检查。
 *
 * @author AiWork
 */
public final class TodoTextNormalizer {

    private static final char SPACE = ' ';

    private static final char LINE_FEED = '\n';

    private static final char TAB = '\t';

    private TodoTextNormalizer() {
    }

    /**
     * 规范化文本：去除首尾空白、全角空格归一为半角、连续空白折叠为单个空格。
     *
     * @param raw 原始文本，可为 null
     * @return 规范化结果，null 入参返回空串
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        boolean pendingSpace = false;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (isBlankChar(current)) {
                pendingSpace = builder.length() > 0;
                continue;
            }
            if (pendingSpace) {
                builder.append(SPACE);
                pendingSpace = false;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    /**
     * 判断文本是否包含非法控制字符（\n、\t 除外）。
     *
     * @param text 文本
     * @return 含非法字符返回 true
     */
    public static boolean containsIllegalChar(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isISOControl(current) && current != LINE_FEED && current != TAB) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按字符（码点）统计长度，emoji 等多字节字符按 1 个字符计。
     *
     * @param text 文本
     * @return 字符数
     */
    public static int charLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.codePointCount(0, text.length());
    }

    private static boolean isBlankChar(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
