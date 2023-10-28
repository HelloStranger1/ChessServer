package com.hellostranger.chessserver.controller;


import com.google.gson.Gson;
import com.hellostranger.chessserver.controller.dto.websocket.*;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.game.Board;
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
                       whitePlayer, blackPlayer);

                sendMessageToAllPlayers(gameId, startMessage);
            }

        }

    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, GameNotFoundException {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        String messageJson = message.getPayload();

        log.info("handleTextMessage. Message is: "+ messageJson + "\n");

        Game currentGame = gameService.getGameById(gameId);

        Gson gson = new Gson();
        Message webSocketMessage = gson.fromJson(messageJson, Message.class);

        if(webSocketMessage.getMessageType() == MessageType.START){
            GameStartMessage startMessage = gson.fromJson(messageJson, GameStartMessage.class);
            sendMessageToAllPlayers(gameId, startMessage);
            return;
        }
        if(webSocketMessage.getMessageType() == MessageType.END){
            GameEndMessage endMessage = gson.fromJson(messageJson, GameEndMessage.class);
            sendMessageToAllPlayers(gameId, endMessage);
            return;
        }
        if(webSocketMessage.getMessageType() == MessageType.CONCEDE){
            if(currentGame.getMoveList() == null || currentGame.getMoveList().size() < 2){
                log.info("Game was aborted.");
                GameEndMessage endMessage = new GameEndMessage(GameState.ABORTED, "Game was aborted.");
                sendMessageToAllPlayers(gameId, endMessage);
                gameService.abortGame(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), currentGame);
                return;
            }
            ConcedeGameMessage concedeGameMessage = gson.fromJson(messageJson, ConcedeGameMessage.class);
            String causeMessage;
            GameState gameResult;
            if(Objects.equals(concedeGameMessage.getPlayerEmail(), currentGame.getWhitePlayer().getEmail())){
                causeMessage = "Black won by resignation";
                gameResult = GameState.BLACK_WIN;
            }else{
                causeMessage = "White won by resignation";
                gameResult = GameState.WHITE_WIN;
            }
            gameService.onGameEnding(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), gameResult, currentGame);
            GameEndMessage endMessage = new GameEndMessage(gameResult, causeMessage);
            sendMessageToAllPlayers(gameId, endMessage);
            return;
        }
        //Message is a Move
        MoveMessage moveMessage = gson.fromJson(messageJson, MoveMessage.class);

        if (currentGame.getGameState() != GameState.ACTIVE) {
            return;
        }
        Game updatedGame;
        /*boolean isCastleMove = false;
        try{
            isCastleMove = isCastle(currentGame, moveMessage);
        } catch (SquareNotFoundException e) {
            log.error("In HandleMessage, isCastle threw SquareNotFoundException e: " + e.getMsg());
        }*/

        try{
            updatedGame = gameService.makeMove(gameId, moveMessage);
        } catch (GameNotFoundException e){
            log.error("Game Not found, error: " + e.getMsg());
            return;
        } catch (GameFinishedException e){
            log.error("Game finished, error: " + e.getMsg());
            return;
        } catch (SquareNotFoundException e){
            log.error("Square Not Found, error: " + e.getMsg());
            return;
        } catch (InvalidMoveException e){
            log.error("Invalid Move, error: " + e.getMsg());
            InvalidMoveMessage msg = new InvalidMoveMessage();
            session.sendMessage(new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)));
            log.info("send: " + new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)) + "to " + session);
            return;
        }
        if(updatedGame != null){
            log.info("Updated game isn't null. state is: " + updatedGame.getGameState());
            sendMessageToAllPlayers(gameId, moveMessage);
            /*if (isCastleMove){
                MoveMessage[] castleMoves = gameService.convertCastleToMoves(moveMessage);
                sendMessageToAllPlayers(gameId, castleMoves[0]);
                sendMessageToAllPlayers(gameId, castleMoves[1]);
            }else{
                sendMessageToAllPlayers(gameId, moveMessage);
            }*/
            if (updatedGame.getGameState() == GameState.WHITE_WIN ||
                    updatedGame.getGameState() == GameState.BLACK_WIN ||
                    updatedGame.getGameState() == GameState.DRAW) {
                //either a win for one of the players or a draw
                GameEndMessage endMessage = new GameEndMessage(updatedGame.getGameState(), "Someone won");
                log.info("game move list is: "+ updatedGame.getMoveList());
                sendMessageToAllPlayers(gameId, endMessage);
            }
        }else{
            log.info("\n \n Updated game is null ?!?!?!?!?! \n \n");
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

    /*private boolean isCastle(Game game, MoveMessage moveMessage) throws SquareNotFoundException {
        Board board = game.getBoard();
        return board.isCastlingMove(
                board.getSquareAt(moveMessage.getStartCol(), moveMessage.getStartRow()),
                board.getSquareAt(moveMessage.getEndCol(), moveMessage.getEndRow()));

    }*/

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
