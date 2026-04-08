package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档模块")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public ApiResponse<DocumentVO> upload(@RequestParam Long knowledgeBaseId,
                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(SecurityUtils.getCurrentUserId(), knowledgeBaseId, file));
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public ApiResponse<PageResponse<DocumentVO>> page(
            @RequestParam Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(documentService.page(SecurityUtils.getCurrentUserId(), knowledgeBaseId, pageNum, pageSize, keyword));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }
}
