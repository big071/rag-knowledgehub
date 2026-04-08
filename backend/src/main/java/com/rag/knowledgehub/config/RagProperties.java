package com.rag.knowledgehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer topK;
}
