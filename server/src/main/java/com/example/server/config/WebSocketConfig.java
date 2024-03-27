package com.example.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@EnableWebSocketMessageBroker
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  //웹소켓의 엔드포인트를 추가합니다. witSockJS()는 웹소켓을 지원하지 않는 브라우저에서도 작동하게 합니다.
                .setAllowedOrigins("http://localhost:8080")
                .withSockJS();
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/pub");     // 이 주소로 메시지 전송한다.
        registry.enableSimpleBroker("/sub");   // 이 주소로 매시지 전달 받는다.
    }
}
