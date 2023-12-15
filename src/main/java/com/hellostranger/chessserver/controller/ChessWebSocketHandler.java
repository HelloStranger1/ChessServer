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


    /**
     * Called when someone connects to the Websocket.
     * If there are 2 players in the game, starts it.
     * @param session The WebSocketSession of the player who is joining
     * @throws GameNotFoundException Thrown if are trying to join a non-existent game.
     * @throws IOException Thrown if there was an I.O. error when trying to send the GameStartMessage
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws GameNotFoundException, IOException {
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

    /**
     * Called whenever a player sends a message.
     * @param session The relevant WebSocketSession of the game
     * @param message One of the possible Messages (Detailed in dto.websocket), in JSON.
     * @throws IOException Thrown if there was an error when trying to send a message
     * @throws GameNotFoundException Thrown if are trying to join a non-existent game.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, GameNotFoundException {

        //Extracting gameId and the message
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        String messageJson = message.getPayload();

        Game currentGame = gameService.getGameById(gameId);

        //Converting the Json into a Message object, the parent object of all other messages.
        Gson gson = new Gson();
        Message webSocketMessage = gson.fromJson(messageJson, Message.class);
        MessageType messageType = webSocketMessage.getMessageType();

        //If this is a start or end message we just want to send it, without changing, to everyone else
        if(messageType == MessageType.END || messageType == MessageType.START){
            passTextMessageToAllPlayers(gameId, message);
        }

        if(webSocketMessage.getMessageType() == MessageType.CONCEDE){
            handleConcedeMessage(currentGame, messageJson);
        }

        if (currentGame.getGameState() != GameState.ACTIVE || messageType != MessageType.MOVE) {
            return;
        }

        //Message is a Move
        MoveMessage moveMessage = gson.fromJson(messageJson, MoveMessage.class);
        Game updatedGame = makeMove(gameId, moveMessage, session);
        if(updatedGame == null){
            return;
        }
        sendMessageToAllPlayers(gameId, moveMessage);
        GameEndMessage endMessage = getGameEndMessage(updatedGame);
        if(endMessage != null){
            sendMessageToAllPlayers(gameId, endMessage);
        }
    }


    /**
     * Matches the correct message for a finished game
     * @param updatedGame The game
     * @return a GameEndMessage to be sent to all players. Can be null, if there was an error.
     */
    private static GameEndMessage getGameEndMessage(Game updatedGame) {
        GameEndMessage endMessage = null;
        if(updatedGame.getGameState() == GameState.WHITE_WIN){
            endMessage = new GameEndMessage(updatedGame.getGameState(), "White won");
        } else if(updatedGame.getGameState() == GameState.BLACK_WIN){
            endMessage = new GameEndMessage(updatedGame.getGameState(), "Black won");
        } else if(updatedGame.getGameState() == GameState.DRAW){
            endMessage = new GameEndMessage(updatedGame.getGameState(), "Game ended in a draw");
        }
        return endMessage;
    }

    /**
     * Handles all the relevant exceptions to making a move
     * @param gameId The id of the game
     * @param moveMessage The move to play
     * @param session The WebSocketSession of the player who is playing the move
     * @return The updated game ofter the move. Can be null, if there was an error.
     */

    private Game makeMove(String gameId, MoveMessage moveMessage, WebSocketSession session){
        Game game = null;
        try{
            game = gameService.makeMove(gameId, moveMessage);
        }catch (GameNotFoundException e){
            log.error("Game Not found, error: " + e.getMsg());
        } catch (GameFinishedException e){
            log.error("Game finished, error: " + e.getMsg());
        } catch (SquareNotFoundException e){
            log.error("Square Not Found, error: " + e.getMsg());
        } catch (InvalidMoveException e) {
            log.error("Invalid Move, error: " + e.getMsg());
            sendInvalidMoveMessage(session);
        }
        return game;
    }

    /**
     * Handles the situation where a player want to resign.
     * @param currentGame - the relevant game
     * @param messageJson - the concede message
     * @throws IOException - Thrown if there was a problem with sending a message of the result
     */
    private void handleConcedeMessage(Game currentGame, String messageJson) throws IOException {
        //The game is considered aborted if it is in the first 2 turns, and doesn't lower your ELO.
        Gson gson = new Gson();
        boolean isAborted = currentGame.getMoveList() == null || currentGame.getMoveList().size() < 2;
        String causeMessage;
        GameState gameResult;
        if(isAborted){
            gameResult = GameState.ABORTED;
            causeMessage = "Game was aborted";
            gameService.abortGame(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), currentGame);
        }
        else{
            ConcedeGameMessage concedeGameMessage = gson.fromJson(messageJson, ConcedeGameMessage.class);
            boolean didWhiteResign = Objects.equals(
                    concedeGameMessage.getPlayerEmail(),
                    currentGame.getWhitePlayer().getEmail()
            );
            if(didWhiteResign){
                causeMessage = "Black won by resignation";
                gameResult = GameState.BLACK_WIN;
            }else{
                causeMessage = "White won by resignation";
                gameResult = GameState.WHITE_WIN;
            }
            gameService.onGameEnding(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), gameResult, currentGame);
        }
        GameEndMessage endMessage = new GameEndMessage(gameResult, causeMessage);
        sendMessageToAllPlayers(currentGame.getId(), endMessage);
    }
    private void sendInvalidMoveMessage(WebSocketSession session){
        Gson gson = new Gson();
        InvalidMoveMessage msg = new InvalidMoveMessage();
        try{
            session.sendMessage(new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)));
        } catch (IOException e){
            log.error("Could send the invalidMoveMessage, IO error. error is: " + e.getMessage());
        }
        log.info("send: " + new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)) + "to " + session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if(gameId == null) return;
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
        if(sessionsInGame == null) return;
        sessionsInGame.remove(session);

    }



    private String extractGameId(String path) {
        // Extract the gameId from the WebSocket URL path
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private void sendMessageToAllPlayers(String gameId, Message message) throws IOException {
        Gson gson = new Gson();
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
        if(sessionsInGame == null) return;
        for (WebSocketSession s : sessionsInGame) {
            s.sendMessage(new TextMessage(gson.toJson(message)));
        }
    }

    private void passTextMessageToAllPlayers(String gameId, TextMessage message) throws  IOException{
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
        if(sessionsInGame == null) return;
        for (WebSocketSession s : sessionsInGame) {
            s.sendMessage(message);
        }
    }

}
