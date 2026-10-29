package com.antdigital.todo.service.component;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.enums.TodoSourceEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.util.TodoTextUtil;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.ValidatedTodo;
import org.springframework.stereotype.Component;

/**
 * 入参校验与规范化（S03），执行系分规则 R01~R04。
 *
 * <p>约定了「不静默截断」语义：名称/描述超长直接报错，避免用户内容被悄悄丢弃。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Component
public class TodoValidator {

    /**
     * 幂等键允许的字符集：字母、数字、连字符、下划线
     */
    private static final String REQUEST_ID_PATTERN = "^[A-Za-z0-9_-]+$";

    /**
     * 校验并规范化创建入参。
     *
     * @param request 原始入参
     * @return 规范化后的待办数据
     * @throws TodoBizException 违反 R01~R04 时抛出对应错误码
     */
    public ValidatedTodo validateAndNormalize(TodoCreateRequest request) {
        if (request == null) {
            throw new TodoBizException(TodoErrorCodeEnum.PARAM_INVALID);
        }
        String requestId = normalizeRequestId(request.getRequestId());
        String itemName = validateItemName(request.getItemName());
        String itemDesc = validateItemDesc(request.getItemDesc());
        String source = resolveSource(request.getSource());
        return new ValidatedTodo(requestId, itemName, itemDesc, source);
    }

    /**
     * R01：幂等键必填，长度 1~64，仅允许字母、数字、连字符与下划线。
     *
     * @param requestId 原始幂等键
     * @return 去除首尾空白后的幂等键
     * @throws TodoBizException 不满足 R01 时抛出 TODO_0001
     */
    public String normalizeRequestId(String requestId) {
        String trimmed = TodoTextUtil.trimWhitespace(requestId);
        if (trimmed.isEmpty()
                || TodoTextUtil.lengthOf(trimmed) > TodoConstants.MAX_REQUEST_ID_LENGTH
                || !trimmed.matches(REQUEST_ID_PATTERN)) {
            throw new TodoBizException(TodoErrorCodeEnum.PARAM_INVALID);
        }
        return trimmed;
    }

    /**
     * R02/R04：名称必填，NFC 规范化并剔除控制字符后，去除首尾空白长度需为 1~128。
     *
     * @param itemName 原始名称
     * @return 规范化后的名称
     */
    private String validateItemName(String itemName) {
        String normalized = TodoTextUtil.trimWhitespace(TodoTextUtil.normalize(itemName));
        if (normalized.isEmpty()) {
            throw new TodoBizException(TodoErrorCodeEnum.ITEM_NAME_REQUIRED);
        }
        if (TodoTextUtil.lengthOf(normalized) > TodoConstants.MAX_ITEM_NAME_LENGTH) {
            throw new TodoBizException(TodoErrorCodeEnum.ITEM_NAME_TOO_LONG);
        }
        return normalized;
    }

    /**
     * R03/R04：描述选填，NFC 规范化并剔除控制字符后长度需为 0~512，超长不截断。
     *
     * @param itemDesc 原始描述
     * @return 规范化后的描述，缺省为空串
     */
    private String validateItemDesc(String itemDesc) {
        String normalized = TodoTextUtil.normalize(itemDesc);
        if (TodoTextUtil.lengthOf(normalized) > TodoConstants.MAX_ITEM_DESC_LENGTH) {
            throw new TodoBizException(TodoErrorCodeEnum.ITEM_DESC_TOO_LONG);
        }
        return normalized;
    }

    /**
     * 解析创建来源，缺省为 web，取值非法时报参数错误。
     *
     * @param source 原始来源
     * @return 规范化后的来源编码
     */
    private String resolveSource(String source) {
        String trimmed = TodoTextUtil.trimWhitespace(source);
        if (trimmed.isEmpty()) {
            return TodoConstants.DEFAULT_SOURCE;
        }
        return TodoSourceEnum.of(trimmed).getCode();
    }
}
