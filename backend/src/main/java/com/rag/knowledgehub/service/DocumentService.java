package com.rag.knowledgehub.service;

import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentVO upload(Long userId, Long knowledgeBaseId, MultipartFile file);

    List<DocumentVO> batchUpload(Long userId, Long knowledgeBaseId, List<MultipartFile> files);

    DocumentVO uploadNewVersion(Long userId, Long documentId, MultipartFile file);

    PageResponse<DocumentVO> page(Long userId, Long knowledgeBaseId, int pageNum, int pageSize, String keyword,
                                  String fileType, String startTime, String endTime, String reviewStatus);

    void review(Long reviewerId, Long documentId, String reviewStatus, String reviewComment);

    void setTags(Long userId, Long documentId, String tags);

    void batchDelete(Long userId, List<Long> ids);

    void delete(Long userId, Long documentId);
}
