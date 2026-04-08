package com.rag.knowledgehub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.dto.auth.AuthResponse;
import com.rag.knowledgehub.dto.auth.LoginRequest;
import com.rag.knowledgehub.dto.auth.RegisterRequest;
import com.rag.knowledgehub.dto.auth.UserProfile;
import com.rag.knowledgehub.entity.User;
import com.rag.knowledgehub.enums.ErrorCode;
import com.rag.knowledgehub.mapper.UserMapper;
import com.rag.knowledgehub.security.JwtTokenUtil;
import com.rag.knowledgehub.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        userMapper.insert(user);

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return AuthResponse.builder().token(token).user(toProfile(user)).build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return AuthResponse.builder().token(token).user(toProfile(user)).build();
    }

    @Override
    public UserProfile currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toProfile(user);
    }

    private UserProfile toProfile(User user) {
        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
