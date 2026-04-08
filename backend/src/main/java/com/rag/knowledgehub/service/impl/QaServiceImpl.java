package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.knowledgehub.config.RagProperties;
import com.rag.knowledgehub.dto.qa.DocumentUsageVO;
import com.rag.knowledgehub.dto.qa.HotQuestionVO;
import com.rag.knowledgehub.dto.qa.QaAnswerResponse;
import com.rag.knowledgehub.dto.qa.SourceSnippet;
import com.rag.knowledgehub.entity.QaRecord;
import com.rag.knowledgehub.mapper.QaRecordMapper;
import com.rag.knowledgehub.service.ElasticsearchVectorService;
import com.rag.knowledgehub.service.EmbeddingService;
import com.rag.knowledgehub.service.LlmService;
import com.rag.knowledgehub.service.QaService;
import com.rag.knowledgehub.websocket.QaWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QaServiceImpl implements QaService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final LlmService llmService;
    private final QaRecordMapper qaRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;
    private final QaWebSocketHandler qaWebSocketHandler;

    public QaServiceImpl(EmbeddingService embeddingService,
                         ElasticsearchVectorService elasticsearchVectorService,
                         LlmService llmService,
                         QaRecordMapper qaRecordMapper,
                         RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper,
                         RagProperties ragProperties,
                         QaWebSocketHandler qaWebSocketHandler) {
        this.embeddingService = embeddingService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.llmService = llmService;
        this.qaRecordMapper = qaRecordMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ragProperties = ragProperties;
        this.qaWebSocketHandler = qaWebSocketHandler;
    }

    @Override
    public QaAnswerResponse ask(Long userId, Long knowledgeBaseId, String question) {
        long start = System.currentTimeMillis();
        String cacheKey = "qa:answer:" + knowledgeBaseId + ":" + DigestUtils.md5DigestAsHex(question.getBytes(StandardCharsets.UTF_8));
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof String json) {
            try {
                QaAnswerResponse response = objectMapper.readValue(json, QaAnswerResponse.class);
                response.setCached(true);
                return response;
            } catch (Exception ignored) {
            }
        }

        List<Double> qVector = embeddingService.embed(question);
        List<SourceSnippet> snippets = elasticsearchVectorService.search(knowledgeBaseId, qVector, ragProperties.getTopK());

        String context = snippets.stream()
                .map(s -> String.format("[文档:%s-片段#%d]\n%s", s.getDocumentName(), s.getChunkIndex(), s.getSnippet()))
                .collect(Collectors.joining("\n\n"));
        String prompt = "请严格基于以下知识库片段回答问题。如果信息不足，请明确说明。\n\n"
                + "上下文:\n" + context + "\n\n"
                + "问题:\n" + question + "\n\n"
                + "输出要求: 1) 先给结论 2) 再给关键依据 3) 不要编造不存在的信息。";

        String answer = llmService.chat(prompt);
        QaAnswerResponse response = QaAnswerResponse.builder()
                .answer(answer)
                .sources(snippets)
                .cached(false)
                .latencyMs(System.currentTimeMillis() - start)
                .build();

        saveRecord(userId, knowledgeBaseId, question, response);
        cacheAnswer(cacheKey, response);
        pushWs(userId, response);
        return response;
    }

    @Override
    public List<HotQuestionVO> hotQuestions(Long userId) {
        List<QaRecord> records = qaRecordMapper.selectList(
                new LambdaQueryWrapper<QaRecord>()
                        .eq(QaRecord::getUserId, userId)
                        .orderByDesc(QaRecord::getId)
                        .last("limit 500")
        );
        return records.stream()
                .collect(Collectors.groupingBy(QaRecord::getQuestion, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> HotQuestionVO.builder().question(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    public List<DocumentUsageVO> documentUsage(Long userId) {
        List<QaRecord> records = qaRecordMapper.selectList(
                new LambdaQueryWrapper<QaRecord>()
                        .eq(QaRecord::getUserId, userId)
                        .orderByDesc(QaRecord::getId)
                        .last("limit 500")
        );

        Map<String, Long> countMap = new HashMap<>();
        for (QaRecord record : records) {
            if (record.getSourceJson() == null || record.getSourceJson().isBlank()) {
                continue;
            }
            try {
                List<SourceSnippet> snippets = objectMapper.readValue(record.getSourceJson(), new TypeReference<List<SourceSnippet>>() {
                });
                for (SourceSnippet snippet : snippets) {
                    String key = snippet.getDocumentId() + "||" + snippet.getDocumentName();
                    countMap.put(key, countMap.getOrDefault(key, 0L) + 1);
                }
            } catch (Exception ignored) {
            }
        }

        return countMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    String[] arr = e.getKey().split("\\|\\|");
                    return DocumentUsageVO.builder()
                            .documentId(Long.parseLong(arr[0]))
                            .documentName(arr[1])
                            .count(e.getValue())
                            .build();
                }).toList();
    }

    private void saveRecord(Long userId, Long knowledgeBaseId, String question, QaAnswerResponse response) {
        try {
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setKnowledgeBaseId(knowledgeBaseId);
            record.setQuestion(question);
            record.setAnswer(response.getAnswer());
            record.setSourceJson(objectMapper.writeValueAsString(response.getSources()));
            qaRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("Failed to save QA record", e);
        }
    }

    private void cacheAnswer(String key, QaAnswerResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(30));
        } catch (Exception ignored) {
        }
    }

    private void pushWs(Long userId, QaAnswerResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "qa_result",
                    "data", response
            ));
            qaWebSocketHandler.sendToUser(userId, payload);
        } catch (Exception ignored) {
        }
    }
}
