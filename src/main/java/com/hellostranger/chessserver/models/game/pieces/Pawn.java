package com.hellostranger.chessserver.models.game.pieces;


import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.Board;
import com.hellostranger.chessserver.models.game.Square;

import java.util.ArrayList;

public class Pawn extends Piece{
    private static final int[][] SPOT_INCREMENTS_MOVE = {{0,1}};
    private static final int[][] SPOT_INCREMENTS_MOVE_FIRST = {{0,1},{0,2}};
    private static final int[][] SPOT_INCREMENTS_TAKE = {{-1,1}, {1,1}};

    public Pawn(Color color, Square currentSquare){
        super(color, PieceType.PAWN, false, currentSquare);
    }
    public Pawn(Color color, Square currentSquare, Boolean hasMoved){
        super(color, PieceType.PAWN, hasMoved, currentSquare);
    }

    @Override
    public Square[] getThreatenedSquares(Board board) {
        ArrayList<Square> positions = new ArrayList<>();
        for(int[] increment : SPOT_INCREMENTS_TAKE){
            Square target;
            if(this.color == Color.WHITE){
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], increment[1], false);
            } else{
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], -1*increment[1], false);
            }
            if(target != null){
                positions.add(target);
            }
        }
        Square[] positionsArr = new Square[positions.size()];
        return positions.toArray(positionsArr);
    }

    @Override
    public Square[] getMovableSquares(Board board) {
        ArrayList<Square> positions = new ArrayList<>();
        int[][] increments;
        if(this.hasMoved){
            increments = SPOT_INCREMENTS_MOVE;
        }else{
            increments = SPOT_INCREMENTS_MOVE_FIRST;
        }
        for(int[] increment : increments) {
            Square target;
            if (this.color == Color.WHITE) {
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], increment[1], false);
            } else {
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], -1 * increment[1], false);
            }
            if (target != null) {
                positions.add(target);
            }
        }
        for(int[] increment : SPOT_INCREMENTS_TAKE){
            Square target;
            if(this.color == Color.WHITE){
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], increment[1], true);
            } else{
                target = board.pawnSpotSearchThreat(this.rowIndex, this.colIndex, this.color, increment[0], -1*increment[1], true);
            }
            if(target != null){
                positions.add(target);
            }
        }
        Square[] positionsArr = new Square[positions.size()];
        return positions.toArray(positionsArr);
    }
}
