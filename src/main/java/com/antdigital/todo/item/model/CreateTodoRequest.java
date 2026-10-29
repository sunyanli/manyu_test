package com.antdigital.todo.item.model;

import com.antdigital.todo.item.constant.TodoConstants;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新增待办请求。
 *
 * <p>注解只做「必填 + 列宽上限」的防御性校验；业务长度（名称 1~100、描述 ≤500 字符）
 * 在规范化之后由 Service 判定，避免首尾空白影响业务语义。</p>
 *
 * @author AiWork
 */
public class CreateTodoRequest {

    /** 事项名称。 */
    @NotBlank(message = "事项名称不能为空")
    @Size(max = TodoConstants.TITLE_COLUMN_MAX_LEN, message = "事项名称不能超过100个字符")
    private String title;

    /** 事项描述，选填。 */
    @Size(max = TodoConstants.DESC_COLUMN_MAX_LEN, message = "事项描述不能超过500个字符")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
