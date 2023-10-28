package com.hellostranger.chessserver.models.game.pieces;

import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Board;
import com.hellostranger.chessserver.models.game.Square;

import java.util.ArrayList;

public class Knight extends Piece{
    private static final int[][] SPOT_INCREMENTS = {{2,1}, {2,-1}, {-2,1},{-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};

    public Knight(Color color, Square currentSquare){
        super(color, PieceType.KNIGHT, false, currentSquare);
    }
    public Knight(Color color, Square currentSquare, Boolean hasMoved){
        super(color, PieceType.KNIGHT, hasMoved, currentSquare);
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
        return getThreatenedSquares(board);
    }
}
