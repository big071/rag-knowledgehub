package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.knowledgehub.config.RagProperties;
import com.rag.knowledgehub.dto.qa.DocumentUsageVO;
import com.rag.knowledgehub.dto.qa.HotQuestionVO;
import com.rag.knowledgehub.dto.qa.QaAnswerResponse;
import com.rag.knowledgehub.dto.qa.SourceSnippet;
import com.rag.knowledgehub.entity.Document;
import com.rag.knowledgehub.entity.QaRecord;
import com.rag.knowledgehub.mapper.DocumentMapper;
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
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QaServiceImpl implements QaService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final LlmService llmService;
    private final QaRecordMapper qaRecordMapper;
    private final DocumentMapper documentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;
    private final QaWebSocketHandler qaWebSocketHandler;

    public QaServiceImpl(EmbeddingService embeddingService,
                         ElasticsearchVectorService elasticsearchVectorService,
                         LlmService llmService,
                         QaRecordMapper qaRecordMapper,
                         DocumentMapper documentMapper,
                         RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper,
                         RagProperties ragProperties,
                         QaWebSocketHandler qaWebSocketHandler) {
        this.embeddingService = embeddingService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.llmService = llmService;
        this.qaRecordMapper = qaRecordMapper;
        this.documentMapper = documentMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ragProperties = ragProperties;
        this.qaWebSocketHandler = qaWebSocketHandler;
    }

    @Override
    public QaAnswerResponse ask(Long userId, Long knowledgeBaseId, String question, String conversationId,
                                String fileType, String startTime, String endTime) {
        long start = System.currentTimeMillis();
        String convId = StringUtils.hasText(conversationId) ? conversationId : UUID.randomUUID().toString().replace("-", "");
        String keyRaw = knowledgeBaseId + "|" + question + "|" + convId + "|" + Optional.ofNullable(fileType).orElse("")
                + "|" + Optional.ofNullable(startTime).orElse("") + "|" + Optional.ofNullable(endTime).orElse("");
        String cacheKey = "qa:answer:" + DigestUtils.md5DigestAsHex(keyRaw.getBytes(StandardCharsets.UTF_8));

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
        List<SourceSnippet> snippets = elasticsearchVectorService.search(knowledgeBaseId, qVector, question, ragProperties.getTopK() * 2);
        snippets = filterSnippets(snippets, fileType, startTime, endTime).stream().limit(ragProperties.getTopK()).toList();

        String historyText = buildHistoryContext(userId, convId);
        String context = snippets.stream()
                .map(s -> String.format("[文档:%s-片段#%d]\n%s", s.getDocumentName(), s.getChunkIndex(), s.getSnippet()))
                .collect(Collectors.joining("\n\n"));

        String prompt = "你是企业知识库助手，请严格基于知识片段回答。信息不足时直接说明。\n\n"
                + "最近对话(最多5轮):\n" + historyText + "\n\n"
                + "知识库上下文:\n" + context + "\n\n"
                + "问题:\n" + question + "\n\n"
                + "输出要求:\n1) 结论\n2) 关键依据\n3) 若有不确定请标记。";

        String answer = llmService.chat(prompt);
        String summary = summarize(answer);
        fillSourceUrls(snippets);

        QaAnswerResponse response = QaAnswerResponse.builder()
                .answer(answer)
                .summary(summary)
                .conversationId(convId)
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

    private String buildHistoryContext(Long userId, String conversationId) {
        List<QaRecord> history = qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, userId)
                .eq(QaRecord::getConversationId, conversationId)
                .orderByDesc(QaRecord::getId)
                .last("limit 5"));
        Collections.reverse(history);
        StringBuilder sb = new StringBuilder();
        for (QaRecord record : history) {
            sb.append("Q: ").append(record.getQuestion()).append("\n")
                    .append("A: ").append(record.getSummary() == null ? record.getAnswer() : record.getSummary()).append("\n\n");
        }
        return sb.toString();
    }

    private List<SourceSnippet> filterSnippets(List<SourceSnippet> snippets, String fileType, String startTime, String endTime) {
        if (snippets.isEmpty()) {
            return snippets;
        }
        Set<Long> ids = snippets.stream().map(SourceSnippet::getDocumentId).collect(Collectors.toSet());
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .in(Document::getId, ids)
                .eq(Document::getLatest, true)
                .eq(Document::getReviewStatus, "APPROVED");

        if (StringUtils.hasText(fileType)) {
            wrapper.eq(Document::getFileType, fileType.toLowerCase());
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Document::getCreatedAt, parseDateTime(startTime));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Document::getCreatedAt, parseDateTime(endTime));
        }
        Set<Long> allowed = documentMapper.selectList(wrapper).stream().map(Document::getId).collect(Collectors.toSet());
        return snippets.stream().filter(s -> allowed.contains(s.getDocumentId())).toList();
    }

    private void fillSourceUrls(List<SourceSnippet> snippets) {
        if (snippets.isEmpty()) {
            return;
        }
        Set<Long> ids = snippets.stream().map(SourceSnippet::getDocumentId).collect(Collectors.toSet());
        Map<Long, String> previewMap = documentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Document::getId, d -> "/files/" + d.getStoragePath(), (a, b) -> a));
        for (SourceSnippet snippet : snippets) {
            snippet.setSourceUrl(previewMap.get(snippet.getDocumentId()));
        }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            if (value.length() == 10) {
                return LocalDateTime.parse(value + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    private String summarize(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String plain = answer.replaceAll("\\s+", " ").trim();
        return plain.length() <= 160 ? plain : plain.substring(0, 160) + "...";
    }

    private void saveRecord(Long userId, Long knowledgeBaseId, String question, QaAnswerResponse response) {
        try {
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setKnowledgeBaseId(knowledgeBaseId);
            record.setQuestion(question);
            record.setAnswer(response.getAnswer());
            record.setSummary(response.getSummary());
            record.setConversationId(response.getConversationId());
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
