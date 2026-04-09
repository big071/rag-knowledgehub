package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseCreateRequest;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseVO;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "知识库模块")
@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Operation(summary = "创建知识库")
    @PostMapping
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "分页查询知识库")
    @GetMapping
    public ApiResponse<PageResponse<KnowledgeBaseVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(knowledgeBaseService.page(SecurityUtils.getCurrentUserId(), pageNum, pageSize, keyword));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }
}
