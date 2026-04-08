package com.rag.knowledgehub.service;

import com.rag.knowledgehub.dto.qa.SourceSnippet;

import java.util.List;

public interface ElasticsearchVectorService {

    void ensureIndex();

    void indexChunk(String docId, Long knowledgeBaseId, Long documentId, String documentName,
                    Integer chunkIndex, String chunkText, List<Double> vector);

    List<SourceSnippet> search(Long knowledgeBaseId, List<Double> questionVector, int topK);

    void deleteByDocumentId(Long documentId);
}
