package com.hellostranger.chessserver.core.helpers

import com.hellostranger.chessserver.core.board.Board
import com.hellostranger.chessserver.core.board.Coord
import com.hellostranger.chessserver.core.board.Move
import com.hellostranger.chessserver.core.board.Piece
import kotlin.math.abs

object MoveUtility {

    // Converts a moveName into internal move representation
    // Name is expected in UCI format, so for example: "e2e4"
    // Promotions can be written as "e7e8=q" or "e7e8q"
    @JvmStatic
    fun getMoveFromUCIName(moveName : String, board: Board) : Move {
        val startSquare  = BoardHelper.squareIndexFromName(moveName.substring(0,2))
        val targetSquare = BoardHelper.squareIndexFromName(moveName.substring(2,2))

        val movedPieceType = Piece.pieceType(board.square[startSquare])
        val startCoord =  Coord(startSquare)
        val targetCoord = Coord(targetSquare)

        var flag = Move.NO_FLAG

        if (movedPieceType == Piece.PAWN) {
            // Promotion
            if (moveName.length > 4) {
                flag = when(moveName.last()) {
                    'q' -> { Move.PROMOTE_TO_QUEEN_FLAG  }
                    'r' -> { Move.PROMOTE_TO_ROOK_FLAG   }
                    'b' -> { Move.PROMOTE_TO_BISHOP_FLAG }
                    'n' -> { Move.PROMOTE_TO_KNIGHT_FLAG }
                    else -> { Move.NO_FLAG}
                }
            } else if (abs(targetCoord.rankIndex - startCoord.rankIndex) == 2) {
                // Double push
                flag = Move.PAWN_TWO_UP_FLAG
            } else if (startCoord.fileIndex != targetCoord.fileIndex && board.square[targetSquare] == Piece.NONE) {
                // En Passant
                flag = Move.EN_PASSANT_CAPTURE_FLAG
            }
        } else if (movedPieceType == Piece.KING) {
            if (abs(startCoord.fileIndex - targetCoord.fileIndex) > 1) {
                flag = Move.CASTLE_FLAG
            }
        }

        return Move(startSquare, targetSquare, flag)
    }

    // Get algebraic name of move (with promotion specified)
    @JvmStatic
    fun getMoveNameUCI(move: Move) : String {
        val startSquareName : String = BoardHelper.squareNameFromIndex(move.startSquare)
        val endSquareName   : String = BoardHelper.squareNameFromIndex(move.targetSquare)
        var moveName : String = startSquareName + endSquareName
        if (!move.isPromotion) {
            return moveName
        }
        when (move.moveFlag) {
            Move.PROMOTE_TO_QUEEN_FLAG -> {
                moveName += "q"
            }
            Move.PROMOTE_TO_ROOK_FLAG -> {
                moveName += "r"
            }
            Move.PROMOTE_TO_BISHOP_FLAG -> {
                moveName += "b"
            }
            Move.PROMOTE_TO_KNIGHT_FLAG -> {
                moveName += "n"
            }
        }
        return moveName
    }
}