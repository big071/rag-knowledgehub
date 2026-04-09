package com.rag.knowledgehub.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.common.web.PageResponse;
import com.rag.knowledgehub.dto.auth.UpdateUserRequest;
import com.rag.knowledgehub.dto.auth.UserProfile;
import com.rag.knowledgehub.entity.Document;
import com.rag.knowledgehub.entity.OperationLog;
import com.rag.knowledgehub.entity.QaRecord;
import com.rag.knowledgehub.entity.SystemConfig;
import com.rag.knowledgehub.entity.User;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.mapper.DocumentMapper;
import com.rag.knowledgehub.mapper.OperationLogMapper;
import com.rag.knowledgehub.mapper.QaRecordMapper;
import com.rag.knowledgehub.mapper.SystemConfigMapper;
import com.rag.knowledgehub.mapper.UserMapper;
import com.rag.knowledgehub.security.RoleConstants;
import com.rag.knowledgehub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员模块")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final OperationLogMapper operationLogMapper;
    private final DocumentMapper documentMapper;
    private final QaRecordMapper qaRecordMapper;

    public AdminController(UserMapper userMapper,
                           SystemConfigMapper systemConfigMapper,
                           OperationLogMapper operationLogMapper,
                           DocumentMapper documentMapper,
                           QaRecordMapper qaRecordMapper) {
        this.userMapper = userMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.operationLogMapper = operationLogMapper;
        this.documentMapper = documentMapper;
        this.qaRecordMapper = qaRecordMapper;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<UserProfile>> users() {
        List<UserProfile> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getId))
                .stream()
                .map(this::toProfile)
                .toList();
        return ApiResponse.success(users);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String role = RoleConstants.normalize(request.getRole());
        if (!RoleConstants.USER.equals(role) && !RoleConstants.DOC_ADMIN.equals(role) && !RoleConstants.SUPER_ADMIN.equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效角色");
        }
        user.setNickname(request.getNickname());
        user.setRole(role);
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        userMapper.updateById(user);
        return ApiResponse.success("更新成功", null);
    }

    @Operation(summary = "系统配置列表")
    @GetMapping("/configs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<SystemConfig>> configs() {
        return ApiResponse.success(systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getConfigKey)));
    }

    @Operation(summary = "保存系统配置")
    @PostMapping("/configs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> saveConfig(@RequestBody SystemConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置键不能为空");
        }
        SystemConfig exists = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, config.getConfigKey()));
        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        if (exists == null) {
            systemConfigMapper.insert(config);
        } else {
            exists.setConfigValue(config.getConfigValue());
            exists.setDescription(config.getDescription());
            exists.setUpdatedBy(SecurityUtils.getCurrentUserId());
            systemConfigMapper.updateById(exists);
        }
        return ApiResponse.success("保存成功", null);
    }

    @Operation(summary = "操作日志")
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<PageResponse<OperationLog>> logs(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(OperationLog::getAction, keyword)
                    .or().like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getRequestUri, keyword));
        }
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return ApiResponse.success(PageResponse.<OperationLog>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
    }

    @Operation(summary = "管理员概览统计")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('DOC_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> overview() {
        Long totalDocs = documentMapper.selectCount(new LambdaQueryWrapper<Document>());
        Long totalQa = qaRecordMapper.selectCount(new LambdaQueryWrapper<QaRecord>());
        List<Map<String, Object>> hotQuestions = qaRecordMapper.selectList(new LambdaQueryWrapper<QaRecord>()
                        .select(QaRecord::getQuestion)
                        .orderByDesc(QaRecord::getId)
                        .last("limit 300"))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(QaRecord::getQuestion, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();
        return ApiResponse.success(Map.of(
                "totalDocuments", totalDocs,
                "totalConversations", totalQa,
                "hotQuestions", hotQuestions
        ));
    }

    private UserProfile toProfile(User u) {
        return UserProfile.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .role(RoleConstants.normalize(u.getRole()))
                .enabled(u.getEnabled())
                .build();
    }
}

