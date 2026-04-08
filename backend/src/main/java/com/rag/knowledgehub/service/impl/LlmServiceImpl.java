package com.rag.knowledgehub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.config.LlmProperties;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.service.LlmService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class LlmServiceImpl implements LlmService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final LlmProperties llmProperties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public LlmServiceImpl(LlmProperties llmProperties, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String prompt) {
        String url = llmProperties.getBaseUrl().endsWith("/v1")
                ? llmProperties.getBaseUrl() + "/chat/completions"
                : llmProperties.getBaseUrl() + "/v1/chat/completions";

        Map<String, Object> bodyMap = Map.of(
                "model", llmProperties.getModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个严谨的知识库助手，必须优先引用给定上下文回答，回答要简洁准确。"),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String body = objectMapper.writeValueAsString(bodyMap);
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, JSON));
            if (StringUtils.hasText(llmProperties.getApiKey())) {
                builder.addHeader("Authorization", "Bearer " + llmProperties.getApiKey());
            }
            Request request = builder.build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 调用失败");
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 返回为空");
                }
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 服务不可用");
        }
    }
}
