package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Square {


    @JsonIgnore
    private transient Board board;

    private Piece piece;

    private int colIndex;

    private int rowIndex;

    Square(int colIndex, int rowIndex){
        this.colIndex = colIndex;
        this.rowIndex = rowIndex;
    }

    Square(Square otherSquare){
        this.board = otherSquare.getBoard();
        if(otherSquare.getPiece() != null){
            this.piece = new Piece(otherSquare.getPiece());
        } else{
            this.piece = null;
        }
        this.colIndex = otherSquare.getColIndex();
        this.rowIndex = otherSquare.getRowIndex();
    }

    @Override
    public String toString() {
        return "Square{" +
                "piece=" + piece +
                ", colIndex=" + colIndex +
                ", rowIndex=" + rowIndex +
                '}';
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof Square){
            return ((Square) other).getRowIndex() == rowIndex && ((Square) other).getColIndex() == colIndex;
        }else{
            return false;
        }
    }
}
