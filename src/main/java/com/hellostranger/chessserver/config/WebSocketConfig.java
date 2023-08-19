package com.hellostranger.chessserver.config;

import com.hellostranger.chessserver.controller.ChessWebSocketHandler;
import com.hellostranger.chessserver.service.GameService;
import com.hellostranger.chessserver.storage.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserRepository repository;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chessWebSocketHandler(), "/chess/{gameId}").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler chessWebSocketHandler() {
        return new ChessWebSocketHandler(new GameService(repository));
    }
}
