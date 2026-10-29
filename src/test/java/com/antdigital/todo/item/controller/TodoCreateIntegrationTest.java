package com.antdigital.todo.item.controller;

import com.antdigital.todo.TodoApplication;
import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.metrics.TodoCreateMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 新增待办端到端测试：真实 Spring 容器 + 真实 MyBatis-Plus + H2（MySQL 兼容模式）。
 *
 * <p>用于验证 INSERT 语句、主键回填与落库字段值，避免仅 Mock Mapper 无法覆盖
 * SQL 映射风险。</p>
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

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private TodoCreateMetrics todoCreateMetrics;

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

    @Test
    @DisplayName("端到端：emoji 名称按 4 字节字符完整落库")
    void should_persistEmoji_when_titleContainsEmoji() throws Exception {
        // Arrange
        String title = "整理材料🚀";
        String body = "{\"title\":\"" + title + "\"}";

        // Act
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"));

        // Assert
        String storedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM todo_item WHERE creator_id = ?", String.class, USER_ID);
        assertThat(storedTitle).isEqualTo(title);
    }

    @Test
    @DisplayName("端到端：缺失 X-Trace-Id 时由应用生成并回写响应头")
    void should_generateTraceId_when_headerAbsent() throws Exception {
        // Arrange
        String body = "{\"title\":\"买牛奶\"}";

        // Act
        String traceId = mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Trace-Id");

        // Assert
        assertThat(traceId).isNotBlank().hasSize(32);
    }

    @Test
    @DisplayName("端到端：非受信来源 IP 被拒绝并返回 TODO_002")
    void should_returnTodo002_when_sourceIpNotTrusted() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.9");
                            return request;
                        })
                        .content("{\"title\":\"买牛奶\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("TODO_002"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM todo_item", Integer.class)).isZero();
    }

    @Test
    @DisplayName("端到端：运行期关闭开关立即生效，拒绝后不残留操作人上下文")
    void should_rejectAndClearContext_when_switchOffAtRuntime() throws Exception {
        // Arrange：不重启，直接向 Environment 注入新值（等价配置中心推送）
        Map<String, Object> toggle = new HashMap<String, Object>();
        toggle.put("todo.create.enabled", "false");
        PropertySource<?> source = new MapPropertySource("test-runtime-toggle", toggle);
        environment.getPropertySources().addFirst(source);
        try {
            // Act & Assert
            mockMvc.perform(post("/api/todo/item/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .content("{\"title\":\"买牛奶\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("TODO_007"));
            assertThat(OperatorContextHolder.get()).isNull();
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM todo_item", Integer.class)).isZero();
        } finally {
            environment.getPropertySources().remove("test-runtime-toggle");
        }
    }

    @Test
    @DisplayName("端到端：成功创建后记录 OK 调用量与耗时指标")
    void should_recordMetrics_when_createSucceeds() throws Exception {
        // Arrange
        double before = todoCreateMetrics.countOf(ResultCode.OK.getCode());
        String body = "{\"title\":\"写周报\"}";

        // Act
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"));

        // Assert
        assertThat(todoCreateMetrics.countOf(ResultCode.OK.getCode())).isEqualTo(before + 1.0d);
        assertThat(todoCreateMetrics.costTimer().count()).isPositive();
    }
}
