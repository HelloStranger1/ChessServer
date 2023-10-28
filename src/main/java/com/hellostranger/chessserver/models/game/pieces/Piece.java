package com.hellostranger.chessserver.models.game.pieces;

import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Board;
import com.hellostranger.chessserver.models.game.Square;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@EqualsAndHashCode
@Slf4j
public abstract class Piece {

    protected Color color;

    protected PieceType pieceType;

    protected Boolean hasMoved;


    protected int colIndex;


    protected int rowIndex;

    protected Piece(Color color, PieceType type, Boolean hasMoved, Square currentSquare){
        this.color = color;
        this.pieceType = type;
        this.hasMoved = hasMoved;
        this.colIndex = currentSquare.getColIndex();
        this.rowIndex = currentSquare.getRowIndex();
    }

    protected Piece(Color color, PieceType type, Boolean hasMoved, int colIndex, int rowIndex){
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

    //Simply changes the position of the piece.
    public void move(Square target){
        this.colIndex = target.getColIndex();
        this.rowIndex = target.getRowIndex();
    }

    public abstract Square[] getThreatenedSquares(Board board);

    public abstract Square[] getMovableSquares(Board board);



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





}
