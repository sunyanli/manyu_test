package com.antdigital.todo.item.controller;

import com.antdigital.todo.TodoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 新增待办端到端测试：真实 Spring 容器 + 真实 MyBatis-Plus + H2（MySQL 兼容模式）。
 *
 * <p>用于验证 INSERT 语句、主键回填与落库字段值，避免仅 Mock Mapper 无法覆盖 SQL 映射风险。</p>
 *
 * @author AiWork
 */
@SpringBootTest(classes = TodoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodoCreateIntegrationTest {

    private static final String USER_ID = "U1001";

    private static final String TENANT_ID = "T01";

    private static final String USER_NAME = "张三";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("端到端：请求落库成功，返回主键且库内字段正确")
    void should_persistTodoItem_when_createSucceeds() throws Exception {
        // Arrange
        String body = "{\"title\":\"  整理  季度汇报材料  \",\"description\":\"汇总三条业务线数据\"}";

        // Act
        String response = mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .header("X-User-Name", USER_NAME)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.msg").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("整理 季度汇报材料"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn().getResponse().getContentAsString();

        // Assert - 回查数据库验证真实落库结果
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.data.id");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT tenant_id, title, description, status, creator_id, creator_name, gmt_create "
                        + "FROM todo_item WHERE id = ?", id.longValue());
        assertThat(row.get("tenant_id")).isEqualTo(TENANT_ID);
        assertThat(row.get("title")).isEqualTo("整理 季度汇报材料");
        assertThat(row.get("description")).isEqualTo("汇总三条业务线数据");
        assertThat(((Number) row.get("status")).intValue()).isZero();
        assertThat(row.get("creator_id")).isEqualTo(USER_ID);
        assertThat(row.get("creator_name")).isEqualTo(USER_NAME);
        assertThat(row.get("gmt_create")).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM todo_item", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("端到端：描述缺省时按空串落库")
    void should_storeEmptyDescription_when_descriptionAbsent() throws Exception {
        // Arrange
        String body = "{\"title\":\"买咖啡豆\"}";

        // Act
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"));

        // Assert
        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM todo_item WHERE title = ?", String.class, "买咖啡豆");
        assertThat(description).isEmpty();
    }
}
