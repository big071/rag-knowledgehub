package com.rag.knowledgehub.service;

import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseCreateRequest;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseVO;

public interface KnowledgeBaseService {

    KnowledgeBaseVO create(Long userId, KnowledgeBaseCreateRequest request);

    PageResponse<KnowledgeBaseVO> page(Long userId, int pageNum, int pageSize, String keyword);

    void delete(Long userId, Long kbId);
}
