package com.rag.knowledgehub.controller;

import com.rag.knowledgehub.common.web.ApiResponse;
import com.rag.knowledgehub.dto.auth.AuthResponse;
import com.rag.knowledgehub.dto.auth.LoginRequest;
import com.rag.knowledgehub.dto.auth.RegisterRequest;
import com.rag.knowledgehub.dto.auth.UserProfile;
import com.rag.knowledgehub.security.SecurityUtils;
import com.rag.knowledgehub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "鉴权模块")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        Long uid = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(authService.currentUser(uid));
    }
}
