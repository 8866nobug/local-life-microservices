package com.wanger.aiservice.config;

import com.wanger.aiservice.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只负责把 Gateway 透传的 X-User-Id 放入 ThreadLocal，鉴权已上移到 Gateway
        registry.addInterceptor(new UserInterceptor());
    }
}
