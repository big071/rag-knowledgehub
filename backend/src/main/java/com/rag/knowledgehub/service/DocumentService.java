package com.rag.knowledgehub.service;

import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentVO upload(Long userId, Long knowledgeBaseId, MultipartFile file);

    PageResponse<DocumentVO> page(Long userId, Long knowledgeBaseId, int pageNum, int pageSize, String keyword);

    void delete(Long userId, Long documentId);
}
