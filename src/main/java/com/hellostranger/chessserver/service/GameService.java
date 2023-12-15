package com.hellostranger.chessserver.service;

import com.google.gson.Gson;
import com.hellostranger.chessserver.controller.dto.websocket.MoveMessage;
import com.hellostranger.chessserver.exceptions.*;
import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.enums.MoveType;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.*;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.game.pieces.King;
import com.hellostranger.chessserver.models.game.pieces.Piece;
import com.hellostranger.chessserver.models.game.pieces.PieceFactory;
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

    public void removeGame(Game game){
        GameStorage.getInstance().removeGame(game);
    }
    public Game createGame(){
        Game game = new Game();
        Board board = new Board();
        board.setBoardWithStandardPieces();
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

                /*game.setBlackPlayer(newGamePlayer);*/
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
            Gson gson = new Gson();
            gameRepresentation.setStartBoardJson(gson.toJson(game.getBoard()));
            log.info("board is " + game.getBoard());

            gameRepresentationRepository.save(gameRepresentation);
            game.setGameRepresentation(gameRepresentation);

        } else{
            game.setGameState(GameState.WAITING);
        }

        log.info("Game players: p1 " + game.getWhitePlayer() + " p2: " + game.getBlackPlayer());
        return game;

    }

    public Game makeMove(String gameId,MoveMessage moveMessage)
            throws InvalidMoveException, GameFinishedException, SquareNotFoundException, GameNotFoundException {

        Game game = getGameById(gameId);
        Board board = game.getBoard();
        String playerEmail = moveMessage.getPlayerEmail();
        int startCol = moveMessage.getStartCol(), startRow = moveMessage.getStartRow();
        int endCol = moveMessage.getEndCol(), endRow = moveMessage.getEndRow();

        boolean isUserWhite = Objects.equals(playerEmail, game.getWhitePlayer().getEmail());

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

        Square startSquare = board.getSquareAt(startCol, startRow);
        Square endSquare = board.getSquareAt(endCol, endRow);

        if(startSquare.getPiece() == null ){
            throw new InvalidMoveException("No piece at that square");
        }else if( (startSquare.getPiece().getColor() == Color.WHITE) != isUserWhite){
            throw new InvalidMoveException("Piece at that square is not yours. the piece is: " + startSquare.getPiece() + "and your color is: " + isUserWhite);
        }

        if (board.isValidMove(startSquare, endSquare)) {
            log.info("GameService, move is legal");
            if(moveMessage.getMoveType() == MoveType.CASTLE){
                board.makeCastlingMove(startSquare, endSquare);
            }else if(moveMessage.getMoveType() != MoveType.REGULAR){
                game.setHalfMoves(-1);
                board.movePiece(startSquare, endSquare);
                PieceType promotionType = switch (moveMessage.getMoveType()){
                    case REGULAR, CASTLE -> null;
                    case PROMOTION_QUEEN -> PieceType.QUEEN;
                    case PROMOTION_ROOK -> PieceType.ROOK;
                    case PROMOTION_KNIGHT -> PieceType.KNIGHT;
                    case PROMOTION_BISHOP -> PieceType.BISHOP;
                };
                board.promotePawnAt(endSquare, promotionType);
                log.info("After promotion board is: " + board.toStringPretty());
            }
            else{
                if(startSquare.getPiece().getPieceType() == PieceType.PAWN ||
                        (endSquare.getPiece() != null && startSquare.getPiece().getColor() != endSquare.getPiece().getColor())
                ){
                    game.setHalfMoves(-1);
                }
                board.movePiece(startSquare, endSquare);
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

        game.addMove(new Move(moveMessage));
        MoveRepresentation moveRepresentation = MoveRepresentation
                .builder()
                .startCol(startCol)
                .endCol(endCol)
                .startRow(startRow)
                .endRow(endRow)
                .moveType(moveMessage.getMoveType())
                .build();
        game.getGameRepresentation().addMoveRepresentation(moveRepresentation);
        moveRepresentationRepository.save(moveRepresentation);

        if(game.getGameState() == GameState.WHITE_WIN || game.getGameState() == GameState.BLACK_WIN || game.getGameState() == GameState.DRAW){
            onGameEnding(game.getWhitePlayer(), game.getBlackPlayer(), game.getGameState(), game);
        }
        log.info("makeMove, game: " + game );
        return game;
    }

    public void onGameEnding(User whitePlayer, User blackPlayer, GameState gameResult,Game game){
        GameRepresentation gameRepresentation = game.getGameRepresentation();
        gameRepresentation.setResult(gameResult);
        whitePlayer.addGameToWhiteGameHistory(gameRepresentation);
        blackPlayer.addGameToBlackGameHistory(gameRepresentation);
        whitePlayer.setTotalGames(game.getWhitePlayer().getTotalGames() + 1);
        blackPlayer.setTotalGames(game.getBlackPlayer().getTotalGames() + 1);
        if(gameResult == GameState.WHITE_WIN){
            whitePlayer.setElo(EloCalculator.calculateEloAfterWin(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterLoss(blackPlayer.getElo(), whitePlayer.getElo()));
            whitePlayer.setGamesWon(whitePlayer.getGamesWon() + 1);
            blackPlayer.setGamesLost(blackPlayer.getGamesLost() + 1);
        } else if(gameResult == GameState.BLACK_WIN){
            whitePlayer.setElo(EloCalculator.calculateEloAfterLoss(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterWin(blackPlayer.getElo(), whitePlayer.getElo()));
            blackPlayer.setGamesWon(blackPlayer.getGamesWon() + 1);
            whitePlayer.setGamesLost(whitePlayer.getGamesLost() + 1);
        } else if (gameResult == GameState.DRAW) {
            whitePlayer.setElo(EloCalculator.calculateEloAfterDraw(whitePlayer.getElo(), blackPlayer.getElo()));
            blackPlayer.setElo(EloCalculator.calculateEloAfterDraw(blackPlayer.getElo(), whitePlayer.getElo()));
            whitePlayer.setGamesDrawn(whitePlayer.getGamesDrawn() + 1);
            blackPlayer.setGamesDrawn(blackPlayer.getGamesDrawn() + 1);
        }

        userRepository.save(whitePlayer);
        userRepository.save(blackPlayer);

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
    private GameState checkGameState(Game game){
        if(game.getHalfMoves() >= 100){
            return GameState.DRAW;
        }
        Board board = game.getBoard();
        if(game.getIsP1turn()){
            //black just played his move. check if white king is in check:
            if(board.isKingInCheck(true)){
                if(!board.canPlayerPlay(Color.WHITE)){
                    //black has been checkmated
                    return GameState.BLACK_WIN;
                }else{
                    return GameState.ACTIVE;
                }
            }else{
                if(!board.canPlayerPlay(Color.WHITE)){
                    return GameState.DRAW;
                }else{
                    return GameState.ACTIVE;
                }
            }
        }else{
            //white just played
            if(board.isKingInCheck(false)){
                if(!board.canPlayerPlay(Color.BLACK)){
                    //black has been checkmated
                    return GameState.WHITE_WIN;
                }else{
                    return GameState.ACTIVE;
                }
            }else{
                if(!board.canPlayerPlay(Color.BLACK)){
                    return GameState.DRAW;
                }else{
                    return GameState.ACTIVE;
                }
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


    public Game convertFENToGame(String fen) throws SquareNotFoundException {
        PieceFactory pieceFactory = new PieceFactory();
        Game game = new Game();
        game.setId(UUID.randomUUID().toString());
        Board board = new Board();
        game.setBoard(board);
        int col = 0, row = 7;
        while(row >= 0){
            if(row == 0 && col == 8){
                row--;
                continue;
            }
            char currentChar = fen.charAt(0);
            fen = fen.substring(1);
            if(currentChar == '/'){
                row--;
                col = 0;
                continue;
            }
            if(currentChar == ' '){
                continue;
            }
            try{
                col += Integer.parseInt(String.valueOf(currentChar));
                continue;
            } catch (NumberFormatException ignored) {}
            //currentChar is a piece
            Color pieceColor;
            if(Character.isUpperCase(currentChar)){
                pieceColor = Color.WHITE;
            } else{
                pieceColor = Color.BLACK;
            }
            PieceType pieceType = null;
            boolean hasMoved = true;
            switch (currentChar){
                case 'Q', 'q' -> pieceType = PieceType.QUEEN;
                case 'R', 'r' -> pieceType = PieceType.ROOK;
                case 'B', 'b' -> pieceType = PieceType.BISHOP;
                case 'N', 'n' -> pieceType = PieceType.KNIGHT;
                case 'K', 'k' -> pieceType = PieceType.KING;
                case 'P', 'p' ->{
                    pieceType = PieceType.PAWN;
                    if( (pieceColor == Color.WHITE && row == 1) || (pieceColor == Color.BLACK && row == 6)){
                        hasMoved = false;
                    }
                }
                default -> log.error("\n convertFromFen failed since the char: " + currentChar +"  isn't actually a piece");

            }
            assert pieceType != null;
            Piece curPiece = pieceFactory.getPiece(pieceType, pieceColor, board.getSquareAt(col, row), hasMoved);
            if(curPiece.getPieceType() == PieceType.KING){
                if(curPiece.getColor() == Color.WHITE){
                    board.setWhiteKing((King) curPiece);
                }
                else {
                    board.setBlackKing( (King) curPiece);
                }
            }
            board.setPieceAt(col, row, curPiece);
            col++;
        }
        //We should have finished all the pieces.
        log.info("Fen left is: " + fen);
        char curChar = fen.charAt(1);
        fen = fen.substring(2);
        game.setIsP1turn(curChar == 'w');
        curChar = fen.charAt(1);
        fen = fen.substring(2);
        if(curChar != '-'){
            while(curChar != ' '){
                if(curChar == 'Q'){
                    board.getSquareAt(4, 0).getPiece().setHasMoved(false); //White King
                    board.getSquareAt(0, 0).getPiece().setHasMoved(false); //White Rook on a1
                } else if(curChar == 'q'){
                    board.getSquareAt(4, 7).getPiece().setHasMoved(false); //Black King
                    board.getSquareAt(0, 7).getPiece().setHasMoved(false); //Black Rook on a8
                } else if(curChar == 'K'){
                    board.getSquareAt(4, 0).getPiece().setHasMoved(false); //White King
                    board.getSquareAt(7, 0).getPiece().setHasMoved(false); //White Rook on h1
                } else if(curChar == 'k'){
                    board.getSquareAt(4, 7).getPiece().setHasMoved(false); //Black King
                    board.getSquareAt(7, 7).getPiece().setHasMoved(false); //Black Rook on h8
                }
                curChar = fen.charAt(1);
                fen = fen.substring(1);
            }
        }
        //En-pasant
        if(fen.charAt(1) == '-'){
            board.setPhantomPawnSquare(null);
            fen = fen.substring(3);
        }else {
            int phantomPawnCol = switch (fen.charAt(1)){
                case 'a' -> 0;
                case 'b' -> 1;
                case 'c' -> 2;
                case 'd' -> 3;
                case 'e' -> 4;
                case 'f' -> 5;
                case 'g' -> 6;
                case 'h' -> 7;
                default -> -1;
            };
            if(phantomPawnCol == -1){
                log.error("\n convertFromFen failed since the col: " + fen.charAt(1) +"  isn't a col in algebreic notation");
            }
            int phantomPawnRow = Integer.parseInt(String.valueOf(fen.charAt(2)));
            board.setPhantomPawnSquare(board.getSquareAt(phantomPawnCol, phantomPawnRow));
            fen = fen.substring(4);
        }
        //Only thing left is half-moves and full-moves.
        //TODO: Convert the halfmoves/fullMoves as well so you could play a complete game from FEN
        game.setFullMoves(0);
        game.setHalfMoves(0);
        return game;

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
