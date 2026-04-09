package com.rag.knowledgehub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.knowledgehub.config.ElasticsearchProperties;
import com.rag.knowledgehub.dto.qa.SourceSnippet;
import com.rag.knowledgehub.service.ElasticsearchVectorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ElasticsearchVectorServiceImpl implements ElasticsearchVectorService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ElasticsearchProperties elasticsearchProperties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public ElasticsearchVectorServiceImpl(ElasticsearchProperties elasticsearchProperties,
                                          OkHttpClient okHttpClient,
                                          ObjectMapper objectMapper) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            ensureIndex();
        } catch (Exception e) {
            log.warn("Elasticsearch index init skipped: {}", e.getMessage());
        }
    }

    @Override
    public void ensureIndex() {
        String index = elasticsearchProperties.getIndexName();
        Request head = builder(index).head().build();
        try (Response response = okHttpClient.newCall(head).execute()) {
            if (response.code() == 200) {
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("knowledgeBaseId", Map.of("type", "long"));
        properties.put("documentId", Map.of("type", "long"));
        properties.put("documentName", Map.of("type", "keyword"));
        properties.put("chunkIndex", Map.of("type", "integer"));
        properties.put("chunkText", Map.of("type", "text", "analyzer", "standard"));
        properties.put("vector", Map.of("type", "dense_vector", "dims", 384));

        Map<String, Object> mapping = Map.of("mappings", Map.of("properties", properties));
        try {
            String body = objectMapper.writeValueAsString(mapping);
            Request put = builder(index).put(RequestBody.create(body, JSON)).build();
            try (Response response = okHttpClient.newCall(put).execute()) {
                if (!response.isSuccessful()) {
                    String msg = response.body() == null ? "" : response.body().string();
                    throw new RuntimeException("Create index failed: " + msg);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void indexChunk(String docId, Long knowledgeBaseId, Long documentId, String documentName,
                           Integer chunkIndex, String chunkText, List<Double> vector) {
        Map<String, Object> payload = Map.of(
                "knowledgeBaseId", knowledgeBaseId,
                "documentId", documentId,
                "documentName", documentName,
                "chunkIndex", chunkIndex,
                "chunkText", chunkText,
                "vector", vector
        );
        try {
            String body = objectMapper.writeValueAsString(payload);
            String path = elasticsearchProperties.getIndexName() + "/_doc/" + docId;
            Request request = builder(path).put(RequestBody.create(body, JSON)).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String msg = response.body() == null ? "" : response.body().string();
                    throw new RuntimeException("Index chunk failed: " + msg);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SourceSnippet> search(Long knowledgeBaseId, List<Double> questionVector, String keyword, int topK) {
        Map<String, Object> innerBool = new HashMap<>();
        innerBool.put("filter", List.of(Map.of("term", Map.of("knowledgeBaseId", knowledgeBaseId))));
        if (StringUtils.hasText(keyword)) {
            innerBool.put("must", List.of(Map.of("match", Map.of("chunkText", keyword))));
        }
        Map<String, Object> payload = Map.of(
                "size", topK,
                "_source", List.of("documentId", "documentName", "chunkIndex", "chunkText"),
                "query", Map.of(
                        "script_score", Map.of(
                                "query", Map.of("bool", innerBool),
                                "script", Map.of(
                                        "source", "cosineSimilarity(params.query_vector, 'vector') + 1.0",
                                        "params", Map.of("query_vector", questionVector)
                                )
                        )
                )
        );

        try {
            String body = objectMapper.writeValueAsString(payload);
            String path = elasticsearchProperties.getIndexName() + "/_search";
            Request request = builder(path).post(RequestBody.create(body, JSON)).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return List.of();
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode hits = root.path("hits").path("hits");
                List<SourceSnippet> result = new ArrayList<>();
                if (hits.isArray()) {
                    for (JsonNode hit : hits) {
                        JsonNode source = hit.path("_source");
                        result.add(SourceSnippet.builder()
                                .documentId(source.path("documentId").asLong())
                                .documentName(source.path("documentName").asText())
                                .chunkIndex(source.path("chunkIndex").asInt())
                                .snippet(source.path("chunkText").asText())
                                .score(hit.path("_score").asDouble())
                                .build());
                    }
                }
                return result;
            }
        } catch (IOException e) {
            log.error("ES search failed", e);
            return List.of();
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        Map<String, Object> payload = Map.of(
                "query", Map.of("term", Map.of("documentId", documentId))
        );
        try {
            String body = objectMapper.writeValueAsString(payload);
            String path = elasticsearchProperties.getIndexName() + "/_delete_by_query";
            Request request = builder(path).post(RequestBody.create(body, JSON)).build();
            try (Response ignored = okHttpClient.newCall(request).execute()) {
                // best effort
            }
        } catch (IOException e) {
            log.warn("Delete document vectors failed", e);
        }
    }

    private Request.Builder builder(String path) {
        String url = elasticsearchProperties.getBaseUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        Request.Builder builder = new Request.Builder().url(url + path);
        if (StringUtils.hasText(elasticsearchProperties.getUsername())) {
            String token = Base64.getEncoder().encodeToString(
                    (elasticsearchProperties.getUsername() + ":" + elasticsearchProperties.getPassword())
                            .getBytes(StandardCharsets.UTF_8));
            builder.addHeader("Authorization", "Basic " + token);
        }
        return builder;
    }
}
