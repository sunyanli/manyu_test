package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * 待办编号生成器实现（S04）：TD + yyyyMMdd + 6 位当日序号。
 *
 * <p>取号能力通过 {@link TodoSequenceProvider} 注入，默认使用本地雪花序号（零外部依赖）；
 * 生产环境可替换为 Redis INCR 实现。序号按 6 位取模补零，重复时由唯一索引兜底、
 * 调用方重试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoNoGeneratorImpl implements TodoNoGenerator {

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(TodoConstants.TODO_NO_DATE_PATTERN);

    /**
     * 取号SPI
     */
    private final TodoSequenceProvider sequenceProvider;

    /**
     * 日期提供者，便于单测替换为固定日期
     */
    private final Supplier<LocalDate> dateSupplier;

    /**
     * 生产构造：注入取号 SPI，日期取系统当天。
     *
     * @param sequenceProvider 取号 SPI
     */
    @Autowired
    public TodoNoGeneratorImpl(TodoSequenceProvider sequenceProvider) {
        this(sequenceProvider, LocalDate::now);
    }

    /**
     * 全参构造，供单测使用。
     *
     * @param sequenceProvider 取号 SPI
     * @param dateSupplier     日期提供者
     */
    TodoNoGeneratorImpl(TodoSequenceProvider sequenceProvider, Supplier<LocalDate> dateSupplier) {
        this.sequenceProvider = sequenceProvider;
        this.dateSupplier = dateSupplier;
    }

    /**
     * 生成下一个待办编号。
     *
     * @return 待办编号，形如 TD20261029000042
     */
    @Override
    public String nextTodoNo() {
        String dateKey = DATE_FORMATTER.format(dateSupplier.get());
        long sequence = sequenceProvider.nextSequence(dateKey);
        long suffix = Math.abs(sequence % TodoConstants.TODO_NO_SEQ_MODULUS);
        return TodoConstants.TODO_NO_PREFIX + dateKey + padLeft(String.valueOf(suffix));
    }

    /**
     * 左补零至序号位数。
     *
     * @param value 序号文本
     * @return 定长序号文本
     */
    private String padLeft(String value) {
        if (value.length() >= TodoConstants.TODO_NO_SEQ_LENGTH) {
            return value;
        }
        StringBuilder builder = new StringBuilder(TodoConstants.TODO_NO_SEQ_LENGTH);
        for (int index = value.length(); index < TodoConstants.TODO_NO_SEQ_LENGTH; index++) {
            builder.append('0');
        }
        return builder.append(value).toString();
    }
}
