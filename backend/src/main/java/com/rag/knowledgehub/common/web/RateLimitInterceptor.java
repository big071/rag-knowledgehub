package com.rag.knowledgehub.common.web;

import com.google.common.util.concurrent.RateLimiter;
import com.rag.knowledgehub.common.exception.BusinessException;
import com.rag.knowledgehub.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getRequestURI() + ":" + request.getRemoteAddr();
        RateLimiter limiter = limiterMap.computeIfAbsent(key, k -> RateLimiter.create(2.0));
        if (!limiter.tryAcquire()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT);
        }
        return true;
    }
}
