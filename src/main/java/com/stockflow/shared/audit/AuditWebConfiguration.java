package com.stockflow.shared.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfiguration implements WebMvcConfigurer {

    private final AuditTrailInterceptor auditTrailInterceptor;

    public AuditWebConfiguration(AuditTrailInterceptor auditTrailInterceptor) {
        this.auditTrailInterceptor = auditTrailInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditTrailInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}
