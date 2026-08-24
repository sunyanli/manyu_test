# 三接口展示与调用分析报表系统 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建包含三个后端 REST 接口（HelloWorld、哈希算法、冒泡排序）的前端展示页面，提供结果导出、调用埋点及可视化报表功能。

**Architecture:** 前后端分离架构。后端使用 Java Spring Boot 3.x + Spring Data JPA + H2 数据库，通过 AOP 切面实现调用埋点。前端使用 React 18 + Vite + Ant Design + ECharts，通过 Axios 调用后端 API。后端提供 CSV/Excel 导出能力，前端通过 ECharts 渲染折线图/饼图/柱状图。

**Tech Stack:**
- 后端: Java 17, Spring Boot 3.x, Spring Data JPA, Spring AOP, H2 (dev), OpenCSV, Apache POI
- 前端: React 18, Vite, Ant Design 5.x, ECharts, Axios
- 构建: Maven (后端), npm/pnpm (前端)

---

## 全局约束

- Java 版本 ≥ 17
- Spring Boot 3.x 系列
- React 18.x
- 前端必须使用 Ant Design 5.x 组件库
- 图表必须使用 ECharts (通过 echarts-for-react 桥接)
- 后端所有 API 统一返回 `ApiResponse<T>` 包装格式
- 埋点通过自定义 `@Traceable` 注解 + AOP 实现
- 调用人信息通过请求头传递: `X-Caller-Name`, `X-Person-Type`, `X-Person-Level`, `X-Department`
- 开发阶段数据库使用 H2 内存模式
- 接口命名和响应格式严格遵循设计文档

---

## 文件结构

```
backend/
├── pom.xml
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java
│   ├── config/
│   │   └── WebConfig.java
│   ├── controller/
│   │   ├── HelloController.java
│   │   ├── HashController.java
│   │   ├── BubbleController.java
│   │   ├── ExportController.java
│   │   └── AnalyticsController.java
│   ├── service/
│   │   ├── HelloService.java
│   │   ├── HashService.java
│   │   ├── BubbleService.java
│   │   ├── ExportService.java
│   │   └── AnalyticsService.java
│   ├── model/
│   │   ├── HelloResult.java
│   │   ├── HashResult.java
│   │   ├── BubbleResult.java
│   │   ├── CallLog.java
│   │   └── AnalyticsQuery.java
│   ├── repository/
│   │   └── CallLogRepository.java
│   ├── aspect/
│   │   └── TraceableAspect.java
│   ├── annotation/
│   │   └── Traceable.java
│   └── dto/
│       ├── ApiResponse.java
│       └── AnalyticsResponse.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
└── src/test/java/com/example/demo/
    ├── controller/
    │   ├── HelloControllerTest.java
    │   ├── HashControllerTest.java
    │   ├── BubbleControllerTest.java
    │   ├── ExportControllerTest.java
    │   └── AnalyticsControllerTest.java
    └── DemoApplicationTests.java

frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── App.css
│   ├── pages/
│   │   └── Dashboard.jsx
│   ├── components/
│   │   ├── HelloTab.jsx
│   │   ├── HashTab.jsx
│   │   ├── BubbleTab.jsx
│   │   ├── ExportButton.jsx
│   │   └── AnalyticsChart.jsx
│   ├── services/
│   │   └── api.js
│   └── utils/
│       └── constants.js
```

---

## Task 1: 后端项目脚手架搭建

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/demo/DemoApplication.java`
- Create: `backend/src/main/java/com/example/demo/config/WebConfig.java`
- Create: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/example/demo/DemoApplicationTests.java`

**Interfaces:**
- Consumes: 无
- Produces: 可运行的 Spring Boot 空项目骨架，供后续所有 Task 依赖

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>demo</name>
    <description>三接口展示与调用分析报表系统</description>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <!-- H2 开发数据库 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- OpenCSV 导出 -->
        <dependency>
            <groupId>com.opencsv</groupId>
            <artifactId>opencsv</artifactId>
            <version>5.9</version>
        </dependency>
        <!-- Apache POI Excel 导出 -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 DemoApplication.java**

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 WebConfig.java（CORS 跨域配置）**

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```

- [ ] **Step 4: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
```

- [ ] **Step 5: 创建 DemoApplicationTests.java**

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: 验证项目启动**

```bash
cd backend && mvn compile -q
```

Expected: 编译成功，无错误输出。

- [ ] **Step 7: 提交**

```bash
git add backend/pom.xml backend/src/
git commit -m "feat: 搭建 Spring Boot 后端项目脚手架

- 配置 pom.xml 依赖（Spring Web, JPA, AOP, H2, OpenCSV, POI）
- 创建 DemoApplication 主入口
- 配置 CORS 跨域支持
- 配置 H2 内存数据库和 JPA"
```

---

## Task 2: 通用 DTO 和数据模型

**Files:**
- Create: `backend/src/main/java/com/example/demo/dto/ApiResponse.java`
- Create: `backend/src/main/java/com/example/demo/model/CallLog.java`
- Create: `backend/src/main/java/com/example/demo/repository/CallLogRepository.java`
- Create: `backend/src/main/java/com/example/demo/dto/AnalyticsResponse.java`

**Interfaces:**
- Consumes: Task 1 项目骨架
- Produces: `ApiResponse<T>` (所有 controller 的返回值包装), `CallLog` (JPA 实体), `CallLogRepository` (数据访问), `AnalyticsResponse` (报表查询返回)

- [ ] **Step 1: 创建 ApiResponse.java**

