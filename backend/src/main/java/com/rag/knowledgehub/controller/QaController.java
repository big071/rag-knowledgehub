package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.dto.qa.QaAnswerResponse;
import com.rag.knowledgehub.dto.qa.QaAskRequest;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "问答模块")
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @Operation(summary = "RAG问答")
    @PostMapping("/ask")
    public ApiResponse<QaAnswerResponse> ask(@Valid @RequestBody QaAskRequest request) {
        return ApiResponse.success(qaService.ask(SecurityUtils.getCurrentUserId(), request.getKnowledgeBaseId(),
                request.getQuestion(), request.getConversationId(), request.getFileType(), request.getStartTime(), request.getEndTime()));
    }
}
