package com.example.frontend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket messaging
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enabling a simple in-memory message broker to route messages to clients
        config.enableSimpleBroker("/topicGloogloo/statistics");  // All messages sent to "/topic" are broadcasted
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registering the WebSocket endpoint, enabling SockJS fallback for unsupported browsers
        registry.addEndpoint("/ws").withSockJS();  // This will be the endpoint the client connects to
    }
}