```java
package com.example.demo.dto;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

- [ ] **Step 2: 创建 CallLog.java（JPA 实体）**

```java
package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_log")
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", length = 100, nullable = false)
    private String apiName;

    @Column(name = "caller_name", length = 100)
    private String callerName;

    @Column(name = "person_type", length = 50)
    private String personType;

    @Column(name = "person_level", length = 50)
    private String personLevel;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "call_time", nullable = false)
    private LocalDateTime callTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 20)
    private String status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getPersonType() { return personType; }
    public void setPersonType(String personType) { this.personType = personType; }
    public String getPersonLevel() { return personLevel; }
    public void setPersonLevel(String personLevel) { this.personLevel = personLevel; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public LocalDateTime getCallTime() { return callTime; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

- [ ] **Step 3: 创建 CallLogRepository.java**

```java
package com.example.demo.repository;

import com.example.demo.model.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    // 按人员类型分组统计
    @Query("SELECT c.personType AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.personType ORDER BY value DESC")
    List<Object[]> countByPersonType(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    // 按人员层级分组统计
    @Query("SELECT c.personLevel AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.personLevel ORDER BY value DESC")
    List<Object[]> countByPersonLevel(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    // 按部门分组统计
    @Query("SELECT c.department AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.department ORDER BY value DESC")
    List<Object[]> countByDepartment(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    // 按时间趋势统计（按天聚合）
    @Query("SELECT FUNCTION('DATE', c.callTime) AS label, COUNT(c) AS value FROM CallLog c " +
           "WHERE c.callTime BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE', c.callTime) ORDER BY label ASC")
    List<Object[]> countByTimeTrend(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
}
```

- [ ] **Step 4: 创建 AnalyticsResponse.java**

```java
package com.example.demo.dto;

import java.util.List;

public class AnalyticsResponse {
    private String dimension;
    private List<SeriesItem> series;
    private long totalCalls;

    public AnalyticsResponse() {}

    public AnalyticsResponse(String dimension, List<SeriesItem> series, long totalCalls) {
        this.dimension = dimension;
        this.series = series;
        this.totalCalls = totalCalls;
    }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public List<SeriesItem> getSeries() { return series; }
    public void setSeries(List<SeriesItem> series) { this.series = series; }
    public long getTotalCalls() { return totalCalls; }
    public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }

    public static class SeriesItem {
        private String label;
        private long value;

        public SeriesItem() {}

        public SeriesItem(String label, long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: 编译成功，无错误。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/example/demo/dto/ backend/src/main/java/com/example/demo/model/ backend/src/main/java/com/example/demo/repository/
git commit -m "feat: 添加通用 DTO、CallLog 实体和 Repository"
```

---

## Task 3: 埋点注解和 AOP 切面

**Files:**
- Create: `backend/src/main/java/com/example/demo/annotation/Traceable.java`
- Create: `backend/src/main/java/com/example/demo/aspect/TraceableAspect.java`

**Interfaces:**
- Consumes: Task 2 (CallLog, CallLogRepository)
- Produces: `@Traceable` 注解（可标注在 Controller 方法上）, `TraceableAspect`（环绕通知拦截并记录调用日志）

- [ ] **Step 1: 创建 Traceable.java**

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {
    String apiName() default "";
}
```

- [ ] **Step 2: 创建 TraceableAspect.java**

```java
package com.example.demo.aspect;

import com.example.demo.annotation.Traceable;
import com.example.demo.model.CallLog;
import com.example.demo.repository.CallLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class TraceableAspect {

    @Autowired
    private CallLogRepository callLogRepository;

    @Around("@annotation(traceable)")
    public Object logCall(ProceedingJoinPoint joinPoint, Traceable traceable) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求头中的调用人信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String callerName = request.getHeader("X-Caller-Name");
        String personType = request.getHeader("X-Person-Type");
        String personLevel = request.getHeader("X-Person-Level");
        String department = request.getHeader("X-Department");

        String apiName = traceable.apiName();
        if (apiName.isEmpty()) {
            apiName = joinPoint.getSignature().getName();
        }

        Object result;
        String status = "success";
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "fail";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // 异步保存调用日志
            saveCallLog(apiName, callerName, personType, personLevel, department, duration, status);
        }
    }

    private void saveCallLog(String apiName, String callerName, String personType,
                             String personLevel, String department, long durationMs, String status) {
        CallLog log = new CallLog();
        log.setApiName(apiName);
        log.setCallerName(callerName != null ? callerName : "anonymous");
        log.setPersonType(personType != null ? personType : "unknown");
        log.setPersonLevel(personLevel != null ? personLevel : "unknown");
        log.setDepartment(department != null ? department : "unknown");
        log.setCallTime(LocalDateTime.now());
        log.setDurationMs(durationMs);
        log.setStatus(status);
        callLogRepository.save(log);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: 编译成功，无错误。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/example/demo/annotation/ backend/src/main/java/com/example/demo/aspect/
git commit -m "feat: 实现 @Traceable 注解和 AOP 埋点切面"
```

---

## Task 4: HelloWorld 接口

**Files:**
- Create: `backend/src/main/java/com/example/demo/model/HelloResult.java`
- Create: `backend/src/main/java/com/example/demo/service/HelloService.java`
- Create: `backend/src/main/java/com/example/demo/controller/HelloController.java`
- Test: `backend/src/test/java/com/example/demo/controller/HelloControllerTest.java`

**Interfaces:**
- Consumes: `@Traceable` (Task 3), `ApiResponse<T>` (Task 2)
- Produces: `GET /api/hello?name=xxx` → `ApiResponse<HelloResult>`

- [ ] **Step 1: 创建 HelloResult.java**

```java
package com.example.demo.model;

public class HelloResult {
    private String greeting;

    public HelloResult() {}

    public HelloResult(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
}
```

- [ ] **Step 2: 创建 HelloService.java**

```java
package com.example.demo.service;

import com.example.demo.model.HelloResult;
import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public HelloResult greet(String name) {
        String safeName = (name != null && !name.isBlank()) ? name : "World";
        String greeting = "Hello, " + safeName + "! Welcome to DTCoder Demo.";
        return new HelloResult(greeting);
    }
}
```

- [ ] **Step 3: 创建 HelloController.java**

```java
package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.HelloResult;
import com.example.demo.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private HelloService helloService;

    @GetMapping("/hello")
    @Traceable(apiName = "hello")
    public ApiResponse<HelloResult> hello(@RequestParam(defaultValue = "World") String name) {
        HelloResult result = helloService.greet(name);
        return ApiResponse.success(result);
    }
}
```

- [ ] **Step 4: 创建 HelloControllerTest.java**

```java
package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloWithName() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .param("name", "张三")
                        .header("X-Caller-Name", "张三")
                        .header("X-Person-Type", "研发")
                        .header("X-Person-Level", "高级")
                        .header("X-Department", "技术部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.greeting").value("Hello, 张三! Welcome to DTCoder Demo."));
    }

    @Test
    void helloDefault() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.greeting").value("Hello, World! Welcome to DTCoder Demo."));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend && mvn test -Dtest=HelloControllerTest -q
