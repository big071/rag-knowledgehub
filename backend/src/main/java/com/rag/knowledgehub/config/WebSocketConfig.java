package com.rag.knowledgehub.config;

import com.rag.knowledgehub.websocket.JwtHandshakeInterceptor;
import com.rag.knowledgehub.websocket.QaWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final QaWebSocketHandler qaWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(QaWebSocketHandler qaWebSocketHandler, JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.qaWebSocketHandler = qaWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(qaWebSocketHandler, "/ws/qa")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
