package com.rag.knowledgehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
}
