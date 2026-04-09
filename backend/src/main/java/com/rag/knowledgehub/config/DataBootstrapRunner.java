package com.rag.knowledgehub.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.knowledgehub.entity.SystemConfig;
import com.rag.knowledgehub.entity.User;
import com.rag.knowledgehub.mapper.SystemConfigMapper;
import com.rag.knowledgehub.mapper.UserMapper;
import com.rag.knowledgehub.security.RoleConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataBootstrapRunner implements CommandLineRunner {

    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrapRunner(UserMapper userMapper,
                               SystemConfigMapper systemConfigMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureUser("user_qa", "Qa@123456", "普通用户", RoleConstants.USER);
        ensureUser("doc_admin", "Doc@123456", "文档管理员", RoleConstants.DOC_ADMIN);
        ensureUser("super_admin", "Super@123456", "超级管理员", RoleConstants.SUPER_ADMIN);

        ensureConfig("qa.context.rounds", "5", "多轮对话轮数");
        ensureConfig("kb.review.required", "true", "文档默认需要审核");
    }

    private void ensureUser(String username, String rawPassword, String nickname, String role) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setRole(role);
        user.setEnabled(true);
        userMapper.insert(user);
    }

    private void ensureConfig(String key, String value, String desc) {
        SystemConfig exists = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        if (exists != null) {
            return;
        }
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(desc);
        systemConfigMapper.insert(config);
    }
}
