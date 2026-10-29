package com.antdigital.todo.item.controller;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.context.OperatorContextHolder;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.exception.GlobalExceptionHandler;
import com.antdigital.todo.item.model.CreateTodoRequest;
import com.antdigital.todo.item.model.CreateTodoResponse;
import com.antdigital.todo.item.service.TodoItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link TodoItemController} 切片测试（standalone MockMvc，不启动 Spring 容器）。
 *
 * @author AiWork
 */
@ExtendWith(MockitoExtension.class)
class TodoItemControllerTest {

    private static final long GENERATED_ID = 102401L;

    @Mock
    private TodoItemService todoItemService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TodoItemController controller = new TodoItemController(todoItemService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        OperatorContextHolder.set(new OperatorContext("U1001", "张三", "T01"));
    }

    @AfterEach
    void tearDown() {
        OperatorContextHolder.clear();
    }

    @Test
    @DisplayName("正常路径：返回 result=OK 与新建记录信息")
    void should_returnOk_when_requestIsValid() throws Exception {
        // Arrange
        CreateTodoResponse response = new CreateTodoResponse();
        response.setId(GENERATED_ID);
        response.setTitle("整理季度汇报材料");
        response.setStatus(0);
        response.setGmtCreate("2026-10-29 09:35:12");
        when(todoItemService.createTodo(any(CreateTodoRequest.class), any(OperatorContext.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"整理季度汇报材料\",\"description\":\"周三前发评审\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.msg").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(GENERATED_ID))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.gmtCreate").value("2026-10-29 09:35:12"));
    }

    @Test
    @DisplayName("名称缺失：返回 TODO_001 且不进入业务层")
    void should_returnTodo001_when_titleMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"只有描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(ResultCode.TODO_001.getCode()))
                .andExpect(jsonPath("$.msg").value("事项名称不能为空"))
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(todoItemService, never()).createTodo(any(CreateTodoRequest.class), any(OperatorContext.class));
    }

    @Test
    @DisplayName("请求体非 JSON：返回 TODO_001")
    void should_returnTodo001_when_bodyNotJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(ResultCode.TODO_001.getCode()));
    }

    @Test
    @DisplayName("业务异常：透传异常自身结果码")
    void should_returnBizCode_when_serviceThrows() throws Exception {
        // Arrange
        when(todoItemService.createTodo(any(CreateTodoRequest.class), any(OperatorContext.class)))
                .thenThrow(new BizException(ResultCode.TODO_005));

        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"待办\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(ResultCode.TODO_005.getCode()))
                .andExpect(jsonPath("$.msg").value(ResultCode.TODO_005.getMessage()));
    }

    @Test
    @DisplayName("登录态缺失：Controller 兜底返回 TODO_002")
    void should_returnTodo002_when_operatorMissing() throws Exception {
        // Arrange
        OperatorContextHolder.clear();

        // Act & Assert
        mockMvc.perform(post("/api/todo/item/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"待办\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(ResultCode.TODO_002.getCode()));
    }
}
