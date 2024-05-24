
@file:OptIn(ExperimentalUnsignedTypes::class)
package com.hellostranger.chessserver.core.board

import kotlin.random.Random
import kotlin.random.nextULong

// Helper class for the calculation of zobrist hash.
// This is a single 64bit value that (non-uniquely) represents the current state of the game.

// It is mainly used for quickly detecting positions that have already been evaluated, to avoid
// potentially performing lots of duplicate work during game search.
object Zobrist {
    // Random numbers are generated for each aspect of the game state, and are used for calculating the hash:

    // piece type, colour, square index
    val piecesArray : Array<ULongArray> = Array(Piece.MaxPieceIndex + 1) {ULongArray(64) {0UL} }

    // Each side has 4 possible castling right states: none, kingside, queenside, both.
    // So, overall, we have 16 options
    val castlingRights = ULongArray(16)

    // En passant file (0 = no ep)
    // Rank doesn't need to be specified since side to move is included in key
    val enPassantFile : ULongArray = ULongArray(9)
    val sideToMove : ULong

    init {
        val seed = 29426028
        val rng = Random(seed)

        for (squareIndex in 0 until 64) {
            for (piece in Piece.pieceIndices) {
                piecesArray[piece][squareIndex] = randomUnsigned64BitNumber(rng)
            }
        }

        for (i in castlingRights.indices) {
            castlingRights[i] = randomUnsigned64BitNumber(rng)
        }

        for (i in enPassantFile.indices) {
            if (i == 0) {
                enPassantFile[i] = 0UL
            } else {
                enPassantFile[i] = randomUnsigned64BitNumber(rng)
            }
        }

        sideToMove = randomUnsigned64BitNumber(rng)
    }


    // Calculate Zobrist Key from current board position
    // Note: this function is slow and should only be used when the board is initially being set up
    // During search, the key should be updated incrementally
    @JvmStatic
    fun calculateZoboristKey(board: Board) : ULong {
        var zobristKey : ULong = 0UL

        for (squareIndex in 0 until 64) {
            val piece : Int = board.square[squareIndex]

            if (Piece.pieceType(piece) != Piece.NONE) {
                zobristKey = zobristKey xor piecesArray[piece][squareIndex]
            }
        }

        zobristKey = zobristKey xor enPassantFile[board.currentGameState.enPassantFile]

        if (board.moveColour == Piece.BLACK) {
            zobristKey = zobristKey xor sideToMove
        }

        zobristKey = zobristKey xor castlingRights[board.currentGameState.castlingRights]

        return zobristKey
    }


    private fun randomUnsigned64BitNumber(rng: Random): ULong {
        return rng.nextULong()
    }
}