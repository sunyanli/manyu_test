package com.antdigital.todo.model.dto;

import javax.validation.constraints.NotBlank;

/**
 * 创建待办请求体（W01 入参），对应系分 5.1.2 入参表。
 *
 * <p>身份字段（creatorId/creatorName/tenantId）不由请求体传入，统一取自网关注入的登录上下文。
 *
 * <p>校验分工：注解负责「非空」这类与框架无关的快速失败；长度按字符（code point）计，
 * 注解按 UTF-16 长度统计会误杀 emoji，故长度与字符集统一由 TodoValidator 校验（R01~R04）。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoCreateRequest {

    /**
     * 幂等键，前端每次表单提交生成一次，重试沿用同一值
     */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 事项名称，原始值，由校验层做规范化与长度校验
     */
    @NotBlank(message = "itemName 不能为空")
    private String itemName;

    /**
     * 事项描述，原始值，选填
     */
    private String itemDesc;

    /**
     * 创建来源，缺省 web
     */
    private String source;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 输出字段快照，名称与描述不打印原文，避免敏感信息落日志。
     *
     * @return 字段快照
     */
    @Override
    public String toString() {
        return "TodoCreateRequest{requestId='" + requestId + '\''
                + ", itemName='(hidden)'"
                + ", itemDesc='(hidden)'"
                + ", source='" + source + '\''
                + '}';
    }
}
