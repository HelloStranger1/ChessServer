package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@EqualsAndHashCode
@Slf4j
public class Piece {

    private Color color;

    private PieceType pieceType;

    private Boolean hasMoved;

    @JsonIgnore
    private int colIndex;

    @JsonIgnore
    private int rowIndex;


    @Override
    public String toString() {
        return "Piece{" +
                "color=" + color +
                ", pieceType=" + pieceType +
                ", hasMoved=" + hasMoved +
                ", colIndex=" + colIndex +
                ", rowIndex=" + rowIndex +
                '}';
    }

    public Piece(Color color, PieceType type, Boolean hasMoved, Square currentSquare){
        this.color = color;
        this.pieceType = type;
        this.hasMoved = hasMoved;
        this.colIndex = currentSquare.getColIndex();
        this.rowIndex = currentSquare.getRowIndex();


    }

    public Piece(Color color, PieceType type, Boolean hasMoved, int colIndex, int rowIndex){
        this.color = color;
        this.pieceType = type;
        this.hasMoved = hasMoved;
        this.colIndex = colIndex;
        this.rowIndex = rowIndex;


    }

    public Piece(Piece otherPiece){
        this.color = otherPiece.getColor();
        this.pieceType = otherPiece.getPieceType();
        this.hasMoved = otherPiece.getHasMoved();
        this.colIndex = otherPiece.getColIndex();
        this.rowIndex = otherPiece.getRowIndex();

    }

    public Boolean canMakeMove(Square currentSquare, Square targetSquare){
        switch (pieceType) {
            case KING -> {
                return canKingMove(currentSquare, targetSquare);
            }
            case QUEEN -> {
                return canQueenMove(currentSquare, targetSquare);
            }
            case ROOK -> {
                return canRookMove(currentSquare, targetSquare);
            }
            case BISHOP -> {
                return canBishopMove(currentSquare, targetSquare);
            }
            case KNIGHT -> {
                return canKnightMove(currentSquare, targetSquare);
            }
            case PAWN -> {
                return canPawnMove(currentSquare, targetSquare);
            }
        }

        log.info("CanPieceMove didn't return from switch.");
        return false;
    }
    private Boolean canKingMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null && targetSquare.getPiece().getColor() == this.getColor()) {
            //the king and rook haven't moved, so its castling
            return !hasMoved && targetSquare.getPiece().pieceType == PieceType.ROOK && !targetSquare.getPiece().getHasMoved();

        }

        int x = Math.abs(currentSquare.getColIndex() - targetSquare.getColIndex());
        int y = Math.abs(currentSquare.getRowIndex() - targetSquare.getRowIndex());
        return x < 2 && y < 2 && x+y != 0;
    }

    private Boolean canQueenMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null && targetSquare.getPiece().getColor() == this.getColor()) {
            return false;
        }

        int colDiff = Math.abs(targetSquare.getColIndex() - currentSquare.getColIndex());
        int rowDiff = Math.abs(targetSquare.getRowIndex() - currentSquare.getRowIndex());
        if(colDiff == 0 || rowDiff == 0 || colDiff == rowDiff){
            log.info("Queen can move from ");
        }
        return colDiff == 0 || rowDiff == 0 || colDiff == rowDiff;
    }

    private Boolean canRookMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null &&  targetSquare.getPiece().getColor() == this.getColor()) {
            return false;
        }

        int colDiff = Math.abs(targetSquare.getColIndex() - currentSquare.getColIndex());
        int rowDiff = Math.abs(targetSquare.getRowIndex() - currentSquare.getRowIndex());

        return colDiff == 0 || rowDiff == 0;
    }


    private Boolean canBishopMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null && targetSquare.getPiece().getColor() == this.getColor()) {
            return false;
        }


        int colDiff = Math.abs(currentSquare.getColIndex() - targetSquare.getColIndex());
        int rowDiff = Math.abs(currentSquare.getRowIndex() - targetSquare.getRowIndex());
        log.info(String.valueOf(" can bishop move?" + colDiff==rowDiff + " from " + currentSquare + "to square" + targetSquare));
        return colDiff == rowDiff;
    }

    private Boolean canKnightMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null && targetSquare.getPiece().getColor() == this.getColor()) {
            return false;
        }

        int x = Math.abs(currentSquare.getColIndex() - targetSquare.getColIndex());
        int y = Math.abs(currentSquare.getRowIndex() - targetSquare.getRowIndex());
        return x * y == 2;
    }

    private Boolean canPawnMove(Square currentSquare, Square targetSquare) {
        if (targetSquare.getPiece() != null && targetSquare.getPiece().getColor() == this.getColor()) {
            return false;
        }

        if(this.getColor() == Color.BLACK){
            if (currentSquare.getColIndex() == targetSquare.getColIndex()) {
                return (currentSquare.getRowIndex() - targetSquare.getRowIndex() == 1) ||
                        (!this.hasMoved && (currentSquare.getRowIndex() - targetSquare.getRowIndex() == 2));
            } else if (Math.abs(currentSquare.getColIndex() - targetSquare.getColIndex()) == 1) {
                if(currentSquare.getRowIndex() - targetSquare.getRowIndex() == 1){
                    if(targetSquare.getPiece() != null){
                        return true;
                    }else if(targetSquare.getBoard().getPhantomPawnSquare() != null){
                        return targetSquare.getBoard().getPhantomPawnSquare().equals(targetSquare);
                    }else{
                        return false;
                    }
                }
            }
        }else{
            if (currentSquare.getColIndex() == targetSquare.getColIndex()) {
                return (currentSquare.getRowIndex() - targetSquare.getRowIndex() == -1) ||
                        (!this.hasMoved && (currentSquare.getRowIndex() - targetSquare.getRowIndex() == -2));
            } else if (Math.abs(currentSquare.getColIndex() - targetSquare.getColIndex()) == 1) {
                if(currentSquare.getRowIndex() - targetSquare.getRowIndex() == -1){
                    if(targetSquare.getPiece() != null){
                        return true;
                    }else if(targetSquare.getBoard().getPhantomPawnSquare() != null){
                        return targetSquare.getBoard().getPhantomPawnSquare().equals(targetSquare);
                    }else{
                        return false;
                    }
                }
            }
        }
        return false;
    }


}