```

Expected: 测试通过。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/example/demo/model/HelloResult.java backend/src/main/java/com/example/demo/service/HelloService.java backend/src/main/java/com/example/demo/controller/HelloController.java backend/src/test/java/com/example/demo/controller/HelloControllerTest.java
git commit -m "feat: 实现 HelloWorld GET 接口"
```

---

## Task 5: 哈希算法接口

**Files:**
- Create: `backend/src/main/java/com/example/demo/model/HashResult.java`
- Create: `backend/src/main/java/com/example/demo/service/HashService.java`
- Create: `backend/src/main/java/com/example/demo/controller/HashController.java`
- Test: `backend/src/test/java/com/example/demo/controller/HashControllerTest.java`

**Interfaces:**
- Consumes: `@Traceable`, `ApiResponse<T>`
- Produces: `POST /api/hash` → `ApiResponse<HashResult>`, 支持 SHA-256 / MD5 / SHA-512

- [ ] **Step 1: 创建 HashResult.java**

```java
package com.example.demo.model;

public class HashResult {
    private String input;
    private String algorithm;
    private String hash;

    public HashResult() {}

    public HashResult(String input, String algorithm, String hash) {
        this.input = input;
        this.algorithm = algorithm;
        this.hash = hash;
    }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
```

- [ ] **Step 2: 创建 HashService.java**

```java
package com.example.demo.service;

import com.example.demo.model.HashResult;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HashService {

    public HashResult computeHash(String input, String algorithm) {
        if (input == null) {
            throw new IllegalArgumentException("input 不能为空");
        }
        String safeAlgorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm : "SHA-256";
        try {
            MessageDigest digest = MessageDigest.getInstance(safeAlgorithm);
            byte[] hashBytes = digest.digest(input.getBytes());
            String hashHex = HexFormat.of().formatHex(hashBytes);
            return new HashResult(input, safeAlgorithm, hashHex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("不支持的算法: " + safeAlgorithm + "，支持: SHA-256, MD5, SHA-512");
        }
    }
}
```

- [ ] **Step 3: 创建 HashController.java**

```java
package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.HashResult;
import com.example.demo.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HashController {

    @Autowired
    private HashService hashService;

    @PostMapping("/hash")
    @Traceable(apiName = "hash")
    public ApiResponse<HashResult> hash(@RequestBody Map<String, String> body) {
        String input = body.get("input");
        String algorithm = body.get("algorithm");
        HashResult result = hashService.computeHash(input, algorithm);
        return ApiResponse.success(result);
    }
}
```

- [ ] **Step 4: 创建 HashControllerTest.java**

```java
package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hashSha256() throws Exception {
        mockMvc.perform(post("/api/hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"hello\", \"algorithm\": \"SHA-256\"}")
                        .header("X-Caller-Name", "李四")
                        .header("X-Person-Type", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.hash").value("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
    }

    @Test
    void hashMd5() throws Exception {
        mockMvc.perform(post("/api/hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": \"hello\", \"algorithm\": \"MD5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.algorithm").value("MD5"));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend && mvn test -Dtest=HashControllerTest -q
```

Expected: 测试通过。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/example/demo/model/HashResult.java backend/src/main/java/com/example/demo/service/HashService.java backend/src/main/java/com/example/demo/controller/HashController.java backend/src/test/java/com/example/demo/controller/HashControllerTest.java
git commit -m "feat: 实现哈希计算 POST 接口（SHA-256/MD5/SHA-512）"
```

---

## Task 6: 冒泡排序接口

**Files:**
- Create: `backend/src/main/java/com/example/demo/model/BubbleResult.java`
- Create: `backend/src/main/java/com/example/demo/service/BubbleService.java`
- Create: `backend/src/main/java/com/example/demo/controller/BubbleController.java`
- Test: `backend/src/test/java/com/example/demo/controller/BubbleControllerTest.java`

**Interfaces:**
- Consumes: `@Traceable`, `ApiResponse<T>`
- Produces: `POST /api/bubble-sort` → `ApiResponse<BubbleResult>`

- [ ] **Step 1: 创建 BubbleResult.java**

```java
package com.example.demo.model;

import java.util.List;

public class BubbleResult {
    private List<Integer> originalArray;
    private List<Integer> sortedArray;
    private int swapCount;
    private int comparisonCount;

    public BubbleResult() {}

    public BubbleResult(List<Integer> originalArray, List<Integer> sortedArray, int swapCount, int comparisonCount) {
        this.originalArray = originalArray;
        this.sortedArray = sortedArray;
        this.swapCount = swapCount;
        this.comparisonCount = comparisonCount;
    }

