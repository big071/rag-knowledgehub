package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<DocumentVO> upload(@RequestParam Long knowledgeBaseId,
                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(SecurityUtils.getCurrentUserId(), knowledgeBaseId, file));
    }

    @Operation(summary = "批量上传")
    @PostMapping("/batch-upload")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<DocumentVO>> batchUpload(@RequestParam Long knowledgeBaseId,
                                                     @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success(documentService.batchUpload(SecurityUtils.getCurrentUserId(), knowledgeBaseId, files));
    }

    @Operation(summary = "文档新版本")
    @PostMapping("/{id}/new-version")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<DocumentVO> uploadNewVersion(@PathVariable Long id,
                                                    @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(documentService.uploadNewVersion(SecurityUtils.getCurrentUserId(), id, file));
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public ApiResponse<PageResponse<DocumentVO>> page(
            @RequestParam Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String reviewStatus
    ) {
        return ApiResponse.success(documentService.page(SecurityUtils.getCurrentUserId(), knowledgeBaseId, pageNum, pageSize,
                keyword, fileType, startTime, endTime, reviewStatus));
    }

    @Operation(summary = "文档审核")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> review(@PathVariable Long id,
                                    @RequestParam String reviewStatus,
                                    @RequestParam(required = false) String reviewComment) {
        documentService.review(SecurityUtils.getCurrentUserId(), id, reviewStatus, reviewComment);
        return ApiResponse.success("审核完成", null);
    }

    @Operation(summary = "设置标签")
    @PostMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> tags(@PathVariable Long id, @RequestParam String tags) {
        documentService.setTags(SecurityUtils.getCurrentUserId(), id, tags);
        return ApiResponse.success("标签已更新", null);
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        documentService.batchDelete(SecurityUtils.getCurrentUserId(), ids);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }
}
