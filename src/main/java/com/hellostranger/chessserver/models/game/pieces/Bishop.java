package com.hellostranger.chessserver.models.game.pieces;

import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Board;
import com.hellostranger.chessserver.models.game.Square;

import java.util.ArrayList;
import java.util.Arrays;

public class Bishop extends Piece{
    private static final int[][] BEAM_INCREMENTS = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
    public Bishop(Color color, Square currentSquare){
        super(color, PieceType.BISHOP, false, currentSquare);
    }

    public Bishop(Color color, Square currentSquare, Boolean hasMoved){
        super(color, PieceType.BISHOP, hasMoved, currentSquare);
    }
    @Override
    public Square[] getThreatenedSquares(Board board) {
        ArrayList<Square> positions = new ArrayList<>();
        for(int[] increment : BEAM_INCREMENTS){
            Square[] squares = board.beamSearchThreat(this.getRowIndex(), this.getColIndex(), this.getColor(), increment[0], increment[1]);
            positions.addAll(Arrays.asList(squares));

        }
        Square[] positionsArr = new Square[positions.size()];
        return positions.toArray(positionsArr);
    }

    @Override
    public Square[] getMovableSquares(Board board) {
        return getThreatenedSquares(board);
    }
}
