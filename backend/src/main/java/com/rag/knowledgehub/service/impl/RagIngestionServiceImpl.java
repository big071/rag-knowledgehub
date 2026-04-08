package com.rag.knowledgehub.service.impl;

import com.rag.knowledgehub.config.RagProperties;
import com.rag.knowledgehub.entity.DocumentChunk;
import com.rag.knowledgehub.mapper.DocumentChunkMapper;
import com.rag.knowledgehub.service.ElasticsearchVectorService;
import com.rag.knowledgehub.service.EmbeddingService;
import com.rag.knowledgehub.service.RagIngestionService;
import com.rag.knowledgehub.util.TextChunker;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class RagIngestionServiceImpl implements RagIngestionService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final DocumentChunkMapper documentChunkMapper;
    private final RagProperties ragProperties;

    public RagIngestionServiceImpl(EmbeddingService embeddingService,
                                   ElasticsearchVectorService elasticsearchVectorService,
                                   DocumentChunkMapper documentChunkMapper,
                                   RagProperties ragProperties) {
        this.embeddingService = embeddingService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.documentChunkMapper = documentChunkMapper;
        this.ragProperties = ragProperties;
    }

    @Override
    public void ingest(Long userId, Long knowledgeBaseId, Long documentId, String documentName, MultipartFile file) {
        try {
            String text = extractText(file);
            List<String> chunks = TextChunker.chunk(text, ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
            int i = 0;
            for (String chunkText : chunks) {
                List<Double> vector = embeddingService.embed(chunkText);
                String esDocId = documentId + "_" + i;
                elasticsearchVectorService.indexChunk(esDocId, knowledgeBaseId, documentId, documentName, i, chunkText, vector);

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setKnowledgeBaseId(knowledgeBaseId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setEsDocId(esDocId);
                documentChunkMapper.insert(chunk);
                i++;
            }
            log.info("Document {} ingested chunks={}", documentId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to ingest document", e);
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }
    }

    private String extractText(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename();
        if (name != null && name.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
