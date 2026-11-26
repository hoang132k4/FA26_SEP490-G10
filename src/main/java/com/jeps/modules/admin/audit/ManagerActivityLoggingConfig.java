package com.jeps.modules.admin.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ManagerActivityLoggingConfig implements WebMvcConfigurer {

    private final ManagerActivityLoggingInterceptor interceptor;

    public ManagerActivityLoggingConfig(ManagerActivityLoggingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }
}