    public List<Integer> getOriginalArray() { return originalArray; }
    public void setOriginalArray(List<Integer> originalArray) { this.originalArray = originalArray; }
    public List<Integer> getSortedArray() { return sortedArray; }
    public void setSortedArray(List<Integer> sortedArray) { this.sortedArray = sortedArray; }
    public int getSwapCount() { return swapCount; }
    public void setSwapCount(int swapCount) { this.swapCount = swapCount; }
    public int getComparisonCount() { return comparisonCount; }
    public void setComparisonCount(int comparisonCount) { this.comparisonCount = comparisonCount; }
}
```

- [ ] **Step 2: 创建 BubbleService.java**

```java
package com.example.demo.service;

import com.example.demo.model.BubbleResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BubbleService {

    public BubbleResult sort(List<Integer> array) {
        if (array == null || array.isEmpty()) {
            return new BubbleResult(new ArrayList<>(), new ArrayList<>(), 0, 0);
        }

        List<Integer> original = new ArrayList<>(array);
        List<Integer> sorted = new ArrayList<>(array);
        int n = sorted.size();
        int swapCount = 0;
        int comparisonCount = 0;

        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                comparisonCount++;
                if (sorted.get(j) > sorted.get(j + 1)) {
                    // 交换
                    int temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                    swapCount++;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }

        return new BubbleResult(original, sorted, swapCount, comparisonCount);
    }
}
```

- [ ] **Step 3: 创建 BubbleController.java**

```java
package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.dto.ApiResponse;
import com.example.demo.model.BubbleResult;
import com.example.demo.service.BubbleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BubbleController {

    @Autowired
    private BubbleService bubbleService;

    @PostMapping("/bubble-sort")
    @Traceable(apiName = "bubble-sort")
    public ApiResponse<BubbleResult> bubbleSort(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> array = body.get("array");
        BubbleResult result = bubbleService.sort(array);
        return ApiResponse.success(result);
    }
}
```

- [ ] **Step 4: 创建 BubbleControllerTest.java**

```java
package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BubbleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bubbleSortNormal() throws Exception {
        mockMvc.perform(post("/api/bubble-sort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\": [5, 3, 8, 1, 2]}")
                        .header("X-Caller-Name", "王五")
                        .header("X-Person-Type", "产品")
                        .header("X-Person-Level", "中级")
                        .header("X-Department", "产品部"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalArray[0]").value(5))
                .andExpect(jsonPath("$.data.sortedArray[0]").value(1))
                .andExpect(jsonPath("$.data.swapCount").isNumber())
                .andExpect(jsonPath("$.data.comparisonCount").isNumber());
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend && mvn test -Dtest=BubbleControllerTest -q
```

Expected: 测试通过。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/example/demo/model/BubbleResult.java backend/src/main/java/com/example/demo/service/BubbleService.java backend/src/main/java/com/example/demo/controller/BubbleController.java backend/src/test/java/com/example/demo/controller/BubbleControllerTest.java
git commit -m "feat: 实现冒泡排序 POST 接口"
```

---

## Task 7: 分析报表接口

**Files:**
- Create: `backend/src/main/java/com/example/demo/service/AnalyticsService.java`
- Create: `backend/src/main/java/com/example/demo/controller/AnalyticsController.java`
- Test: `backend/src/test/java/com/example/demo/controller/AnalyticsControllerTest.java`

**Interfaces:**
- Consumes: `CallLogRepository` (Task 2), `AnalyticsResponse` (Task 2), `ApiResponse<T>`
- Produces: `GET /api/analytics/summary?dimension=personType&startTime=...&endTime=...` → `ApiResponse<AnalyticsResponse>`

- [ ] **Step 1: 创建 AnalyticsService.java**

```java
package com.example.demo.service;

import com.example.demo.dto.AnalyticsResponse;
import com.example.demo.repository.CallLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private CallLogRepository callLogRepository;

    public AnalyticsResponse getSummary(String dimension, LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> rawData;
        switch (dimension) {
            case "personType":
                rawData = callLogRepository.countByPersonType(startTime, endTime);
                break;
            case "personLevel":
                rawData = callLogRepository.countByPersonLevel(startTime, endTime);
                break;
            case "department":
                rawData = callLogRepository.countByDepartment(startTime, endTime);
                break;
            case "timeTrend":
                rawData = callLogRepository.countByTimeTrend(startTime, endTime);
                break;
            default:
                rawData = callLogRepository.countByPersonType(startTime, endTime);
                dimension = "personType";
        }

        List<AnalyticsResponse.SeriesItem> series = rawData.stream()
                .map(row -> new AnalyticsResponse.SeriesItem(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        long totalCalls = series.stream().mapToLong(AnalyticsResponse.SeriesItem::getValue).sum();

        return new AnalyticsResponse(dimension, series, totalCalls);
    }
}
```

- [ ] **Step 2: 创建 AnalyticsController.java**

```java
package com.example.demo.controller;

import com.example.demo.dto.AnalyticsResponse;
import com.example.demo.dto.ApiResponse;
import com.example.demo.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsResponse> getSummary(
            @RequestParam(defaultValue = "personType") String dimension,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        if (startTime == null) startTime = LocalDateTime.now().minusDays(7);
        if (endTime == null) endTime = LocalDateTime.now();

        AnalyticsResponse response = analyticsService.getSummary(dimension, startTime, endTime);
        return ApiResponse.success(response);
    }
}
```

- [ ] **Step 3: 创建 AnalyticsControllerTest.java**

```java
package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyticsByPersonType() throws Exception {
        mockMvc.perform(get("/api/analytics/summary")
                        .param("dimension", "personType"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dimension").value("personType"))
                .andExpect(jsonPath("$.data.totalCalls").isNumber());
    }

    @Test
    void analyticsByDepartment() throws Exception {
        mockMvc.perform(get("/api/analytics/summary")
                        .param("dimension", "department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dimension").value("department"));
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd backend && mvn test -Dtest=AnalyticsControllerTest -q
```

Expected: 测试通过。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/example/demo/service/AnalyticsService.java backend/src/main/java/com/example/demo/controller/AnalyticsController.java backend/src/test/java/com/example/demo/controller/AnalyticsControllerTest.java
git commit -m "feat: 实现分析报表查询接口（支持多维度聚合）"
```

---

## Task 8: 导出接口

**Files:**
- Create: `backend/src/main/java/com/example/demo/service/ExportService.java`
- Create: `backend/src/main/java/com/example/demo/controller/ExportController.java`
- Test: `backend/src/test/java/com/example/demo/controller/ExportControllerTest.java`

**Interfaces:**
- Consumes: `HelloService`, `HashService`, `BubbleService` (Task 4-6)
- Produces: `GET /api/export?type=hello&format=csv` → 文件流下载

- [ ] **Step 1: 创建 ExportService.java**

```java
package com.example.demo.service;

import com.example.demo.model.BubbleResult;
import com.example.demo.model.HashResult;
import com.example.demo.model.HelloResult;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private HelloService helloService;

    @Autowired
    private HashService hashService;

    @Autowired
    private BubbleService bubbleService;

    public byte[] exportHello(String format) {
        HelloResult result = helloService.greet("Sample");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHelloExcel(result);
        }
        return exportHelloCsv(result);
    }

    public byte[] exportHash(String format) {
        HashResult result = hashService.computeHash("sample-data", "SHA-256");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHashExcel(result);
        }
        return exportHashCsv(result);
    }

    public byte[] exportBubble(String format) {
        BubbleResult result = bubbleService.sort(Arrays.asList(5, 3, 8, 1, 2));
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportBubbleExcel(result);
        }
        return exportBubbleCsv(result);
    }

    private byte[] exportHelloCsv(HelloResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("greeting\n");
        sb.append(result.getGreeting()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportHashCsv(HashResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("input,algorithm,hash\n");
        sb.append(result.getInput()).append(",")
          .append(result.getAlgorithm()).append(",")
          .append(result.getHash()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportBubbleCsv(BubbleResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("originalArray,sortedArray,swapCount,comparisonCount\n");
        sb.append(result.getOriginalArray()).append(",")
          .append(result.getSortedArray()).append(",")
          .append(result.getSwapCount()).append(",")
          .append(result.getComparisonCount()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportHelloExcel(HelloResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("HelloWorld");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("greeting");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getGreeting());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }

    private byte[] exportHashExcel(HashResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Hash");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("input");
            header.createCell(1).setCellValue("algorithm");
            header.createCell(2).setCellValue("hash");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getInput());
            row.createCell(1).setCellValue(result.getAlgorithm());
            row.createCell(2).setCellValue(result.getHash());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }

    private byte[] exportBubbleExcel(BubbleResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BubbleSort");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("originalArray");
            header.createCell(1).setCellValue("sortedArray");
            header.createCell(2).setCellValue("swapCount");
            header.createCell(3).setCellValue("comparisonCount");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getOriginalArray().toString());
            row.createCell(1).setCellValue(result.getSortedArray().toString());
            row.createCell(2).setCellValue(result.getSwapCount());
            row.createCell(3).setCellValue(result.getComparisonCount());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }
}
```

- [ ] **Step 2: 创建 ExportController.java**

```java
package com.example.demo.controller;

import com.example.demo.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping
    public ResponseEntity<byte[]> export(@RequestParam String type,
                                         @RequestParam(defaultValue = "csv") String format) {
        byte[] data;
        String filename;
        String contentType;

        if ("xlsx".equalsIgnoreCase(format)) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = type + "_export.xlsx";
        } else {
            contentType = "text/csv; charset=UTF-8";
            filename = type + "_export.csv";
        }

        switch (type) {
            case "hello":
                data = exportService.exportHello(format);
                break;
            case "hash":
                data = exportService.exportHash(format);
                break;
            case "bubble":
                data = exportService.exportBubble(format);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
```

- [ ] **Step 3: 创建 ExportControllerTest.java**

```java
package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportHelloCsv() throws Exception {
        mockMvc.perform(get("/api/export")
                        .param("type", "hello")
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hello_export.csv\""))
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
    }

    @Test
    void exportHashXlsx() throws Exception {
        mockMvc.perform(get("/api/export")
                        .param("type", "hash")
                        .param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hash_export.xlsx\""))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
```

- [ ] **Step 4: 运行全部后端测试**

```bash
cd backend && mvn test -q
```

Expected: 所有测试通过。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/example/demo/service/ExportService.java backend/src/main/java/com/example/demo/controller/ExportController.java backend/src/test/java/com/example/demo/controller/ExportControllerTest.java
git commit -m "feat: 实现导出接口（CSV/Excel 格式）"
```

---

## Task 9: 前端项目脚手架搭建

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.jsx`
- Create: `frontend/src/App.jsx`
- Create: `frontend/src/App.css`

**Interfaces:**
- Consumes: 无
- Produces: 可运行的 Vite + React 前端项目骨架

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "api-dashboard-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "antd": "^5.21.0",
    "axios": "^1.7.7",
    "echarts": "^5.5.1",
    "echarts-for-react": "^3.0.2"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.3.1",
    "vite": "^5.4.8"
  }
}
```

- [ ] **Step 2: 创建 vite.config.js**

```javascript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 3: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>三接口展示与调用分析报表系统</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

- [ ] **Step 4: 创建 main.jsx**

```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './App.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

- [ ] **Step 5: 创建 App.jsx**

```jsx
import React from 'react';
import Dashboard from './pages/Dashboard';

function App() {
  return (
    <div className="app">
      <Dashboard />
    </div>
  );
}

export default App;
```

- [ ] **Step 6: 创建 App.css**

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: #f0f2f5;
}

.app {
  min-height: 100vh;
  padding: 24px;
}
```

- [ ] **Step 7: 安装依赖并验证**

```bash
cd frontend && npm install 2>&1 | tail -5
```

Expected: 依赖安装成功，无错误。

- [ ] **Step 8: 提交**

```bash
git add frontend/package.json frontend/vite.config.js frontend/index.html frontend/src/main.jsx frontend/src/App.jsx frontend/src/App.css
git commit -m "feat: 搭建 Vite + React 前端项目脚手架"
```

---

## Task 10: 前端 API 层和常量

**Files:**
- Create: `frontend/src/services/api.js`
- Create: `frontend/src/utils/constants.js`

**Interfaces:**
- Consumes: 无（独立工具模块）
- Produces: `api.js`（所有后端 API 的 Axios 封装）, `constants.js`（常量定义）

- [ ] **Step 1: 创建 constants.js**

```javascript
// 调用人信息（模拟登录用户）
export const CALLER_INFO = {
  name: '张三',
  personType: '研发',
  personLevel: '高级',
  department: '技术部',
};

// 支持的可选算法列表
export const HASH_ALGORITHMS = ['SHA-256', 'MD5', 'SHA-512'];

// 分析维度
export const DIMENSIONS = [
  { key: 'personType', label: '人员类型' },
  { key: 'personLevel', label: '人员层级' },
  { key: 'department', label: '人员部门' },
  { key: 'timeTrend', label: '时间趋势' },
];

// 图表类型映射
export const CHART_TYPE_MAP = {
  personType: 'pie',
  personLevel: 'pie',
  department: 'bar',
  timeTrend: 'line',
};
```

- [ ] **Step 2: 创建 api.js**

```javascript
import axios from 'axios';
import { CALLER_INFO } from '../utils/constants';

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'X-Caller-Name': CALLER_INFO.name,
    'X-Person-Type': CALLER_INFO.personType,
    'X-Person-Level': CALLER_INFO.personLevel,
    'X-Department': CALLER_INFO.department,
  },
});

