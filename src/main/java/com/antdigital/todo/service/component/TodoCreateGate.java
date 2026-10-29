package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.util.TodoTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 创建功能发布开关（G17.1/G15.3/G12.2）：总开关 + 租户尾号灰度。
 *
 * <p>对应系分 §7.2 灰度与 §7.3 一级应急：关闭 {@code todo.create.enabled} 即可止血，
 * 无需回滚发布包；{@code todo.create.gray.tenants} 按租户 ID 尾号白名单放量，
 * 留空表示全量开放。未开放统一返回 TODO_0010，不以限流错误码混淆语义。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoCreateGate {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoCreateGate.class);

    /**
     * 灰度尾号匹配的最小长度
     */
    private static final int GRAY_TAIL_LENGTH = 1;

    /**
     * 创建功能总开关
     */
    private final boolean createEnabled;

    /**
     * 灰度租户尾号白名单，空集合表示全量开放
     */
    private final Set<String> grayTails;

    /**
     * 构造注入开关配置。
     *
     * @param createEnabled 创建功能总开关
     * @param grayTenants   灰度租户尾号，逗号分隔，可为空
     */
    public TodoCreateGate(@Value("${todo.create.enabled:true}") boolean createEnabled,
                          @Value("${todo.create.gray.tenants:}") String grayTenants) {
        this.createEnabled = createEnabled;
        this.grayTails = parseGrayTails(grayTenants);
        LOGGER.info("todo create gate initialized, enabled={}, grayTails={}", createEnabled, grayTails);
    }

    /**
     * 校验当前租户是否允许创建待办。
     *
     * @param tenantId 可信租户标识
     * @throws TodoBizException 总开关关闭或租户未命中灰度时抛出 TODO_0010
     */
    public void check(String tenantId) {
        if (!createEnabled) {
            LOGGER.warn("todo create feature disabled by switch");
            throw new TodoBizException(TodoErrorCodeEnum.FEATURE_DISABLED);
        }
        if (grayTails.isEmpty()) {
            return;
        }
        String tail = resolveTenantTail(tenantId);
        if (!grayTails.contains(tail)) {
            LOGGER.info("tenant not in gray scope, tenantTail={}", tail);
            throw new TodoBizException(TodoErrorCodeEnum.FEATURE_DISABLED);
        }
    }

    /**
     * 解析灰度尾号配置。
     *
     * @param grayTenants 逗号分隔的尾号配置
     * @return 尾号集合，无法解析时为空集合（等价全量开放）
     */
    private Set<String> parseGrayTails(String grayTenants) {
        String trimmed = TodoTextUtil.trimWhitespace(grayTenants);
        if (trimmed.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> tails = new LinkedHashSet<>();
        String[] parts = trimmed.split(TodoConstants.GRAY_TAIL_SEPARATOR);
        for (String part : parts) {
            String tail = TodoTextUtil.trimWhitespace(part);
            if (tail.length() == GRAY_TAIL_LENGTH) {
                tails.add(tail);
            }
        }
        if (tails.isEmpty()) {
            LOGGER.error("todo create gray tenants config is invalid, treat as full release. value={}", grayTenants);
        }
        return tails;
    }

    /**
     * 取租户标识尾号，与系分「按租户尾号灰度」一致。
     *
     * @param tenantId 租户标识
     * @return 尾号字符，租户为空时返回空串
     */
    private String resolveTenantTail(String tenantId) {
        String trimmed = TodoTextUtil.trimWhitespace(tenantId);
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.substring(trimmed.length() - GRAY_TAIL_LENGTH);
    }
}
