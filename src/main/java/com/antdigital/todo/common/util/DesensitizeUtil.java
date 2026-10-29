package com.antdigital.todo.common.util;

import java.util.regex.Pattern;

/**
 * 日志脱敏工具：对手机号、身份证号、邮箱做掩码并按长度截断，满足系分 6.4.3.2 要求。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public final class DesensitizeUtil {

    /**
     * 日志文本最大长度，超出部分截断
     */
    private static final int MAX_LOG_TEXT_LENGTH = 100;

    /**
     * 手机号掩码规则：保留前 3 位与后 4 位
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    /**
     * 身份证号掩码规则：保留前 6 位与后 4 位
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}([0-9Xx]{4})");

    /**
     * 邮箱掩码规则：保留前 1~3 位与域名
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.+-]{1,3})[\\w.+-]*(@[\\w.-]+)");

    /**
     * 手机号掩码替换模板
     */
    private static final String PHONE_REPLACEMENT = "$1****$2";

    /**
     * 身份证号掩码替换模板
     */
    private static final String ID_CARD_REPLACEMENT = "$1********$2";

    /**
     * 邮箱掩码替换模板
     */
    private static final String EMAIL_REPLACEMENT = "$1***$2";

    private DesensitizeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 对文本做敏感信息掩码并截断，供日志与审计事件使用。
     *
     * @param text 原始文本，null 返回空串
     * @return 脱敏并截断后的文本
     */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String masked = ID_CARD_PATTERN.matcher(text).replaceAll(ID_CARD_REPLACEMENT);
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT);
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT);
        return TodoTextUtil.truncate(masked, MAX_LOG_TEXT_LENGTH);
    }
}
