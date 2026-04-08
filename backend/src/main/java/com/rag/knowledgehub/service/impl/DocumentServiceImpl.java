package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import com.rag.knowledgehub.entity.Document;
import com.rag.knowledgehub.entity.KnowledgeBase;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.mapper.DocumentMapper;
import com.rag.knowledgehub.mapper.KnowledgeBaseMapper;
import com.rag.knowledgehub.service.DocumentService;
import com.rag.knowledgehub.service.ElasticsearchVectorService;
import com.rag.knowledgehub.service.RagIngestionService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RagIngestionService ragIngestionService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final RedissonClient redissonClient;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DocumentServiceImpl(DocumentMapper documentMapper,
                               KnowledgeBaseMapper knowledgeBaseMapper,
                               RagIngestionService ragIngestionService,
                               ElasticsearchVectorService elasticsearchVectorService,
                               RedissonClient redissonClient) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.ragIngestionService = ragIngestionService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.redissonClient = redissonClient;
    }

    @Override
    public DocumentVO upload(Long userId, Long knowledgeBaseId, MultipartFile file) {
        KnowledgeBase kb = checkKbOwner(userId, knowledgeBaseId);
        RLock lock = redissonClient.getLock("kb:upload:" + knowledgeBaseId);
        try {
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                Path dir = Paths.get(uploadDir);
                Files.createDirectories(dir);
                String suffix = getSuffix(file.getOriginalFilename());
                String fileKey = UUID.randomUUID() + suffix;
                Path target = dir.resolve(fileKey);
                file.transferTo(target.toFile());

                Document doc = new Document();
                doc.setKnowledgeBaseId(knowledgeBaseId);
                doc.setUserId(userId);
                doc.setFileName(file.getOriginalFilename());
                doc.setFileType(suffix.replace(".", ""));
                doc.setFileSize(file.getSize());
                doc.setStoragePath(fileKey);
                doc.setParseStatus("PROCESSING");
                documentMapper.insert(doc);

                try {
                    ragIngestionService.ingest(userId, knowledgeBaseId, doc.getId(), doc.getFileName(), file);
                    doc.setParseStatus("DONE");
                } catch (Exception e) {
                    doc.setParseStatus("FAILED");
                    documentMapper.updateById(doc);
                    throw e;
                }
                documentMapper.updateById(doc);

                kb.setDocCount((kb.getDocCount() == null ? 0 : kb.getDocCount()) + 1);
                knowledgeBaseMapper.updateById(kb);
                return toVO(doc);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传处理中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传失败，请重试");
    }

    @Override
    public PageResponse<DocumentVO> page(Long userId, Long knowledgeBaseId, int pageNum, int pageSize, String keyword) {
        checkKbOwner(userId, knowledgeBaseId);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Document::getFileName, keyword);
        }
        Page<Document> page = documentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DocumentVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResponse.<DocumentVO>builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    @Override
    public void delete(Long userId, Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        KnowledgeBase kb = checkKbOwner(userId, doc.getKnowledgeBaseId());
        documentMapper.deleteById(documentId);
        elasticsearchVectorService.deleteByDocumentId(documentId);
        Integer current = kb.getDocCount() == null ? 0 : kb.getDocCount();
        kb.setDocCount(Math.max(0, current - 1));
        knowledgeBaseMapper.updateById(kb);
        Path file = Paths.get(uploadDir, doc.getStoragePath());
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private KnowledgeBase checkKbOwner(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return kb;
    }

    private DocumentVO toVO(Document doc) {
        return DocumentVO.builder()
                .id(doc.getId())
                .knowledgeBaseId(doc.getKnowledgeBaseId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .parseStatus(doc.getParseStatus())
                .previewUrl("/files/" + doc.getStoragePath())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型不支持");
        }
        String suffix = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        if (!suffix.equals(".pdf") && !suffix.equals(".md") && !suffix.equals(".markdown") && !suffix.equals(".txt")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PDF/Markdown/TXT");
        }
        return suffix;
    }
}
