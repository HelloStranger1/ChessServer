package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.websocket.MoveMessage;
import com.hellostranger.chessserver.core.Arbiter;
import com.hellostranger.chessserver.core.EloCalculator;
import com.hellostranger.chessserver.core.GameResult;
import com.hellostranger.chessserver.core.board.Board;
import com.hellostranger.chessserver.core.board.Move;
import com.hellostranger.chessserver.core.board.Piece;
import com.hellostranger.chessserver.core.helpers.FenUtility;
import com.hellostranger.chessserver.core.helpers.MoveUtility;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.game.*;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.storage.MoveRepresentationRepository;
import com.hellostranger.chessserver.storage.GameRepresentationRepository;
import com.hellostranger.chessserver.storage.GameStorage;
import com.hellostranger.chessserver.storage.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final GameRepresentationRepository gameRepresentationRepository;

    @Autowired
    private final MoveRepresentationRepository moveRepresentationRepository;

    public String createPrivateGame(){
        Game game = createGame(GameResult.WaitingPrivate);
        return GameStorage.getInstance().generateShortCode(game);

    }
    public Game createGame(GameResult state){
        Game game = new Game();
        Board board = Board.createBoard();
        board.loadStartPosition();
        game.setId(UUID.randomUUID().toString());
        game.setBoard(board);
        game.setGameState(state);
        GameStorage.getInstance().setGame(game);

        return game;
    }
    public Game joinPrivateGame(String playerEmail, String shortenedCode) throws GameFullException, GameNotFoundException {
        String gameId = GameStorage.getInstance().matchCodeToId(shortenedCode);
        if(Objects.equals(gameId, "")){
            log.error("No game found!!!!!");
            throw new GameNotFoundException("No game found!!!!!");
        }
        return joinGame(gameId, playerEmail);
    }
    public Game joinRandomGame(String playerEmail) throws GameFullException {
        Map<String, Game> games = GameStorage.getInstance().getGames();
        for(String gameId : games.keySet()){
            Game game = games.get(gameId);
            if(game.getGameState() == GameResult.Waiting){
                return joinGame(gameId, playerEmail);
            }
        }
        //no open games
        Game newGame = createGame(GameResult.Waiting);
        return joinGame(newGame.getId(), playerEmail);
    }
    public Game joinGame(String gameId, String playerEmail) throws GameFullException {
        Game game = getGameById(gameId);
        User user = userRepository.findByEmail(playerEmail)
                .orElseThrow();
        log.info("Join game, game is: {}player email is: {} player is: {}", game, playerEmail, user);
        if (game.getWhitePlayer() != null && game.getBlackPlayer() != null) {
            throw new GameFullException("The game is already full.");
        }

        if(game.getWaitingPlayer() != null){
            //we randomize who plays black and who plays white
            Random rnd = new Random();
            boolean isWaitingPlayerWhite = rnd.nextBoolean();

            if(isWaitingPlayerWhite){
                game.setWhitePlayer(game.getWaitingPlayer());
                game.setBlackPlayer(user);
            }else{
                game.setBlackPlayer(game.getWaitingPlayer());
                game.setWhitePlayer(user);
            }
            //since we have 2 players, we reset waitingPlayer to null
            game.setWaitingPlayer(null);
        } else{

            game.setWaitingPlayer(user);
        }

        if(game.getWhitePlayer() != null && game.getBlackPlayer() != null) {
            game.setGameState(GameResult.InProgress);

            GameRepresentation gameRepresentation = new GameRepresentation();
            gameRepresentation.setWhitePlayer(game.getWhitePlayer());
            gameRepresentation.setBlackPlayer(game.getBlackPlayer());
            gameRepresentation.setStartBoardFen(FenUtility.currentFen(game.getBoard(), true));
            log.info("board is {}", game.getBoard());

            gameRepresentationRepository.save(gameRepresentation);
            game.setGameRepresentation(gameRepresentation);

        }
        log.info("Game players: p1 {} p2: {}", game.getWhitePlayer(), game.getBlackPlayer());
        return game;

    }


    public Game makeMove(String gameId,MoveMessage moveMessage)
            throws InvalidMoveException, GameFinishedException {

        Game game = getGameById(gameId);
        Board board = game.getBoard();
        String playerEmail = moveMessage.getPlayerEmail();
        Move move = new Move(moveMessage.getMove());

        boolean isUserWhite = Objects.equals(playerEmail, game.getWhitePlayer().getEmail());

        if (game.getGameState() != GameResult.InProgress) {
            throw new GameFinishedException("The game has already finished.");
        }


        User user = getPlayer(game, playerEmail);
        if (user == null) {
            throw new InvalidMoveException("you are not a player in this game.");
        }
        if(!checkIfIsPlayersTurn(game, user)){
            throw new InvalidMoveException("not your turn");
        }

        int startSquareIndex = move.getStartSquare();
        int movingPiece = board.getSquare()[startSquareIndex];

        if (movingPiece == Piece.NONE){
            throw new InvalidMoveException("No piece at that square");
        } else if (Piece.isWhite(movingPiece) != isUserWhite){
            throw new InvalidMoveException("Piece at that square is not yours. The piece type is: "
                    +Piece.getSymbol(movingPiece) + "And is the piece white? " + Piece.isWhite(movingPiece) + " While are you white? " + isUserWhite);
        }

        if (isUserWhite) {
            game.setIsDrawOfferedByBlack(false);
        } else {
            game.setIsDrawOfferedByWhite(false);
        }

        if (!validateMove(game, move)) {
            throw new InvalidMoveException("Move is not legal. move in UCE is: " + MoveUtility.getMoveNameUCI(move));
        }
        log.info("GameService, move is legal");
        board.makeMove(move, false);

        GameResult state = Arbiter.getGameState(board);
        game.setGameState(state);
        MoveRepresentation moveRepresentation = new MoveRepresentation();
        moveRepresentation.setMove(MoveUtility.getMoveNameUCI(move));
        game.getGameRepresentation().addMoveRepresentation(moveRepresentation);
        moveRepresentationRepository.save(moveRepresentation);

        if (Arbiter.isWinResult(state) || Arbiter.isDrawResult(state)) {
            onGameEnding(game.getWhitePlayer(), game.getBlackPlayer(), state, game);
        }
        game.addFenToList(FenUtility.currentFen(board, true));
        log.info("makeMove, game: {}", game);
        return game;
    }

    public void onGameEnding(User whitePlayer, User blackPlayer, GameResult gameResult,Game game){
        game.setGameState(gameResult);
        GameRepresentation gameRepresentation = game.getGameRepresentation();
        gameRepresentation.setResult(gameResult);
        if (gameResult != GameResult.Aborted) {
            whitePlayer.addGameToWhiteGameHistory(gameRepresentation);
            blackPlayer.addGameToBlackGameHistory(gameRepresentation);
            whitePlayer.setTotalGames(game.getWhitePlayer().getTotalGames() + 1);
            blackPlayer.setTotalGames(game.getBlackPlayer().getTotalGames() + 1);
        }
        if(Arbiter.isWhiteWinResult(gameResult)){
            whitePlayer.setElo(EloCalculator.calculateEloAfterWin(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterLoss(blackPlayer.getElo(), whitePlayer.getElo()));
            whitePlayer.setGamesWon(whitePlayer.getGamesWon() + 1);
            blackPlayer.setGamesLost(blackPlayer.getGamesLost() + 1);
        } else if(Arbiter.isBlackWinResult(gameResult)){
            whitePlayer.setElo(EloCalculator.calculateEloAfterLoss(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterWin(blackPlayer.getElo(), whitePlayer.getElo()));
            blackPlayer.setGamesWon(blackPlayer.getGamesWon() + 1);
            whitePlayer.setGamesLost(whitePlayer.getGamesLost() + 1);
        } else if (Arbiter.isDrawResult(gameResult)) {
            whitePlayer.setElo(EloCalculator.calculateEloAfterDraw(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterDraw(blackPlayer.getElo(), whitePlayer.getElo()));
            whitePlayer.setGamesDrawn(whitePlayer.getGamesDrawn() + 1);
            blackPlayer.setGamesDrawn(blackPlayer.getGamesDrawn() + 1);
        }

        userRepository.save(whitePlayer);
        userRepository.save(blackPlayer);

    }

    private boolean validateMove(Game game, Move move) {
        Move[] legalMoves = game.moveGenerator.generateMoves(game.getBoard(), false);
        for (Move legalMove : legalMoves) {
            if (Move.sameMove(legalMove, move)) {
                return true;
            }
        }
        return false;
    }

    public void abortGame(User whitePlayer, User blackPlayer, Game game){
        GameRepresentation gameRepresentation = game.getGameRepresentation();
        if(gameRepresentation != null && gameRepresentation.getMoveRepresentations() != null){
            gameRepresentation.getMoveRepresentations().clear();
            gameRepresentationRepository.delete(gameRepresentation);
        }
        if(whitePlayer != null){
            userRepository.save(whitePlayer);
        }
        if(blackPlayer != null){
            userRepository.save(blackPlayer);
        }
        GameStorage.getInstance().removeGame(game);
    }

    public Game getGameById(String gameId) {
        Game game = GameStorage.getInstance().getGames().get(gameId);
        if (game == null) {
            log.error("No game exists with id: {}", gameId);
        }
        return game;
    }

    private boolean checkIfIsPlayersTurn(Game game, User player){
        return (player.getEmail().equals(game.getWhitePlayer().getEmail())) == game.getBoard().isWhiteToMove();
    }

    private User getPlayer(Game game, String playerEmail) {
        if(Objects.equals(game.getWhitePlayer().getEmail(), playerEmail)){
            return game.getWhitePlayer();
        } else if(Objects.equals(game.getBlackPlayer().getEmail(), playerEmail)){
            return game.getBlackPlayer();
        }
        return null;
    }




}
