package com.rag.knowledgehub.service;

import com.rag.knowledgehub.dto.auth.AuthResponse;
import com.rag.knowledgehub.dto.auth.ChangePasswordRequest;
import com.rag.knowledgehub.dto.auth.LoginRequest;
import com.rag.knowledgehub.dto.auth.RegisterRequest;
import com.rag.knowledgehub.dto.auth.UserProfile;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfile currentUser(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);
}
