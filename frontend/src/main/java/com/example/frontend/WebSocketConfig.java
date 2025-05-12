package com.example.frontend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * <p>Configuration class for enabling and setting up WebSocket messaging using STOMP protocol in Spring Boot.</p>
 *
 * <p>This class registers WebSocket endpoints and configures the message broker
 * for broadcasting real-time updates such as system statistics.</p>
 */
@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket messaging
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker to handle routing messages between clients and the server.
     *
     * @param config the message broker registry to be configured.
     *               Enables a simple in-memory broker with destination prefix "/topicGloogloo/statistics".
     */
    @Override
    public void configureMessageBroker(@SuppressWarnings("null") MessageBrokerRegistry config) {
        // Enabling a simple in-memory message broker to route messages to clients
        config.enableSimpleBroker("/topicGloogloo/statistics");  // All messages sent to "/topic" are broadcasted
    }

    /**
     * Registers the STOMP WebSocket endpoint and enables SockJS fallback for browsers that do not support WebSockets.
     *
     * @param registry the STOMP endpoint registry to register endpoints with.
     */
    @Override
    public void registerStompEndpoints(@SuppressWarnings("null") StompEndpointRegistry registry) {
        // Registering the WebSocket endpoint, enabling SockJS fallback for unsupported browsers
        registry.addEndpoint("/ws").withSockJS();  // This will be the endpoint the client connects to
    }
}
