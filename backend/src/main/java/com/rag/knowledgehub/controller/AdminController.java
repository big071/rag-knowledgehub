package com.rag.knowledgehub.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.dto.auth.UserProfile;
import com.rag.knowledgehub.entity.User;
import com.rag.knowledgehub.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员模块")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;

    public AdminController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "用户列表（管理员）")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserProfile>> users() {
        List<UserProfile> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getId))
                .stream()
                .map(u -> UserProfile.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .role(u.getRole())
                        .build())
                .toList();
        return ApiResponse.success(users);
    }
}
