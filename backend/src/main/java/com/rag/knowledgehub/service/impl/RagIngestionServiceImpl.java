package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
public class RagIngestionServiceImpl implements RagIngestionService {

    private static final Set<String> IMAGE_TYPES = Set.of("png", "jpg", "jpeg", "bmp", "gif", "webp");

    private final EmbeddingService embeddingService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final DocumentChunkMapper documentChunkMapper;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public RagIngestionServiceImpl(EmbeddingService embeddingService,
                                   ElasticsearchVectorService elasticsearchVectorService,
                                   DocumentChunkMapper documentChunkMapper,
                                   RagProperties ragProperties,
                                   ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.documentChunkMapper = documentChunkMapper;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void ingest(Long userId, Long knowledgeBaseId, Long documentId, String documentName, MultipartFile file) {
        ingest(userId, knowledgeBaseId, documentId, documentName, file, null);
    }

    @Override
    public void ingest(Long userId, Long knowledgeBaseId, Long documentId, String documentName, MultipartFile file, Long previousDocumentId) {
        try {
            String text = extractText(file);
            List<String> chunks = TextChunker.chunk(text, ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
            Map<String, List<Double>> oldVectors = loadOldVectors(previousDocumentId);

            int i = 0;
            for (String chunkText : chunks) {
                String contentHash = sha256(chunkText);
                List<Double> vector = oldVectors.get(contentHash);
                if (vector == null) {
                    vector = embeddingService.embed(chunkText);
                }
                String esDocId = documentId + "_" + i;
                elasticsearchVectorService.indexChunk(esDocId, knowledgeBaseId, documentId, documentName, i, chunkText, vector);

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setKnowledgeBaseId(knowledgeBaseId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setContentHash(contentHash);
                chunk.setVectorJson(objectMapper.writeValueAsString(vector));
                chunk.setEsDocId(esDocId);
                documentChunkMapper.insert(chunk);
                i++;
            }
            log.info("Document {} ingested chunks={}, reusedVectors={}", documentId, chunks.size(), countReuse(chunks, oldVectors));
        } catch (Exception e) {
            log.error("Failed to ingest document", e);
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }
    }

    public String extractText(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename();
        String suffix = suffix(name);
        if ("pdf".equals(suffix)) {
            return readPdf(file.getBytes());
        }
        if ("docx".equals(suffix) || "doc".equals(suffix)) {
            return readWord(file.getBytes());
        }
        if ("xlsx".equals(suffix) || "xls".equals(suffix)) {
            return readExcel(file.getBytes());
        }
        if ("pptx".equals(suffix) || "ppt".equals(suffix)) {
            return readPpt(file.getBytes());
        }
        if (IMAGE_TYPES.contains(suffix)) {
            return "[图片文档] 文件名: " + (name == null ? "未知" : name) + "。系统已收录该图片，暂不进行OCR抽取。";
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private String readPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String readWord(byte[] bytes) throws IOException {
        try {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                StringBuilder sb = new StringBuilder();
                document.getParagraphs().forEach(p -> sb.append(p.getText()).append('\n'));
                document.getTables().forEach(t -> sb.append(t.getText()).append('\n'));
                return sb.toString();
            }
        } catch (Exception ex) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private String readExcel(byte[] bytes) throws IOException, InvalidFormatException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                sb.append("# ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    row.forEach(cell -> sb.append(formatter.formatCellValue(cell)).append('\t'));
                    sb.append('\n');
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String readPpt(byte[] bytes) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(bytes));
             XMLSlideShow ppt = new XMLSlideShow(pkg)) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                sb.append("# Slide").append('\n');
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        sb.append(textShape.getText()).append('\n');
                    }
                }
                sb.append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private Map<String, List<Double>> loadOldVectors(Long previousDocumentId) {
        if (previousDocumentId == null) {
            return Map.of();
        }
        List<DocumentChunk> oldChunks = documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, previousDocumentId));
        Map<String, List<Double>> map = new HashMap<>();
        for (DocumentChunk chunk : oldChunks) {
            if (chunk.getVectorJson() == null || chunk.getContentHash() == null) {
                continue;
            }
            try {
                List<Double> vector = objectMapper.readValue(chunk.getVectorJson(), new TypeReference<>() {
                });
                map.put(chunk.getContentHash(), vector);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private int countReuse(List<String> chunks, Map<String, List<Double>> oldVectors) {
        int n = 0;
        for (String chunk : chunks) {
            if (oldVectors.containsKey(sha256(chunk))) {
                n++;
            }
        }
        return n;
    }

    private String suffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(source.hashCode());
        }
    }
}
