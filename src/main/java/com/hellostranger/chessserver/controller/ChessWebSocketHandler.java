package com.hellostranger.chessserver.controller;


import com.google.gson.Gson;
import com.hellostranger.chessserver.controller.dto.websocket.*;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.game.Game;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.service.GameService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@AllArgsConstructor

public class ChessWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, Set<WebSocketSession>> gameSessions = new ConcurrentHashMap<>();
    private final GameService gameService;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if (gameId != null) {
            // Add the session to the corresponding game session set
            gameSessions.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

            Game currentGame = gameService.getGameById(gameId);

            if(currentGame.getGameState() == GameState.ACTIVE){
                User whitePlayer = currentGame.getWhitePlayer();
                User blackPlayer = currentGame.getBlackPlayer();
                GameStartMessage startMessage = new GameStartMessage(
                        whitePlayer.getName(),
                        blackPlayer.getName(),
                        whitePlayer.getEmail(),
                        blackPlayer.getEmail(),
                        whitePlayer.getElo(),
                        blackPlayer.getElo());

                sendMessageToAllPlayers(gameId, startMessage);
            }

        }

    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, GameNotFoundException {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());

        String messageJson = message.getPayload();
        log.info("handleTextMessage: "+ messageJson);
        Game currentGame = gameService.getGameById(gameId);
        Gson gson = new Gson();
        Message webSocketMessage = gson.fromJson(messageJson, Message.class);
        log.info("Websocket message : " + webSocketMessage);
        if(webSocketMessage.getMessageType() == MessageType.MOVE){
            MoveMessage moveMessage = gson.fromJson(messageJson, MoveMessage.class);
            log.info("handle move, " + gameId + "moveMsg: " + moveMessage + "Session: " + session);

            if (currentGame.getGameState() != GameState.ACTIVE) {
                return;
            }
            Game updatedGame;
            boolean isCastleMove = false;
            try{
                isCastleMove = isCastle(currentGame, moveMessage);
            } catch (SquareNotFoundException e) {
                log.info("idk checking if castle websocket. e: " + e.getMsg());
            }

            try{
                updatedGame = gameService.makeMove(gameId, moveMessage.getPlayerEmail(),
                        moveMessage.getStartCol(),
                        moveMessage.getStartRow(),
                        moveMessage.getEndCol(),
                        moveMessage.getEndRow());
            } catch (GameNotFoundException e){
                log.info("Not found, error: " + e.getMsg());
                return;
            } catch (GameFinishedException e){
                log.info("Game finished, error: " + e.getMsg());
                return;
            } catch (SquareNotFoundException e){
                log.info("Square Not Found, error: " + e.getMsg());
                return;
            } catch (InvalidMoveException e){
                log.info("Invalid Move, error: " + e.getMsg());
                InvalidMoveMessage msg = new InvalidMoveMessage();
                session.sendMessage(new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)));
                log.info("send: " + new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)) + "to " + session);
                return;
            }
            if(updatedGame != null){
                log.info("Updated game isn't null. state is: " + updatedGame.getGameState());
                if (isCastleMove){
                    MoveMessage[] castleMoves = gameService.convertCastleToMoves(moveMessage);
                    sendMessageToAllPlayers(gameId, castleMoves[0]);
                    sendMessageToAllPlayers(gameId, castleMoves[1]);
                }else{
                    sendMessageToAllPlayers(gameId, moveMessage);
                }
                if (updatedGame.getGameState() == GameState.WHITE_WIN ||
                        updatedGame.getGameState() == GameState.BLACK_WIN ||
                        updatedGame.getGameState() == GameState.DRAW) {
                    //either a win for one of the players or a draw
                    GameEndMessage endMessage = new GameEndMessage(updatedGame.getGameState());
                    log.info("game move list is: "+ updatedGame.getMoveList());
                    sendMessageToAllPlayers(gameId, endMessage);
                }
            }else{
                log.info("\n \n Updated game is null ?!?!?!?!?! \n \n");
            }


        } else if(webSocketMessage.getMessageType() == MessageType.START){
            GameStartMessage startMessage = gson.fromJson(messageJson, GameStartMessage.class);
            sendMessageToAllPlayers(gameId, startMessage);
        } else if(webSocketMessage.getMessageType() == MessageType.END){
            GameEndMessage endMessage = gson.fromJson(messageJson, GameEndMessage.class);
            sendMessageToAllPlayers(gameId, endMessage);
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if (gameId != null) {
            Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
            if (sessionsInGame != null) {
                sessionsInGame.remove(session);
            }
        }
    }

    private String extractGameId(String path) {
        // Extract the gameId from the WebSocket URL path
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private boolean isCastle(Game game, MoveMessage moveMessage) throws SquareNotFoundException {
        return gameService.isCastle(game,
                game.getBoard().getSquareAt(moveMessage.getStartCol(), moveMessage.getStartRow()),
                game.getBoard().getSquareAt(moveMessage.getEndCol(), moveMessage.getEndRow()));
    }

    private void sendMessageToAllPlayers(String gameId, Message message) throws IOException {
        Gson gson = new Gson();
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);

        if (sessionsInGame != null) {
            for (WebSocketSession s : sessionsInGame) {
                s.sendMessage(new TextMessage(gson.toJson(message)));
            }
        }
    }

}
