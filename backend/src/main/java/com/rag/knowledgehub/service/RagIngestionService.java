package com.rag.knowledgehub.service;

import org.springframework.web.multipart.MultipartFile;

public interface RagIngestionService {

    void ingest(Long userId, Long knowledgeBaseId, Long documentId, String documentName, MultipartFile file);

    void ingest(Long userId, Long knowledgeBaseId, Long documentId, String documentName, MultipartFile file, Long previousDocumentId);
}
