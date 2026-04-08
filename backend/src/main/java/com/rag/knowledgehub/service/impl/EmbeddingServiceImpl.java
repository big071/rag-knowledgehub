package com.rag.knowledgehub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.config.EmbeddingProperties;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.service.EmbeddingService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final EmbeddingProperties embeddingProperties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingServiceImpl(EmbeddingProperties embeddingProperties, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.embeddingProperties = embeddingProperties;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", embeddingProperties.getModel(),
                    "text", text
            ));
            Request request = new Request.Builder()
                    .url(embeddingProperties.getBaseUrl() + "/embed")
                    .post(RequestBody.create(body, JSON))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 服务调用失败");
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode arr = root.get("embedding");
                if (arr == null || !arr.isArray()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 服务返回异常");
                }
                List<Double> vector = new ArrayList<>(arr.size());
                arr.forEach(n -> vector.add(n.asDouble()));
                return vector;
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding 服务不可用");
        }
    }
}
