package com.hellostranger.chessserver.models.game.pieces;


import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Board;
import com.hellostranger.chessserver.models.game.Square;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece{
    private static final int[][] SPOT_INCREMENTS = {{1,-1}, {1,0}, {1,1},{0,1}, {0,-1}, {-1,-1}, {-1,0}, {-1,1}};

    public King(Color color, Square currentSquare){
        super(color, PieceType.KING, false, currentSquare);
    }
    public King(Color color, Square currentSquare, Boolean hasMoved){
        super(color, PieceType.KING, hasMoved, currentSquare);
    }


    @Override
    public Square[] getThreatenedSquares(Board board) {
        ArrayList<Square> positions = new ArrayList<>();
        for(int[] increment : SPOT_INCREMENTS){
            Square target = board.spotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], increment[1]);
            if(target != null){
                positions.add(target);
            }
        }
        Square[] positionsArr = new Square[positions.size()];
        return positions.toArray(positionsArr);
    }

    @Override
    public Square[] getMovableSquares(Board board) {
        Square[] regularMoves = getThreatenedSquares(board);
        if(this.hasMoved){
            return regularMoves;
        }
        ArrayList<Square>  movableSquares = new ArrayList<>(List.of(regularMoves));
        Square[][] squares = board.getSquaresArray();

        if(squares[this.rowIndex][0].getPiece() != null && !squares[this.rowIndex][0].getPiece().hasMoved){
            //King can long castle
            movableSquares.add(squares[this.rowIndex][0]);
        }
        if(squares[this.rowIndex][7].getPiece() != null && !squares[this.rowIndex][7].getPiece().hasMoved){
            //King can long castle
            movableSquares.add(squares[this.rowIndex][7]);
        }
        Square[] movesArr = new Square[movableSquares.size()];
        return movableSquares.toArray(movesArr);
    }
}
