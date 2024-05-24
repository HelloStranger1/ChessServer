package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.core.GameResult;
import com.hellostranger.chessserver.core.board.Board;
import com.hellostranger.chessserver.core.helpers.BoardHelper;
import com.hellostranger.chessserver.core.moveGeneration.MoveGenerator;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Game {

    private String id;


    @JsonIgnore
    private Board board;

    @JsonIgnore
    private User whitePlayer;

    @JsonIgnore
    private User blackPlayer;

    @JsonIgnore
    private User waitingPlayer;

    @JsonIgnore
    public MoveGenerator moveGenerator = new MoveGenerator();

    private GameResult gameState;


    private List<String> boardsFen;

    private Boolean isDrawOfferedByWhite = false;
    private Boolean isDrawOfferedByBlack = false;

    @JsonIgnore
    private GameRepresentation gameRepresentation = null;

    public void addFenToList(String fen) {
        if (boardsFen == null) {
            boardsFen = new ArrayList<>();
        }
        boardsFen.add(fen);
    }

    @Override
    public String toString() {
        if (whitePlayer == null || blackPlayer == null) {
            return "Board is: " + BoardHelper.createDiagram(board, true, true, true);
        }
        return "White email is: " + whitePlayer.getEmail() + " Black email is: " + blackPlayer.getEmail() + "and the board is: \n"+ BoardHelper.createDiagram(board, true, true, true);
    }
//    public String convertGameToFEN(){
//        StringBuilder FEN = new StringBuilder();
//        int emptySquareCount = 0;
//        for(int row = 7; row >= 0; row--){
//            for(int col = 7; col >= 0; col --){
//                try{
//                    Square currentSquare = this.getBoard().getSquareAt(col, row);
//                    if(currentSquare.getPiece() != null){
//                        if(emptySquareCount != 0){
//                            FEN.append(emptySquareCount);
//                        }
//                        char pieceChar = getPieceChar(currentSquare.getPiece());
//                        FEN.append(pieceChar);
//                    }else{
//                        emptySquareCount++;
//                    }
//                } catch (SquareNotFoundException ignored){
//                }
//            }
//            FEN.append("/");
//        }
//        if(this.getIsP1turn()){
//            FEN.append(" w");
//        }else{
//            FEN.append(" b");
//        }
//
//        StringBuilder castleRepresentation = new StringBuilder();
//
//        try{
//            //white king side
//            if(this.getBoard().isCastlingMove(
//                    this.getBoard().getSquareAt(3, 0),
//                    this.getBoard().getSquareAt(7,0))
//            ) {
//                castleRepresentation.append("K");
//            }
//            //white queen side
//            if(this.getBoard().isCastlingMove(
//                    this.getBoard().getSquareAt(3, 0),
//                    this.getBoard().getSquareAt(1,0))
//            ) {
//                castleRepresentation.append("Q");
//            }
//
//            //black king side
//            if(this.getBoard().isCastlingMove(
//                    this.getBoard().getSquareAt(3, 7),
//                    this.getBoard().getSquareAt(7,7))
//            ) {
//                castleRepresentation.append("k");
//            }
//            //black queen side
//            if(this.getBoard().isCastlingMove(
//                    this.getBoard().getSquareAt(3, 7),
//                    this.getBoard().getSquareAt(1,7))
//            ) {
//                castleRepresentation.append("q");
//            }
//        } catch (SquareNotFoundException ignored){
//        }
//        if(castleRepresentation.isEmpty()){
//            FEN.append(" -");
//        }else{
//            FEN.append(" ");
//            FEN.append(castleRepresentation);
//
//        }
//
//        //add "phantom pawn"
//        if(this.getBoard().getPhantomPawnSquare() != null){
//            int phantomCol = this.getBoard().getPhantomPawnSquare().getColIndex();
//            int phantomRow = this.getBoard().getPhantomPawnSquare().getRowIndex();
//            switch (phantomCol){
//                case 0 -> FEN.append(" a");
//                case 1 -> FEN.append(" b");
//                case 2 -> FEN.append(" c");
//                case 3 -> FEN.append(" d");
//                case 4 -> FEN.append(" e");
//                case 5 -> FEN.append(" f");
//                case 6 -> FEN.append(" g");
//                case 7 -> FEN.append(" h");
//            }
//            FEN.append(phantomRow+1);
//        }
//
//        String movesCount = " " +this.getHalfMoves() + " " + this.getFullMoves();
//        FEN.append(movesCount);
//        return FEN.toString();
//    }
//    private char getPieceChar(Piece piece){
//        boolean isPieceWhite = piece.getColor() == Color.WHITE;
//        switch (piece.getPieceType()){
//            case KING -> {
//                if(isPieceWhite) return 'K'; else return 'k';
//            }
//            case QUEEN -> {
//                if(isPieceWhite) return 'Q'; else return 'q';
//            }
//            case ROOK -> {
//                if(isPieceWhite) return 'R'; else return 'r';
//            }
//            case BISHOP -> {
//                if(isPieceWhite) return 'B'; else return 'b';
//            }
//            case KNIGHT -> {
//                if(isPieceWhite) return 'N'; else return 'n';
//            }
//            case PAWN -> {
//                if(isPieceWhite) return 'P'; else return 'p';
//            }
//        }
//        return '\0';
//    }
}
