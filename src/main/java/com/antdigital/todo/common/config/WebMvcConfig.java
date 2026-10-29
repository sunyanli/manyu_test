package com.antdigital.todo.common.config;

import com.antdigital.todo.common.interceptor.LoginInterceptor;
import com.antdigital.todo.common.interceptor.RateLimitInterceptor;
import com.antdigital.todo.common.security.TrustedGatewayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：注册登录态与限流拦截器。
 *
 * @author AiWork
 */
@Configuration
@EnableConfigurationProperties({TodoCreateProperties.class, TrustedGatewayProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String API_PATTERN = "/api/**";

    private final LoginInterceptor loginInterceptor;

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(API_PATTERN)
                .excludePathPatterns("/api/actuator/**")
                .order(1);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(API_PATTERN)
                .excludePathPatterns("/api/actuator/**")
                .order(2);
    }
}
