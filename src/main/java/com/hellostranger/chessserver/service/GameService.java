package com.hellostranger.chessserver.service;

import com.google.gson.Gson;
import com.hellostranger.chessserver.controller.dto.websocket.MoveMessage;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.entities.BoardRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.*;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.storage.BoardRepresentationRepository;
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
    private final BoardRepresentationRepository boardRepresentationRepository;

    public Game createGame(){
        Game game = new Game();
        Board board = new Board();
        game.setId(UUID.randomUUID().toString());
        game.setBoard(board);
        game.setGameState(GameState.NEW);
        GameStorage.getInstance().setGame(game);

        return game;
    }
    public Game joinRandomGame(String playerEmail) throws GameFullException, GameNotFoundException, NoOpenGameException {
        Map<String, Game> games = GameStorage.getInstance().getGames();
        for(String gameId : games.keySet()){
            Game game = games.get(gameId);
            if(game.getGameState() == GameState.NEW || game.getGameState() == GameState.WAITING){
                return joinGame(gameId, playerEmail);
            }
        }

        //no open games
        Game newGame = createGame();
        return joinGame(newGame.getId(), playerEmail);
    }
    public Game joinGame(String gameId, String playerEmail) throws GameNotFoundException, GameFullException {
        Game game = getGameById(gameId);
        User user = userRepository.findByEmail(playerEmail)
                .orElseThrow();
        log.info("Join game, game is: " + game + "player email is: " + playerEmail +" player is: " + user);
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
                log.info("user of newGamePlayer is: " + user + "and users whiteGameHistory is: " + user.getWhiteGamesHistory() + "and the black: " + user.getBlackGamesHistory());
            }else{
                game.setBlackPlayer(game.getWaitingPlayer());
                game.setWhitePlayer(user);
                log.info("user of newGamePlayer is: " + user + "and users whiteGameHistory is: " + user.getWhiteGamesHistory() + "and the black: " + user.getBlackGamesHistory());
            }
            //since we have 2 players, we reset waitingPlayer to null
            game.setWaitingPlayer(null);
        } else{

            game.setWaitingPlayer(user);
        }

        if(game.getIsP1turn() == null){
            game.setIsP1turn(true);
        }

        if(game.getWhitePlayer() != null && game.getBlackPlayer() != null){
            game.setGameState(GameState.ACTIVE);

            GameRepresentation gameRepresentation = new GameRepresentation();
            gameRepresentation.setWhitePlayer(game.getWhitePlayer());
            gameRepresentation.setBlackPlayer(game.getBlackPlayer());
            gameRepresentation.setResult(GameState.ACTIVE);
            Gson gson = new Gson();
            log.info("board is: " + game.getBoard());
            BoardRepresentation boardRepresentation = BoardRepresentation
                    .builder()
                    .boardJson(gson.toJson(game.getBoard()))
                    .game(gameRepresentation)
                    .build();

            gameRepresentation.addBoardRepresentation(boardRepresentation);
            gameRepresentationRepository.save(gameRepresentation);
            boardRepresentationRepository.save(boardRepresentation);
            game.setGameRepresentation(gameRepresentation);
        } else{
            game.setGameState(GameState.WAITING);
        }
        log.info("Game players: p1 " + game.getWhitePlayer() + " p2: " + game.getBlackPlayer());
        return game;

    }

    public Game makeMove(String gameId, String playerEmail, int startCol, int startRow,int endCol, int endRow)
            throws InvalidMoveException, GameFinishedException, SquareNotFoundException, GameNotFoundException {
        Game game = getGameById(gameId);
        boolean isUserWhite = Objects.equals(playerEmail, game.getWhitePlayer().getEmail());
        log.info("isPlaying white: " + isUserWhite);
        if (game.getGameState() != GameState.ACTIVE) {
            throw new GameFinishedException("The game has already finished.");
        }
        User user = getPlayer(game, playerEmail);
        if (user == null) {
            throw new InvalidMoveException("you are not a player in this game.");
        }
        if(!checkIfIsPlayersTurn(game, user)){
            throw new InvalidMoveException("not your turn");
        }
        Square startSquare = game.getBoard().getSquareAt(startCol, startRow);
        if(startSquare.getPiece() == null ){
            throw new InvalidMoveException("No piece at that square");
        }else if( (startSquare.getPiece().getColor() == Color.WHITE) != isUserWhite){
            throw new InvalidMoveException("Piece at that square is not yours. the piece is: " + startSquare.getPiece() + "and your color is: " + isUserWhite);
        }

        Square endSquare = game.getBoard().getSquareAt(endCol, endRow);
        if (game.getBoard().isValidMove(startSquare, endSquare)) {
            log.info("GameService, move is legal");
            if(isCastle(game, startSquare, endSquare)){
                game.getBoard().makeCastlingMove(startSquare, endSquare);
            }else{
                if( startSquare.getPiece().getPieceType() == PieceType.PAWN ||
                        (endSquare.getPiece() != null && startSquare.getPiece().getColor() != endSquare.getPiece().getColor())
                ){
                    game.setHalfMoves(-1);
                }
                game.getBoard().movePiece(startSquare, endSquare);
            }

        }else{
            throw new InvalidMoveException("Move is not legal");
        }
        game.setIsP1turn(!game.getIsP1turn());
        if(game.getIsP1turn()){
            //black just played a move, increasing the full move count.
            game.setFullMoves(game.getFullMoves() + 1);
        }
        game.setHalfMoves(game.getHalfMoves() + 1);
        GameState state = checkGameState(game);
        game.setGameState(state);
        game.addMove(new Move(startCol, startRow, endCol, endRow));
        Gson gson = new Gson();
        BoardRepresentation boardRepresentation = BoardRepresentation
                .builder()
                .boardJson(gson.toJson(game.getBoard()))
                .game(game.getGameRepresentation())
                .build();
        game.getGameRepresentation().addBoardRepresentation(boardRepresentation);
        gameRepresentationRepository.save(game.getGameRepresentation());
        boardRepresentationRepository.save(boardRepresentation);

        if(game.getGameState() == GameState.WHITE_WIN || game.getGameState() == GameState.BLACK_WIN || game.getGameState() == GameState.DRAW){
            onGameEnding(game.getWhitePlayer(), game.getBlackPlayer(), game.getGameState(), game);
        }
        log.info("makeMove, game: " + game );
        return game;
    }

    public Game endGame(Game game, GameState result){
        onGameEnding(game.getWhitePlayer(), game.getBlackPlayer(), result, game);
        return game;
    }

    private void onGameEnding(User whitePlayer, User blackPlayer, GameState gameResult,Game game){
        if(game.getGameState() != gameResult){
            game.setGameState(gameResult);
        }
        GameRepresentation gameRepresentation = game.getGameRepresentation();
        gameRepresentation.setResult(gameResult);
        whitePlayer.addGameToWhiteGameHistory(gameRepresentation);
        blackPlayer.addGameToBlackGameHistory(gameRepresentation);
        updateEloScores(game);
        whitePlayer.setTotalGames(game.getWhitePlayer().getTotalGames() + 1);
        blackPlayer.setTotalGames(game.getBlackPlayer().getTotalGames() + 1);
        if(gameResult == GameState.WHITE_WIN){
            whitePlayer.setGamesWon(whitePlayer.getGamesWon() + 1);
            blackPlayer.setGamesLost(blackPlayer.getGamesLost() + 1);
        } else if(gameResult == GameState.BLACK_WIN){
            blackPlayer.setGamesWon(blackPlayer.getGamesWon() + 1);
            whitePlayer.setGamesLost(whitePlayer.getGamesLost() + 1);
        } else if (gameResult == GameState.DRAW) {
            whitePlayer.setGamesDrawn(whitePlayer.getGamesDrawn() + 1);
            blackPlayer.setGamesDrawn(blackPlayer.getGamesDrawn() + 1);
        }

        userRepository.save(whitePlayer);
        userRepository.save(blackPlayer);
        gameRepresentationRepository.save(game.getGameRepresentation());

        GameStorage.getInstance().getGames().remove(game.getId());

    }
    public boolean isCastle(Game game, Square startSquare, Square endSquare){
        return game.getBoard().isCastlingMove(startSquare, endSquare);
    }

    private GameState checkGameState(Game game){
        if(game.getHalfMoves() >= 100){
            return GameState.DRAW;
        }
        if(game.getIsP1turn()){
            //black just played his move. check if white king is in check:
            try{
                if(game.getBoard().isKingInCheck(true)){
                    if(!game.getBoard().canPlayerPlay(Color.WHITE)){
                        //black has been checkmated
                        log.info("black won by checkmate.");
                        return GameState.BLACK_WIN;
                    }else{
                        return GameState.ACTIVE;
                    }
                }else{
                    if(!game.getBoard().canPlayerPlay(Color.WHITE)){
                        return GameState.DRAW;
                    }else{
                        return GameState.ACTIVE;
                    }
                }
            } catch (SquareNotFoundException e){
                log.info("isGameOver square not found: " + e.getMsg());
                return GameState.ACTIVE;
            }
        }else{
            //white just played
            try{
                if(game.getBoard().isKingInCheck(false)){
                    if(!game.getBoard().canPlayerPlay(Color.BLACK)){
                        log.info("white won by checkmate.");
                        //black has been checkmated
                        return GameState.WHITE_WIN;
                    }else{
                        return GameState.ACTIVE;
                    }
                }else{
                    if(!game.getBoard().canPlayerPlay(Color.BLACK)){
                        return GameState.DRAW;
                    }else{
                        return GameState.ACTIVE;
                    }
                }
            } catch (SquareNotFoundException e){
                log.info("isGameOver square not found: " + e.getMsg());
                return GameState.ACTIVE;
            }
        }
    }
    public void updateEloScores(Game game){
        //receives a finished game, updated the elo of the players who played in it.
        User white = game.getWhitePlayer(), black = game.getBlackPlayer();
        int whiteElo = white.getElo(), blackELo = black.getElo();
        if(game.getGameState() == GameState.DRAW){
            white.setElo(EloCalculator.calculateEloAfterDraw(whiteElo, blackELo));
            black.setElo(EloCalculator.calculateEloAfterDraw(blackELo, whiteElo));
        } else if(game.getGameState() == GameState.WHITE_WIN){
            white.setElo(EloCalculator.calculateEloAfterWin(whiteElo, blackELo));
            black.setElo(EloCalculator.calculateEloAfterLoss(blackELo, whiteElo));
        } else if(game.getGameState() == GameState.BLACK_WIN){
            white.setElo(EloCalculator.calculateEloAfterLoss(whiteElo, blackELo));
            black.setElo(EloCalculator.calculateEloAfterWin(blackELo, whiteElo));
        }

    }


    public Game getGameById(String gameId) throws GameNotFoundException {
        Game game = GameStorage.getInstance().getGames().get(gameId);
        if (game == null) {
            throw new GameNotFoundException("Game with ID " + gameId + " not found.");
        }
        return game;
    }

    private boolean checkIfIsPlayersTurn(Game game, User player){
        if(Objects.equals(game.getWhitePlayer().getEmail(), player.getEmail())){
            return game.getIsP1turn();
        } else{
            return !game.getIsP1turn();
        }
    }

    private User getPlayer(Game game, String playerEmail) {
        if(Objects.equals(game.getWhitePlayer().getEmail(), playerEmail)){
            return game.getWhitePlayer();
        } else if(Objects.equals(game.getBlackPlayer().getEmail(), playerEmail)){
            return game.getBlackPlayer();
        }
        return null;
    }

    public MoveMessage[] convertCastleToMoves(MoveMessage moveMessage){
        //assumes the moveMessage is for castling
        MoveMessage kingMove, rookMove;
        if(moveMessage.getEndCol() > moveMessage.getStartCol()){
            //O-O
            kingMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getStartCol(), moveMessage.getStartRow(), moveMessage.getStartCol() + 2, moveMessage.getEndRow());
            rookMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getEndCol(), moveMessage.getEndRow(), moveMessage.getEndCol() - 2, moveMessage.getEndRow());
            rookMove.setSecondCastleMove(true);
        }else{
            //O-O-O
            kingMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getStartCol(), moveMessage.getStartRow(), moveMessage.getStartCol() - 2, moveMessage.getEndRow());
            rookMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getEndCol(), moveMessage.getEndRow(), moveMessage.getEndCol() + 3, moveMessage.getEndRow());
            rookMove.setSecondCastleMove(true);
        }
        MoveMessage[] moves = new MoveMessage[2];
        moves[0] = kingMove;
        moves[1] = rookMove;
        return moves;

    }




}
