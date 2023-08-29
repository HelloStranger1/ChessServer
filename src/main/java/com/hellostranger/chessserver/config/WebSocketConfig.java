package com.hellostranger.chessserver.config;

import com.hellostranger.chessserver.controller.ChessWebSocketHandler;
import com.hellostranger.chessserver.service.GameService;
import com.hellostranger.chessserver.storage.BoardRepresentationRepository;
import com.hellostranger.chessserver.storage.GameRepresentationRepository;
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

    private final UserRepository userRepository;
    private final GameRepresentationRepository gameRepresentationRepository;
    private final BoardRepresentationRepository boardRepresentationRepository;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chessWebSocketHandler(), "/chess/{gameId}").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler chessWebSocketHandler() {
        return new ChessWebSocketHandler(new GameService(userRepository, gameRepresentationRepository, boardRepresentationRepository));
    }
}
