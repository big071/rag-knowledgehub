package com.rag.knowledgehub.common.web;

import com.rag.knowledgehub.entity.OperationLog;
import com.rag.knowledgehub.mapper.OperationLogMapper;
import com.rag.knowledgehub.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogMapper operationLogMapper;

    public OperationLogInterceptor(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/me")) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setUserId(SecurityUtils.getCurrentUserId());
        log.setUsername(SecurityUtils.getCurrentUsername());
        log.setRole(SecurityUtils.getCurrentRole());
        log.setAction(request.getMethod() + " " + request.getRequestURI());
        log.setMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setIp(request.getRemoteAddr());
        log.setSuccess(ex == null && response.getStatus() < 400);
        log.setDetail(ex == null ? null : ex.getMessage());
        operationLogMapper.insert(log);
    }
}
