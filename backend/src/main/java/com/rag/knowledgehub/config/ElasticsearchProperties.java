package com.rag.knowledgehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.elasticsearch")
public class ElasticsearchProperties {

    private String baseUrl;
    private String username;
    private String password;
    private String indexName;
}
