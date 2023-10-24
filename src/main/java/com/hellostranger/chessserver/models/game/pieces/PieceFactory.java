package com.hellostranger.chessserver.models.game.pieces;

import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Square;

public class PieceFactory {

    public Piece getPiece(PieceType pieceType, Color color, Square square){
        return switch (pieceType){
            case KING -> new King(color, square);
            case QUEEN -> new Queen(color, square);
            case ROOK -> new Rook(color, square);
            case BISHOP -> new Bishop(color, square);
            case KNIGHT -> new Knight(color, square);
            case PAWN -> new Pawn(color, square);
        };
    }
    public Piece getPiece(PieceType pieceType, Color color, Square square, Boolean hasMoved){
        return switch (pieceType){
            case KING -> new King(color, square, hasMoved);
            case QUEEN -> new Queen(color, square, hasMoved);
            case ROOK -> new Rook(color, square, hasMoved);
            case BISHOP -> new Bishop(color, square, hasMoved);
            case KNIGHT -> new Knight(color, square, hasMoved);
            case PAWN -> new Pawn(color, square, hasMoved);
        };
    }


}