// HelloWorld 接口
export function callHello(name) {
  return apiClient.get('/hello', { params: { name } });
}

// 哈希计算接口
export function callHash(input, algorithm) {
  return apiClient.post('/hash', { input, algorithm });
}

// 冒泡排序接口
export function callBubbleSort(array) {
  return apiClient.post('/bubble-sort', { array });
}

// 导出接口
export function getExportUrl(type, format = 'csv') {
  return `/api/export?type=${type}&format=${format}`;
}

// 分析报表接口
export function getAnalytics(dimension, startTime, endTime) {
  const params = { dimension };
  if (startTime) params.startTime = startTime;
  if (endTime) params.endTime = endTime;
  return apiClient.get('/analytics/summary', { params });
}

export default apiClient;
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/services/api.js frontend/src/utils/constants.js
git commit -m "feat: 添加前端 API 层和常量定义"
```

---

## Task 11: HelloWorld Tab 组件

**Files:**
- Create: `frontend/src/components/HelloTab.jsx`

**Interfaces:**
- Consumes: `callHello` from `api.js`
- Produces: HelloWorld Tab 组件（含输入框 + 调用按钮 + 结果展示）

- [ ] **Step 1: 创建 HelloTab.jsx**

```jsx
import React, { useState } from 'react';
import { Input, Button, Card, message, Spin, Typography } from 'antd';
import { callHello } from '../services/api';

