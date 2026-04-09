package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseCreateRequest;
import com.rag.knowledgehub.dto.kb.KnowledgeBaseVO;
import com.rag.knowledgehub.entity.KnowledgeBase;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.mapper.KnowledgeBaseMapper;
import com.rag.knowledgehub.security.PermissionUtils;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public KnowledgeBaseVO create(Long userId, KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setDocCount(0);
        knowledgeBaseMapper.insert(kb);
        return toVO(kb);
    }

    @Override
    public PageResponse<KnowledgeBaseVO> page(Long userId, int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .orderByDesc(KnowledgeBase::getId);
        if (!PermissionUtils.isDocAdmin(SecurityUtils.getCurrentRole())) {
            // 普通用户可检索问答，允许读取全局已建知识库
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KnowledgeBase::getName, keyword);
        }
        Page<KnowledgeBase> page = knowledgeBaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<KnowledgeBaseVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResponse.<KnowledgeBaseVO>builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    @Override
    public void delete(Long userId, Long kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (!PermissionUtils.isDocAdmin(SecurityUtils.getCurrentRole()) && !kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        knowledgeBaseMapper.deleteById(kbId);
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .docCount(kb.getDocCount())
                .createdAt(kb.getCreatedAt())
                .build();
    }
}
