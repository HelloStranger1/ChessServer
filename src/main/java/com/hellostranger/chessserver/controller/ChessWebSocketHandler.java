package com.hellostranger.chessserver.controller;


import com.google.gson.Gson;
import com.hellostranger.chessserver.controller.dto.websocket.*;
import com.hellostranger.chessserver.core.Arbiter;
import com.hellostranger.chessserver.core.GameResult;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.game.Game;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.service.GameService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    private final Map<String, Set<WebSocketSession>> gameSessions = new ConcurrentHashMap<>(); // Game sessions
    private final Map<WebSocketSession, String> sessionEmailMap = new ConcurrentHashMap<>();
    private final GameService gameService;


    /**
     * Called when someone connects to the Websocket.
     * If there are 2 players in the game, starts it.
     * @param session The WebSocketSession of the player who is joining
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if (gameId == null) {
            log.error("AfterConnectionEstablished. Couldn't extract gameId from path.");
            return;
        }
        // Add the session to the corresponding game session set
        gameSessions.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);
        Game currentGame = gameService.getGameById(gameId);
        if (currentGame == null) {
            log.error("After connection established. Couldn't find game with id: {}", gameId);
            return;
        }

        if (currentGame.getGameState() == GameResult.InProgress) {
            User whitePlayer = currentGame.getWhitePlayer();
            User blackPlayer = currentGame.getBlackPlayer();
            GameStartMessage startMessage = new GameStartMessage(whitePlayer, blackPlayer);
            try {
                sendMessageToAllPlayers(gameId, startMessage);
            } catch (IOException e) {
                log.error("After connection established. Couldn't send message to all players.", e);
            }
        }
    }

    /**
     * Called whenever a player sends a message.
     * @param session The relevant WebSocketSession of the game
     * @param message One of the possible Messages (Detailed in dto.websocket), in JSON.
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NotNull TextMessage message) {

        //Extracting gameId and the message
        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if (gameId == null) {
            log.error("HandleTextMessage. Couldn't extract gameId from path.");
            return;
        }

        String messageJson = message.getPayload();

        Game currentGame = gameService.getGameById(gameId);
        if (currentGame == null) {
            log.error("handleTextMessage. Couldn't find a game with id: {}", gameId);
            return;
        }

        //Converting the Json into a Message object, the parent object of all other messages.
        Gson gson = new Gson();
        Message webSocketMessage = gson.fromJson(messageJson, Message.class);
        MessageType messageType = webSocketMessage.getMessageType();


        //If this is a start or end message we just want to send it, without changing, to everyone else
        if(messageType == MessageType.END || messageType == MessageType.START){
            try {
                passTextMessageToAllPlayers(session, gameId, message);
            } catch (IOException e) {
                log.error("handle message. had a problem sending message to everyone.");
            }
            log.info("I didn't think we would ever get this message since we are the ones sending it");
            return;
        }

        if(webSocketMessage.getMessageType() == MessageType.CONCEDE){
            ConcedeGameMessage concedeGameMessage = gson.fromJson(messageJson, ConcedeGameMessage.class);
            try {
                handleConcedeMessage(currentGame, concedeGameMessage);
            } catch (IOException e) {
                log.error("HandleMessage. Had a problem sending concede message to everyone.");
            }

            return;
        }

        if(webSocketMessage.getMessageType() == MessageType.INVALID_MOVE){
            log.error("We are the ones sending this, we shouldn't receive it");
            return;
        }

        if (webSocketMessage.getMessageType() == MessageType.DRAW_OFFER) {
            try {
                passTextMessageToAllPlayers(session, gameId,  message);
            } catch (IOException e) {
                log.error("handle message. had a problem sending draw message to everyone.");

            }
            Game game = gameService.getGameById(gameId);
            DrawOfferMessage msg = gson.fromJson(messageJson, DrawOfferMessage.class);
            if (msg.isWhite()) {
                game.setIsDrawOfferedByWhite(true);
            } else {
                game.setIsDrawOfferedByBlack(true);
            }
            if (game.getIsDrawOfferedByBlack() && game.getIsDrawOfferedByWhite()) {
                gameService.onGameEnding(game.getWhitePlayer(), game.getBlackPlayer(), GameResult.DrawByAgreement, game);
                GameEndMessage endMessage = getGameEndMessage(game);
                if(endMessage != null) {
                    try {
                        sendMessageToAllPlayers(gameId, endMessage);
                    } catch (IOException e) {
                        log.error("HandleMessage. Had a problem sending message to everyone.", e);
                    }
                }
            }

            return;
        }

        if (webSocketMessage.getMessageType() != MessageType.MOVE) {
            log.error("This should have been a move. The real type is: {}", webSocketMessage.getMessageType());
            return;
        }

        if (currentGame.getGameState() != GameResult.InProgress) {
            log.error("Game isn't active. you can't play a move yet");
            return;
        }

        MoveMessage moveMessage = gson.fromJson(messageJson, MoveMessage.class);

        //The game has started, we need to save the user email
        sessionEmailMap.computeIfAbsent(session, k -> moveMessage.getPlayerEmail());

        Game updatedGame = makeMove(gameId, moveMessage, session);
        if(updatedGame == null){
            return;
        }
        try {
            passTextMessageToAllPlayers(session, gameId, message);
        } catch (IOException e) {
            log.error("HandleMessage. Had a problem sending message to everyone.");
        }
        GameEndMessage endMessage = getGameEndMessage(updatedGame);
        if(endMessage != null){
            try {
                sendMessageToAllPlayers(gameId, endMessage);
            } catch (IOException e) {
                log.error("HandleMessage. Had a problem sending message to everyone.", e);
            }
        }
    }


    /**
     * Matches the correct message for a finished game
     * @param updatedGame The game
     * @return a GameEndMessage to be sent to all players. Can be null, if there was an error.
     */
    private static GameEndMessage getGameEndMessage(Game updatedGame) {
        int whiteElo = updatedGame.getWhitePlayer().getElo();
        int blackElo = updatedGame.getBlackPlayer().getElo();
        GameResult gameResult = updatedGame.getGameState();
        String gameDescription = Arbiter.getResultDescription(gameResult);
        if (gameDescription.isBlank()) {
            return null;
        }
        return new GameEndMessage(gameResult, gameDescription, whiteElo, blackElo, updatedGame.getGameRepresentation().getId());
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
        } catch (GameFinishedException e){
            log.error("Game finished, error: {}", e.getMsg());
        } catch (InvalidMoveException e) {
            log.error("Invalid Move, error: {}", e.getMsg());
            sendInvalidMoveMessage(session);
        }
        return game;
    }

    /**
     * Handles the situation where a player want to resign.
     * @param currentGame - the relevant game
     * @param message - the concede message
     * @throws IOException - Thrown if there was a problem with sending a message of the result
     */
    private void handleConcedeMessage(Game currentGame, ConcedeGameMessage message) throws IOException {
        //The game is considered aborted if it is in the first 2 turns, and doesn't lower your ELO.
        boolean isAborted = currentGame.getBoard().getAllGameMoves().size() < 2;
        String causeMessage;
        GameResult gameResult;
        if(isAborted){
            gameResult = GameResult.Aborted;
            causeMessage = "Game was aborted";
            gameService.abortGame(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), currentGame);
        }
        else{
            if (currentGame.getGameState() != GameResult.InProgress &&
                    currentGame.getGameState() != GameResult.Waiting &&
                    currentGame.getGameState() != GameResult.WaitingPrivate) {
                log.info("Someone resigned, but game is already over");
                return;
            }
            log.info("Someone resigned. his email is: {} and the white email: {}", message.getPlayerEmail(), currentGame.getWhitePlayer().getEmail());
            boolean didWhiteResign = Objects.equals(
                    message.getPlayerEmail(),
                    currentGame.getWhitePlayer().getEmail()
            );
            if(didWhiteResign){
                causeMessage = "Black won by resignation";
                gameResult = GameResult.WhiteResigned;
            }else{
                causeMessage = "White won by resignation";
                gameResult = GameResult.BlackResigned;
            }
            gameService.onGameEnding(currentGame.getWhitePlayer(), currentGame.getBlackPlayer(), gameResult, currentGame);
        }

        GameEndMessage endMessage = new GameEndMessage(gameResult, causeMessage, currentGame.getWhitePlayer().getElo(), currentGame.getBlackPlayer().getElo(), currentGame.getGameRepresentation().getId());
        sendMessageToAllPlayers(currentGame.getId(), endMessage);
    }
    private void sendInvalidMoveMessage(WebSocketSession session){
        Gson gson = new Gson();
        InvalidMoveMessage msg = new InvalidMoveMessage();
        try{
            session.sendMessage(new TextMessage(gson.toJson(msg, InvalidMoveMessage.class)));
        } catch (IOException e){
            log.error("Could send the invalidMoveMessage, IO error. error is: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.error("Connection closed");

        String gameId = extractGameId(Objects.requireNonNull(session.getUri()).getPath());
        if(gameId == null) return;
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
        if(sessionsInGame == null) return;
        sessionsInGame.remove(session);
        try{
            Game game = gameService.getGameById(gameId);
            GameResult gameState = game.getGameState();
            boolean gameDidntStart = gameState == GameResult.NotStarted || gameState == GameResult.Waiting || gameState == GameResult.WaitingPrivate;
            boolean isAborted = game.getBoard().getAllGameMoves().size() < 2;
            if(gameDidntStart || isAborted){
                gameService.abortGame(game.getWhitePlayer(), game.getBlackPlayer(), game);

                sendMessageToAllPlayers(game.getId(), new GameEndMessage(GameResult.Aborted, "Game was aborted", -1, -1, -1));
                sessionEmailMap.remove(session);
                return;
            }
            String playerEmail = sessionEmailMap.get(session);
            if(playerEmail == null){
                log.error("Couldn't find player to resign with (connection closed)");
                return;
            }
            ConcedeGameMessage message =  new ConcedeGameMessage(playerEmail);
            handleConcedeMessage(game, message);
        } catch (IOException e){
            sessionEmailMap.remove(session);
            return;
        } catch (NullPointerException e) {
            return;
        }
        sessionEmailMap.remove(session);

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

    private void passTextMessageToAllPlayers(@NonNull WebSocketSession session, String gameId, TextMessage message) throws  IOException{
        Set<WebSocketSession> sessionsInGame = gameSessions.get(gameId);
        if(sessionsInGame == null) return;
        for (WebSocketSession s : sessionsInGame) {
            if (!s.equals(session)) {
                s.sendMessage(message);
            }
        }
    }



}
