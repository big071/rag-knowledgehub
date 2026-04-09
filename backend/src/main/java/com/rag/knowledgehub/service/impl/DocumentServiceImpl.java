package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.DocumentVO;
import com.rag.knowledgehub.entity.*;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.mapper.*;
import com.rag.knowledgehub.security.PermissionUtils;
import com.rag.knowledgehub.security.SecurityUtils;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_SUFFIX = Set.of("pdf", "md", "markdown", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "png", "jpg", "jpeg", "bmp", "gif", "webp");

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentTagMapper documentTagMapper;
    private final DocumentTagRelMapper documentTagRelMapper;
    private final RagIngestionService ragIngestionService;
    private final ElasticsearchVectorService elasticsearchVectorService;
    private final RedissonClient redissonClient;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DocumentServiceImpl(DocumentMapper documentMapper,
                               KnowledgeBaseMapper knowledgeBaseMapper,
                               DocumentTagMapper documentTagMapper,
                               DocumentTagRelMapper documentTagRelMapper,
                               RagIngestionService ragIngestionService,
                               ElasticsearchVectorService elasticsearchVectorService,
                               RedissonClient redissonClient) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentTagMapper = documentTagMapper;
        this.documentTagRelMapper = documentTagRelMapper;
        this.ragIngestionService = ragIngestionService;
        this.elasticsearchVectorService = elasticsearchVectorService;
        this.redissonClient = redissonClient;
    }

    @Override
    public DocumentVO upload(Long userId, Long knowledgeBaseId, MultipartFile file) {
        checkManagePermission(userId, knowledgeBaseId);
        return doUpload(userId, knowledgeBaseId, file, null, 1, null);
    }

    @Override
    public List<DocumentVO> batchUpload(Long userId, Long knowledgeBaseId, List<MultipartFile> files) {
        checkManagePermission(userId, knowledgeBaseId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少上传一个文件");
        }
        List<DocumentVO> list = new ArrayList<>();
        for (MultipartFile file : files) {
            list.add(doUpload(userId, knowledgeBaseId, file, null, 1, null));
        }
        return list;
    }

    @Override
    public DocumentVO uploadNewVersion(Long userId, Long documentId, MultipartFile file) {
        Document old = mustGetDocument(documentId);
        checkManagePermission(userId, old.getKnowledgeBaseId());

        old.setLatest(false);
        documentMapper.updateById(old);

        int nextVersion = (old.getVersionNo() == null ? 1 : old.getVersionNo()) + 1;
        return doUpload(userId, old.getKnowledgeBaseId(), file, old.getId(), nextVersion, old.getId());
    }

    @Override
    public PageResponse<DocumentVO> page(Long userId, Long knowledgeBaseId, int pageNum, int pageSize, String keyword,
                                         String fileType, String startTime, String endTime, String reviewStatus) {
        checkReadPermission(userId, knowledgeBaseId);
        String role = SecurityUtils.getCurrentRole();

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getId);

        if (!PermissionUtils.isDocAdmin(role)) {
            wrapper.eq(Document::getLatest, true).eq(Document::getReviewStatus, "APPROVED");
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Document::getFileName, keyword);
        }
        if (StringUtils.hasText(fileType)) {
            wrapper.eq(Document::getFileType, fileType.toLowerCase());
        }
        if (StringUtils.hasText(reviewStatus)) {
            wrapper.eq(Document::getReviewStatus, reviewStatus);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Document::getCreatedAt, parseDateTime(startTime));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Document::getCreatedAt, parseDateTime(endTime));
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
    public void review(Long reviewerId, Long documentId, String reviewStatus, String reviewComment) {
        Document doc = mustGetDocument(documentId);
        String value = reviewStatus == null ? "" : reviewStatus.trim().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(value) && !"REJECTED".equals(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "reviewStatus 仅支持 APPROVED/REJECTED");
        }
        doc.setReviewStatus(value);
        doc.setReviewedBy(reviewerId);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewComment(reviewComment);
        documentMapper.updateById(doc);
    }

    @Override
    public void setTags(Long userId, Long documentId, String tags) {
        Document doc = mustGetDocument(documentId);
        checkManagePermission(userId, doc.getKnowledgeBaseId());

        documentTagRelMapper.delete(new LambdaQueryWrapper<DocumentTagRel>().eq(DocumentTagRel::getDocumentId, documentId));
        if (!StringUtils.hasText(tags)) {
            return;
        }
        Set<String> names = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String name : names) {
            DocumentTag tag = documentTagMapper.selectOne(new LambdaQueryWrapper<DocumentTag>().eq(DocumentTag::getName, name));
            if (tag == null) {
                tag = new DocumentTag();
                tag.setName(name);
                documentTagMapper.insert(tag);
            }
            DocumentTagRel rel = new DocumentTagRel();
            rel.setDocumentId(documentId);
            rel.setTagId(tag.getId());
            documentTagRelMapper.insert(rel);
        }
    }

    @Override
    public void batchDelete(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            delete(userId, id);
        }
    }

    @Override
    public void delete(Long userId, Long documentId) {
        Document doc = mustGetDocument(documentId);
        checkManagePermission(userId, doc.getKnowledgeBaseId());

        documentMapper.deleteById(documentId);
        elasticsearchVectorService.deleteByDocumentId(documentId);
        documentTagRelMapper.delete(new LambdaQueryWrapper<DocumentTagRel>().eq(DocumentTagRel::getDocumentId, documentId));

        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
        if (kb != null) {
            Integer current = kb.getDocCount() == null ? 0 : kb.getDocCount();
            kb.setDocCount(Math.max(0, current - 1));
            knowledgeBaseMapper.updateById(kb);
        }

        Path file = Paths.get(uploadDir, doc.getStoragePath());
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private DocumentVO doUpload(Long userId, Long knowledgeBaseId, MultipartFile file, Long parentDocumentId,
                                int versionNo, Long previousDocumentId) {
        RLock lock = redissonClient.getLock("kb:upload:" + knowledgeBaseId);
        try {
            if (!lock.tryLock(0, 20, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传繁忙，请稍后重试");
            }

            String suffix = getSuffix(file.getOriginalFilename());
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            byte[] raw = file.getBytes();
            String fileKey = UUID.randomUUID() + "." + suffix;
            Path target = dir.resolve(fileKey);
            file.transferTo(target.toFile());

            Document doc = new Document();
            doc.setKnowledgeBaseId(knowledgeBaseId);
            doc.setUserId(userId);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileType(suffix);
            doc.setFileSize(file.getSize());
            doc.setStoragePath(fileKey);
            doc.setSourceHash(sha256(raw));
            doc.setParseStatus("PROCESSING");
            doc.setReviewStatus("PENDING");
            doc.setVersionNo(versionNo);
            doc.setParentDocumentId(parentDocumentId);
            doc.setLatest(true);

            SensitiveResult sensitive = detectSensitive(new String(raw, StandardCharsets.UTF_8));
            doc.setSensitiveHit(sensitive.hit());
            doc.setSensitiveTip(sensitive.tip());
            documentMapper.insert(doc);

            try {
                ragIngestionService.ingest(userId, knowledgeBaseId, doc.getId(), doc.getFileName(), file, previousDocumentId);
                doc.setParseStatus("DONE");
            } catch (Exception e) {
                doc.setParseStatus("FAILED");
                documentMapper.updateById(doc);
                throw e;
            }
            documentMapper.updateById(doc);

            KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
            if (kb != null) {
                kb.setDocCount((kb.getDocCount() == null ? 0 : kb.getDocCount()) + 1);
                knowledgeBaseMapper.updateById(kb);
            }
            return toVO(doc);
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
    }

    private void checkManagePermission(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        String role = SecurityUtils.getCurrentRole();
        if (PermissionUtils.isDocAdmin(role)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无文档管理权限");
    }

    private void checkReadPermission(Long userId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
    }

    private Document mustGetDocument(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return doc;
    }

    private DocumentVO toVO(Document doc) {
        List<Long> tagIds = documentTagRelMapper.selectList(new LambdaQueryWrapper<DocumentTagRel>()
                        .eq(DocumentTagRel::getDocumentId, doc.getId()))
                .stream().map(DocumentTagRel::getTagId).toList();
        String tags = "";
        if (!tagIds.isEmpty()) {
            tags = documentTagMapper.selectBatchIds(tagIds).stream().map(DocumentTag::getName).collect(Collectors.joining(","));
        }

        return DocumentVO.builder()
                .id(doc.getId())
                .knowledgeBaseId(doc.getKnowledgeBaseId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .parseStatus(doc.getParseStatus())
                .reviewStatus(doc.getReviewStatus())
                .versionNo(doc.getVersionNo())
                .parentDocumentId(doc.getParentDocumentId())
                .latest(doc.getLatest())
                .sensitiveHit(doc.getSensitiveHit())
                .sensitiveTip(doc.getSensitiveTip())
                .tags(tags)
                .previewUrl("/files/" + doc.getStoragePath())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型不支持");
        }
        String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SUFFIX.contains(suffix)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PDF/Markdown/TXT/Word/Excel/PPT/图片");
        }
        return suffix;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            if (value.length() == 10) {
                return LocalDateTime.parse(value + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间格式请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(bytes));
        }
    }

    private SensitiveResult detectSensitive(String text) {
        if (!StringUtils.hasText(text)) {
            return new SensitiveResult(false, null);
        }
        boolean phone = text.matches("(?s).*1[3-9]\\d{9}.*");
        boolean idCard = text.matches("(?s).*[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])([0-2][1-9]|10|20|30|31)\\d{3}[0-9Xx].*");
        if (phone || idCard) {
            return new SensitiveResult(true, "检测到手机号或身份证号，请先脱敏后分发");
        }
        return new SensitiveResult(false, null);
    }

    private record SensitiveResult(boolean hit, String tip) {
    }
}
