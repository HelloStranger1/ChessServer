package com.hellostranger.chessserver.core.board

class GameState(
    val capturedPieceType: Int = Piece.NONE,
    val enPassantFile: Int = 0,
    val castlingRights: Int = 0,
    val fiftyMoveCounter: Int = 0,
    val zobristKey: ULong = 0UL
) {
    companion object {

        const val CLEAR_WHITE_KINGSIDE_MASK  : Int = 0b1110
        const val CLEAR_WHITE_QUEENSIDE_MASK : Int = 0b1101
        const val CLEAR_BLACK_KINGSIDE_MASK  : Int = 0b1011
        const val CLEAR_BLACK_QUEENSIDE_MASK : Int = 0b0111
    }

    fun hasKingSideCastleRights(isWhite: Boolean): Boolean {
        val mask = if (isWhite) 1 else 4
        return (castlingRights and mask) != 0
    }

    fun hasQueenSideCastleRights(isWhite: Boolean): Boolean {
        val mask = if (isWhite) 2 else 8
        return (castlingRights and mask) != 0
    }
}
