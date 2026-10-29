package com.antdigital.todo.service.component;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.ValidatedTodo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoValidator} 入参校验与规范化单元测试，覆盖系分规则 R01~R04。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoValidatorTest {

    private static final String VALID_REQUEST_ID = "8f2b1c34-5d6e-4a7b-9c8d-0e1f2a3b4c5d";

    private static final String VALID_NAME = "整理季度经营分析材料";

    private static final String EMOJI = "\uD83D\uDE00";

    private final TodoValidator todoValidator = new TodoValidator();

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("正常入参：返回规范化后的名称、描述与来源")
    void should_returnNormalizedTodo_when_validRequest() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, VALID_NAME, "汇总 Q3 数据");

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getRequestId()).isEqualTo(VALID_REQUEST_ID);
        assertThat(validated.getItemName()).isEqualTo(VALID_NAME);
        assertThat(validated.getItemDesc()).isEqualTo("汇总 Q3 数据");
        assertThat(validated.getSource()).isEqualTo("web");
    }

    @Test
    @DisplayName("名称首尾空白（含全角空格）被去除")
    void should_trimName_when_nameHasLeadingAndTrailingSpaces() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, "　 整理周报 \t", null);

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getItemName()).isEqualTo("整理周报");
    }

    @Test
    @DisplayName("控制字符被剔除，换行与制表符保留")
    void should_removeControlCharsAndKeepNewline_when_descHasControlChars() {
        // Arrange
        TodoCreateRequest request =
                buildRequest(VALID_REQUEST_ID, "整理周报", "第一行\n第二行\t结束\u0000");

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getItemDesc()).isEqualTo("第一行\n第二行\t结束");
    }

    @Test
    @DisplayName("描述缺省时规范化为空串")
    void should_returnEmptyDesc_when_descIsNull() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, "整理周报", null);

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getItemDesc()).isEmpty();
    }

    @Test
    @DisplayName("名称长度按字符计：128 个 emoji 合法")
    void should_acceptName_when_128EmojiChars() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, repeat(EMOJI, 128), null);

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getItemName()).isEqualTo(repeat(EMOJI, 128));
    }

    @Test
    @DisplayName("描述长度按字符计：512 字符合法")
    void should_acceptDesc_when_512Chars() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, VALID_NAME, repeat("描", 512));

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getItemDesc()).hasSize(512);
    }

    @Test
    @DisplayName("来源取值大小写不敏感，统一归一为小写编码")
    void should_normalizeSource_when_sourceIsUpperCase() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, VALID_NAME, null);
        request.setSource("API");

        // Act
        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        // Assert
        assertThat(validated.getSource()).isEqualTo("api");
    }

    // ==================== 参数校验（R01） ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "req 1", "req#1",
            "invalid-request-id-which-is-way-too-long-to-be-accepted-by-the-validator-0001"})
    @DisplayName("幂等键缺失、含非法字符或超长时返回 TODO_0001")
    void should_throwParamInvalid_when_requestIdIllegal(String requestId) {
        // Arrange
        TodoCreateRequest request = buildRequest(requestId, VALID_NAME, null);

        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(request))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.PARAM_INVALID.getCode()));
    }

    @Test
    @DisplayName("请求体为 null 时返回 TODO_0001")
    void should_throwParamInvalid_when_requestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(null))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.PARAM_INVALID.getCode()));
    }

    // ==================== 名称校验（R02） ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "　　", "\u0000\u0001"})
    @DisplayName("名称去空白且剔除控制字符后为空时返回 TODO_0002")
    void should_throwItemNameRequired_when_nameIsBlank(String itemName) {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, itemName, null);

        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(request))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.ITEM_NAME_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("名称超过 128 字符时返回 TODO_0003，不做静默截断")
    void should_throwItemNameTooLong_when_nameOverLimit() {
        // Arrange
        String tooLongName = repeat("周", 129);
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, tooLongName, null);

        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(request))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.ITEM_NAME_TOO_LONG.getCode()));
    }

    // ==================== 描述校验（R03） ====================

    @Test
    @DisplayName("描述超过 512 字符时返回 TODO_0004，不做静默截断")
    void should_throwItemDescTooLong_when_descOverLimit() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, VALID_NAME, repeat("描", 513));

        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(request))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.ITEM_DESC_TOO_LONG.getCode()));
    }

    @Test
    @DisplayName("来源取值非法时返回 TODO_0001")
    void should_throwParamInvalid_when_sourceIllegal() {
        // Arrange
        TodoCreateRequest request = buildRequest(VALID_REQUEST_ID, VALID_NAME, null);
        request.setSource("wechat");

        // Act & Assert
        assertThatThrownBy(() -> todoValidator.validateAndNormalize(request))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.PARAM_INVALID.getCode()));
    }

    // ==================== 测试数据构造 ====================

    private TodoCreateRequest buildRequest(String requestId, String itemName, String itemDesc) {
        TodoCreateRequest request = new TodoCreateRequest();
        request.setRequestId(requestId);
        request.setItemName(itemName);
        request.setItemDesc(itemDesc);
        return request;
    }

    private static String repeat(String unit, int times) {
        StringBuilder builder = new StringBuilder(unit.length() * times);
        for (int index = 0; index < times; index++) {
            builder.append(unit);
        }
        return builder.toString();
    }
}
