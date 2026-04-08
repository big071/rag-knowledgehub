package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.dto.qa.DocumentUsageVO;
import com.rag.knowledgehub.dto.qa.HotQuestionVO;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "统计模块")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final QaService qaService;

    public StatsController(QaService qaService) {
        this.qaService = qaService;
    }

    @Operation(summary = "热点问题")
    @GetMapping("/hot-questions")
    public ApiResponse<List<HotQuestionVO>> hotQuestions() {
        return ApiResponse.success(qaService.hotQuestions(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "文档使用频次")
    @GetMapping("/document-usage")
    public ApiResponse<List<DocumentUsageVO>> documentUsage() {
        return ApiResponse.success(qaService.documentUsage(SecurityUtils.getCurrentUserId()));
    }
}
