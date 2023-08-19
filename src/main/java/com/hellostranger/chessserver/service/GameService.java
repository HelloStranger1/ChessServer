package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.websocket.MoveMessage;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.FENRepresentation;
import com.hellostranger.chessserver.models.GameRepresentation;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.*;
import com.hellostranger.chessserver.models.user.User;
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
        if (game.getPlayer1() != null && game.getPlayer2() != null) {
            throw new GameFullException("The game is already full.");
        }

        if(game.getWaitingGamePlayer() != null){
            //we randomize who plays black and who plays white
            Random rnd = new Random();
            boolean isWaitingPlayerWhite = rnd.nextBoolean();
            var newGamePlayer = GamePlayer
                    .builder()
                    .name(user.getName())
                    .elo(user.getElo())
                    .user(user)
                    .gameRepresentation(game.getWaitingGamePlayer().getGameRepresentation())
                    .build();
            if(isWaitingPlayerWhite){
                game.setPlayer1(game.getWaitingGamePlayer());
                game.getPlayer1().setColor(Color.WHITE);
                newGamePlayer.setColor(Color.BLACK);

                newGamePlayer.getGameRepresentation().addPlayer(newGamePlayer);
                log.info("newGamePlayer is: " + newGamePlayer + " and the gameRepresentation is: " + newGamePlayer.getGameRepresentation());

                user.addGamePlayer(newGamePlayer);
                log.info("user of newGamePlayer is: " + user + "and users gameHistory is: " + user.getGamesHistory());

                game.setPlayer2(newGamePlayer);
            }else{
                game.setPlayer2(game.getWaitingGamePlayer());
                game.getPlayer2().setColor(Color.BLACK);
                newGamePlayer.setColor(Color.WHITE);
                newGamePlayer.getGameRepresentation().addPlayer(newGamePlayer);
                log.info("newGamePlayer is: " + newGamePlayer + " and the gameRepresentation is: " + newGamePlayer.getGameRepresentation());

                user.addGamePlayer(newGamePlayer);
                log.info("user of newGamePlayer is: " + user + "and users gameHistory is: " + user.getGamesHistory());

                game.setPlayer1(newGamePlayer);
            }
            //since we have 2 players, we reset waitingPlayer to null
            game.setWaitingGamePlayer(null);
        } else{
            //we set the new player as the waiting player.
            var newGamePlayer = GamePlayer
                    .builder()
                    .name(user.getName())
                    .elo(user.getElo())
                    .user(user)
                    .gameRepresentation(new GameRepresentation())
                    .build();
            user.addGamePlayer(newGamePlayer);
            newGamePlayer.getGameRepresentation().addPlayer(newGamePlayer);
            game.setWaitingGamePlayer(newGamePlayer);
        }

        if(game.getIsP1turn() == null){
            game.setIsP1turn(true);
        }

        if(game.getPlayer1() != null && game.getPlayer2() != null){
            game.setGameState(GameState.ACTIVE);
            game.getPlayer1().setGameState(GameState.ACTIVE);
            game.getPlayer2().setGameState(GameState.ACTIVE);
        } else{
            game.setGameState(GameState.WAITING);
        }

        log.info("Game players: p1 " + game.getPlayer1() + " p2: " + game.getPlayer2());
        return game;

    }

    public Game makeMove(String gameId, String playerEmail, int startCol, int startRow,int endCol, int endRow)
            throws InvalidMoveException, GameFinishedException, SquareNotFoundException, GameNotFoundException {
        Game game = getGameById(gameId);

        if (game.getGameState() != GameState.ACTIVE) {
            throw new GameFinishedException("The game has already finished.");
        }

        GamePlayer gamePlayer = getPlayer(game, playerEmail);
        if (gamePlayer == null) {
            throw new InvalidMoveException("you are not a player in this game.");
        }
        if(!checkIfIsPlayersTurn(game, gamePlayer)){
            throw new InvalidMoveException("not your turn");
        }
        Square startSquare = game.getBoard().getSquareAt(startCol, startRow);
        if(startSquare.getPiece() == null || startSquare.getPiece().getColor() != gamePlayer.getColor()){
            throw new InvalidMoveException("Piece doesn't exist at that square or its not yours");
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
        GamePlayer p1 = game.getPlayer1();
        GamePlayer p2 = game.getPlayer2();
        game.setGameState(state);
        p1.setGameState(state);
        p2.setGameState(state);
        game.addMove(new Move(startCol, startRow, endCol, endRow));
        String FEN = convertGameToFEN(game);
        p1.getGameRepresentation().addFENRepresentation(FEN);
        User p1User = userRepository.findByEmail(p1.getEmail())
                .orElseThrow();
        log.info("p1User is: " + p1User);
        log.info("p1 is: " + p1);
        log.info("gameRep is: " + p1.getGameRepresentation());
        User p2User = userRepository.findByEmail(p2.getEmail())
                .orElseThrow();
        p1User.addGamePlayer(p1);
        p2User.addGamePlayer(p2);
        userRepository.save(p1User);
        userRepository.save(p2User);
        log.info("makeMove, game: " + game );
        return game;
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
    public Game getGameById(String gameId) throws GameNotFoundException {
        Game game = GameStorage.getInstance().getGames().get(gameId);
        if (game == null) {
            throw new GameNotFoundException("Game with ID " + gameId + " not found.");
        }
        return game;
    }

    private boolean checkIfIsPlayersTurn(Game game, GamePlayer gamePlayer){
        if(game.getPlayer1() == gamePlayer){
            return game.getIsP1turn();
        } else{
            return !game.getIsP1turn();
        }
    }

    private GamePlayer getPlayer(Game game, String playerEmail) {
        if(Objects.equals(game.getPlayer1().getEmail(), playerEmail)){
            return game.getPlayer1();
        } else if(Objects.equals(game.getPlayer2().getEmail(), playerEmail)){
            return game.getPlayer2();
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
        }else{
            //O-O-O
            kingMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getStartCol(), moveMessage.getStartRow(), moveMessage.getStartCol() - 2, moveMessage.getEndRow());
            rookMove = new MoveMessage(moveMessage.getPlayerEmail(), moveMessage.getEndCol(), moveMessage.getEndRow(), moveMessage.getEndCol() + 3, moveMessage.getEndRow());
        }
        MoveMessage[] moves = new MoveMessage[2];
        moves[0] = kingMove;
        moves[1] = rookMove;
        return moves;

    }

    public String convertGameToFEN(Game game){
        StringBuilder FEN = new StringBuilder();
        int emptySquareCount = 0;
        for(int row = 7; row >= 0; row--){
            for(int col = 7; col >= 0; col --){
                try{
                    Square currentSquare = game.getBoard().getSquareAt(col, row);
                    if(currentSquare.getPiece() != null){
                        if(emptySquareCount != 0){
                            FEN.append(emptySquareCount);
                        }
                        char pieceChar = getPieceChar(currentSquare.getPiece());
                        FEN.append(pieceChar);
                    }else{
                        emptySquareCount++;
                    }
                } catch (SquareNotFoundException e){
                    log.info("what happened. i dont even know" + e.getMsg());
                }
            }
            FEN.append("/");
        }
        if(game.getIsP1turn()){
            FEN.append(" w");
        }else{
            FEN.append(" b");
        }

        StringBuilder castleRepresentation = new StringBuilder();

        try{
            //white king side
            if(game.getBoard().isCastlingMove(
                    game.getBoard().getSquareAt(3, 0),
                    game.getBoard().getSquareAt(7,0))
            ) {
                castleRepresentation.append("K");
            }
            //white queen side
            if(game.getBoard().isCastlingMove(
                    game.getBoard().getSquareAt(3, 0),
                    game.getBoard().getSquareAt(1,0))
            ) {
                castleRepresentation.append("Q");
            }

            //black king side
            if(game.getBoard().isCastlingMove(
                    game.getBoard().getSquareAt(3, 7),
                    game.getBoard().getSquareAt(7,7))
            ) {
                castleRepresentation.append("k");
            }
            //black queen side
            if(game.getBoard().isCastlingMove(
                    game.getBoard().getSquareAt(3, 7),
                    game.getBoard().getSquareAt(1,7))
            ) {
                castleRepresentation.append("q");
            }
        } catch (SquareNotFoundException e){
            log.info("i really have no idea what is wrong" + e.getMsg());
        }
        if(castleRepresentation.isEmpty()){
            FEN.append(" -");
        }else{
            FEN.append(" ");
            FEN.append(castleRepresentation);

        }

        //add "phantom pawn"
        if(game.getBoard().getPhantomPawnSquare() != null){
            int phantomCol = game.getBoard().getPhantomPawnSquare().getColIndex();
            int phantomRow = game.getBoard().getPhantomPawnSquare().getRowIndex();
            switch (phantomCol){
                case 0 -> FEN.append(" a");
                case 1 -> FEN.append(" b");
                case 2 -> FEN.append(" c");
                case 3 -> FEN.append(" d");
                case 4 -> FEN.append(" e");
                case 5 -> FEN.append(" f");
                case 6 -> FEN.append(" g");
                case 7 -> FEN.append(" h");
            }
            FEN.append(phantomRow+1);
        }

        String movesCount = " " +game.getHalfMoves() + " " + game.getFullMoves();
        FEN.append(movesCount);
        return FEN.toString();
    }

    /*
        Converts a piece to the char representation of it. a white king -> K, a black knight -> n and etc
     */
    private char getPieceChar(Piece piece){
        boolean isPieceWhite = piece.getColor() == Color.WHITE;
        switch (piece.getPieceType()){
            case KING -> {
                if(isPieceWhite) return 'K'; else return 'k';
            }
            case QUEEN -> {
                        if(isPieceWhite) return 'Q'; else return 'q';
            }
            case ROOK -> {
                if(isPieceWhite) return 'R'; else return 'r';
            }
            case BISHOP -> {
                if(isPieceWhite) return 'B'; else return 'b';
            }
            case KNIGHT -> {
                if(isPieceWhite) return 'N'; else return 'n';
            }
            case PAWN -> {
                if(isPieceWhite) return 'P'; else return 'p';
            }
        }
        return '\0';
    }


}
