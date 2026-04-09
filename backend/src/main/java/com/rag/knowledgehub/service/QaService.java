package com.rag.knowledgehub.service;

import com.rag.knowledgehub.dto.qa.DocumentUsageVO;
import com.rag.knowledgehub.dto.qa.HotQuestionVO;
import com.rag.knowledgehub.dto.qa.QaAnswerResponse;

import java.util.List;

public interface QaService {

    QaAnswerResponse ask(Long userId, Long knowledgeBaseId, String question, String conversationId,
                         String fileType, String startTime, String endTime);

    List<HotQuestionVO> hotQuestions(Long userId);

    List<DocumentUsageVO> documentUsage(Long userId);
}
