package com.rag.knowledgehub.config;

import com.rag.knowledgehub.common.web.RateLimitInterceptor;
import com.rag.knowledgehub.common.web.OperationLogInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor, OperationLogInterceptor operationLogInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/qa/**");
        registry.addInterceptor(operationLogInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + location);
    }
}