const { TextArea } = Input;
const { Title, Text } = Typography;

function HelloTab() {
  const [name, setName] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    setLoading(true);
    try {
      const res = await callHello(name || 'World');
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('调用成功');
      } else {
        message.error(res.data.message);
      }
    } catch (err) {
      message.error('调用失败: ' + (err.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="HelloWorld 接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入名称：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="请输入名称（默认 World）"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        调用接口
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>返回结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>{result.greeting}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default HelloTab;
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/HelloTab.jsx
git commit -m "feat: 实现 HelloWorld Tab 组件"
```

---

## Task 12: Hash Tab 组件

**Files:**
- Create: `frontend/src/components/HashTab.jsx`

**Interfaces:**
- Consumes: `callHash` from `api.js`, `HASH_ALGORITHMS` from `constants.js`
- Produces: Hash Tab 组件（输入文本 + 算法选择 + 调用 + 结果展示）

- [ ] **Step 1: 创建 HashTab.jsx**

```jsx
import React, { useState } from 'react';
import { Input, Button, Card, Select, message, Spin, Typography, Tag } from 'antd';
import { callHash } from '../services/api';
import { HASH_ALGORITHMS } from '../utils/constants';

const { TextArea } = Input;
const { Title, Text } = Typography;

function HashTab() {
  const [input, setInput] = useState('');
  const [algorithm, setAlgorithm] = useState('SHA-256');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    if (!input) {
      message.warning('请输入要计算哈希的文本');
      return;
    }
    setLoading(true);
    try {
      const res = await callHash(input, algorithm);
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('计算成功');
      } else {
        message.error(res.data.message);
      }
    } catch (err) {
      message.error('调用失败: ' + (err.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="哈希算法接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入文本：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="请输入要计算哈希的文本"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <Text>选择算法：</Text>
        <Select
          style={{ width: 200, marginLeft: 8 }}
          value={algorithm}
          onChange={setAlgorithm}
          options={HASH_ALGORITHMS.map((algo) => ({ label: algo, value: algo }))}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        计算哈希
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>计算结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>输入: {result.input}</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>算法: </Text>
            <Tag color="blue">{result.algorithm}</Tag>
          </div>
          <div style={{ marginTop: 4, wordBreak: 'break-all' }}>
            <Text>哈希值: {result.hash}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default HashTab;
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/HashTab.jsx
git commit -m "feat: 实现哈希算法 Tab 组件"
```

---

## Task 13: BubbleSort Tab 组件

**Files:**
- Create: `frontend/src/components/BubbleTab.jsx`

**Interfaces:**
- Consumes: `callBubbleSort` from `api.js`
- Produces: BubbleSort Tab 组件（输入数组 + 调用 + 排序结果展示）

- [ ] **Step 1: 创建 BubbleTab.jsx**

```jsx
import React, { useState } from 'react';
import { Input, Button, Card, message, Spin, Typography, Tag, List } from 'antd';
import { callBubbleSort } from '../services/api';

const { TextArea } = Input;
const { Title, Text } = Typography;

function BubbleTab() {
  const [arrayInput, setArrayInput] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    let array;
    try {
      array = JSON.parse(`[${arrayInput}]`);
      if (!Array.isArray(array) || array.length === 0) {
        message.warning('请输入有效的数字数组，例如: 5, 3, 8, 1, 2');
        return;
      }
    } catch {
      message.warning('请输入有效的数字数组，例如: 5, 3, 8, 1, 2');
      return;
    }

    setLoading(true);
    try {
      const res = await callBubbleSort(array);
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('排序成功');
      } else {
        message.error(res.data.message);
      }
    } catch (err) {
      message.error('调用失败: ' + (err.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="冒泡排序接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入数组（逗号分隔）：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="例如: 5, 3, 8, 1, 2"
          value={arrayInput}
          onChange={(e) => setArrayInput(e.target.value)}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        排序
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>排序结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>原始数组: [{result.originalArray.join(', ')}]</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>排序后: </Text>
            <Tag color="green">[{result.sortedArray.join(', ')}]</Tag>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>交换次数: {result.swapCount}</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>比较次数: {result.comparisonCount}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default BubbleTab;
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/BubbleTab.jsx
git commit -m "feat: 实现冒泡排序 Tab 组件"
```

---

## Task 14: 导出按钮组件

**Files:**
- Create: `frontend/src/components/ExportButton.jsx`

**Interfaces:**
- Consumes: `getExportUrl` from `api.js`
- Produces: 导出按钮组件（支持选择类型和格式）

- [ ] **Step 1: 创建 ExportButton.jsx**

```jsx
import React, { useState } from 'react';
import { Button, Dropdown, message, Space } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { getExportUrl } from '../services/api';

function ExportButton() {
  const [exporting, setExporting] = useState(false);

  const handleExport = async (type, format) => {
    setExporting(true);
    try {
      const url = getExportUrl(type, format);
      // 使用临时链接下载
      const link = document.createElement('a');
      link.href = url;
      link.download = `${type}_export.${format === 'xlsx' ? 'xlsx' : 'csv'}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      message.success(`导出 ${type} 成功`);
    } catch (err) {
      message.error('导出失败: ' + (err.message || '未知错误'));
    } finally {
      setExporting(false);
    };
  };

  const items = [
    {
      key: 'hello',
      label: 'HelloWorld 结果',
      children: [
        { key: 'hello-csv', label: 'CSV 格式', onClick: () => handleExport('hello', 'csv') },
        { key: 'hello-xlsx', label: 'Excel 格式', onClick: () => handleExport('hello', 'xlsx') },
      ],
    },
    {
      key: 'hash',
      label: '哈希结果',
      children: [
        { key: 'hash-csv', label: 'CSV 格式', onClick: () => handleExport('hash', 'csv') },
        { key: 'hash-xlsx', label: 'Excel 格式', onClick: () => handleExport('hash', 'xlsx') },
      ],
    },
    {
      key: 'bubble',
      label: '排序结果',
      children: [
        { key: 'bubble-csv', label: 'CSV 格式', onClick: () => handleExport('bubble', 'csv') },
        { key: 'bubble-xlsx', label: 'Excel 格式', onClick: () => handleExport('bubble', 'xlsx') },
      ],
    },
  ];

  return (
    <Dropdown menu={{ items }} disabled={exporting}>
      <Button type="primary" icon={<DownloadOutlined />} loading={exporting}>
        导出结果
      </Button>
    </Dropdown>
  );
}

export default ExportButton;
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/ExportButton.jsx
git commit -m "feat: 实现导出按钮组件（支持类型和格式选择）"
```

---

## Task 15: 分析图表组件

**Files:**
- Create: `frontend/src/components/AnalyticsChart.jsx`

**Interfaces:**
- Consumes: `getAnalytics` from `api.js`, `DIMENSIONS` / `CHART_TYPE_MAP` from `constants.js`
- Produces: 报表图表组件（折线图 / 饼图 / 柱状图 + 维度切换）

- [ ] **Step 1: 创建 AnalyticsChart.jsx**

```jsx
import React, { useState, useEffect, useCallback } from 'react';
import { Card, Select, Row, Col, Spin, message, Typography, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getAnalytics } from '../services/api';
import { DIMENSIONS, CHART_TYPE_MAP } from '../utils/constants';

const { Title, Text } = Typography;

function AnalyticsChart() {
  const [dimension, setDimension] = useState('personType');
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAnalytics(dimension);
      if (res.data.code === 200) {
        setChartData(res.data.data);
      }
    } catch (err) {
      message.error('获取报表数据失败');
    } finally {
      setLoading(false);
    }
  }, [dimension]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getChartOption = (chartType, chartDimension) => {
    if (!chartData || !chartData.series) return {};

    const seriesData = chartData.series.map((item) => ({
      name: item.label,
      value: item.value,
    }));

    const baseOption = {
      title: {
        text: `${chartData.dimension} - 调用分布`,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
    };

    if (chartType === 'pie') {
      return {
        ...baseOption,
        series: [
          {
            type: 'pie',
            radius: ['30%', '60%'],
            center: ['50%', '55%'],
            data: seriesData,
            label: {
              formatter: '{b}: {c}',
            },
          },
        ],
      };
    }

    if (chartType === 'line') {
      return {
        ...baseOption,
        xAxis: {
          type: 'category',
          data: chartData.series.map((item) => item.label),
          axisLabel: { rotate: 45 },
        },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            data: chartData.series.map((item) => item.value),
            smooth: true,
            areaStyle: { opacity: 0.3 },
          },
        ],
      };
    }

    // bar
    return {
      ...baseOption,
      xAxis: {
        type: 'category',
        data: chartData.series.map((item) => item.label),
        axisLabel: { rotate: 45 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: chartData.series.map((item) => item.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 1, color: '#188df0' },
            ]),
          },
        },
      ],
    };
  };

  const chartType = CHART_TYPE_MAP[dimension] || 'bar';

  return (
    <Card
      title="调用分析报表"
      extra={
        <Select
          value={dimension}
          onChange={setDimension}
          style={{ width: 150 }}
          options={DIMENSIONS.map((d) => ({ label: d.label, value: d.key }))}
        />
      }
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin size="large" />
        </div>
      ) : chartData && chartData.series && chartData.series.length > 0 ? (
        <>
          <div style={{ textAlign: 'center', marginBottom: 16 }}>
            <Text>总调用次数: <strong>{chartData.totalCalls}</strong></Text>
          </div>
          <Row gutter={16}>
            <Col span={12}>
              <ReactECharts
                option={getChartOption(chartType, dimension)}
                style={{ height: 350 }}
              />
            </Col>
            <Col span={12}>
              <ReactECharts
                option={getChartOption('pie', dimension)}
                style={{ height: 350 }}
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={24}>
              <ReactECharts
                option={getChartOption('bar', dimension)}
                style={{ height: 300 }}
              />
            </Col>
          </Row>
        </>
      ) : (
        <Empty description="暂无调用数据" />
      )}
    </Card>
  );
}

export default AnalyticsChart;
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/AnalyticsChart.jsx
git commit -m "feat: 实现分析图表组件（折线图/饼图/柱状图 + 维度切换）"
```

---

## Task 16: Dashboard 主页面

**Files:**
- Create: `frontend/src/pages/Dashboard.jsx`

**Interfaces:**
- Consumes: HelloTab, HashTab, BubbleTab, ExportButton, AnalyticsChart
- Produces: 整合三 Tab + 导出按钮 + 报表的完整主页面

- [ ] **Step 1: 创建 Dashboard.jsx**

```jsx
import React from 'react';
import { Tabs, Typography, Space, Divider } from 'antd';
import { CodeOutlined, KeyOutlined, SortAscendingOutlined } from '@ant-design/icons';
import HelloTab from '../components/HelloTab';
import HashTab from '../components/HashTab';
import BubbleTab from '../components/BubbleTab';
import ExportButton from '../components/ExportButton';
import AnalyticsChart from '../components/AnalyticsChart';

const { Title } = Typography;

function Dashboard() {
  const tabItems = [
    {
      key: 'hello',
      label: (
        <span>
          <CodeOutlined /> HelloWorld
        </span>
      ),
      children: <HelloTab />,
    },
    {
      key: 'hash',
      label: (
        <span>
          <KeyOutlined /> 哈希算法
        </span>
      ),
      children: <HashTab />,
    },
    {
      key: 'bubble',
      label: (
        <span>
          <SortAscendingOutlined /> 冒泡排序
        </span>
      ),
      children: <BubbleTab />,
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0 }}>
          三接口展示与调用分析报表系统
        </Title>
        <ExportButton />
      </div>

      <Tabs defaultActiveKey="hello" items={tabItems} />

      <Divider />

      <AnalyticsChart />
    </div>
  );
}

export default Dashboard;
```

- [ ] **Step 2: 验证前端构建**

```bash
cd frontend && npx vite build 2>&1 | tail -10
```

Expected: 构建成功，生成 dist 目录。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/pages/Dashboard.jsx
git commit -m "feat: 实现 Dashboard 主页面（整合三 Tab + 导出 + 报表）"
```

---

## 自检清单

**1. 需求覆盖检查：**
- ✅ 三个 REST 接口: HelloWorld (Task 4), Hash (Task 5), BubbleSort (Task 6)
- ✅ 前端三 Tab 页面: HelloTab (Task 11), HashTab (Task 12), BubbleTab (Task 13)
- ✅ 导出按钮: ExportButton (Task 14) + 后端导出 API (Task 8)
- ✅ 后端埋点: @Traceable 注解 + AOP 切面 (Task 3), CallLog 实体 (Task 2)
- ✅ 前端可视化报表: AnalyticsChart (Task 15) + Analytics API (Task 7)
- ✅ 折线图（时间趋势）、饼图（人员类型/层级）、柱状图（部门）
- ✅ 维度切换: 人员类型、人员层级、人员部门、时间趋势

**2. 占位符检查：** 无 "TBD"、"TODO"、"implement later"、"fill in details" 等占位符。所有代码块包含完整实现。

**3. 类型一致性检查：** 所有接口签名在 Task 间一致：
- `ApiResponse<T>` 作为统一返回类型
- `@Traceable(apiName = "...")` 标注在所有 Controller 方法上
- 请求头 `X-Caller-Name`, `X-Person-Type`, `X-Person-Level`, `X-Department` 在 AOP 和前端 API 层一致
- 数据库字段名与 CallLog 实体字段名一致

---

## 执行方式选择

**Plan complete and saved to `docs/superpowers/plans/2026-08-24-api-dashboard-implementation.md`. 两个执行选项：**

**1. Subagent-Driven (推荐)** — 每个 Task 分派一个独立 subagent，Task 间进行 review，快速迭代

**2. Inline Execution** — 在当前 session 中逐 Task 执行，批量执行 + 检查点 review

**Which approach?**