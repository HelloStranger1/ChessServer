package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.models.game.pieces.Piece;
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
