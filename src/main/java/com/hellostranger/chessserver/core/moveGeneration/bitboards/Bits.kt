package com.hellostranger.chessserver.core.moveGeneration.bitboards

import com.hellostranger.chessserver.core.helpers.BoardHelper
import kotlin.math.max
import kotlin.math.min

/**
 * A collection of precomputed bitboards for use during move gen, search, and more
 */
@ExperimentalUnsignedTypes
object Bits {
    const val fileA : ULong = 0x101010101010101UL
    const val rank1 : ULong = 0xFFUL

    val whiteKingsideMask = (1UL shl BoardHelper.f1) or (1UL shl BoardHelper.g1)
    val blackKingsideMask = (1UL shl BoardHelper.f8) or (1UL shl BoardHelper.g8)

    // This is used to check squares that might be attacked by the opponent, so king can't move there.
    val whiteQueensideMaskChecks = (1UL shl BoardHelper.d1) or (1UL shl BoardHelper.c1)
    val blackQueensideMaskChecks = (1UL shl BoardHelper.d8) or (1UL shl BoardHelper.c8)

    // This is the mask for the path itself. Can't have any piece in the way, but an enemy can threaten b1/b8
    val whiteQueensideMask = whiteQueensideMaskChecks or (1UL shl BoardHelper.b1)
    val blackQueensideMask = blackQueensideMaskChecks or (1UL shl BoardHelper.b8)

    val whitePassedPawnMasks : ULongArray = ULongArray(64)
    val blackPassedPawnMasks : ULongArray = ULongArray(64)

    val fileMasks          : ULongArray = ULongArray(8)
    val rankMasks          : ULongArray = ULongArray(8)
    val adjacentFileMasks  : ULongArray = ULongArray(8)

    // the index of the mask is calculated by 7 + rank - file. this gives us the following:
    // 14 13 12 11 10  9  8  7
    // 13 12 11 10  9  8  7  6
    // 12 11 10  9  8  7  6  5
    // 11 10  9  8  7  6  5  4
    // 10  9  8  7  6  5  4  3
    //  9  8  7  6  5  4  3  2
    //  8  7  6  5  4  3  2  1
    //  7  6  5  4  3  2  1  0
    // So the relevant mask for a square will be at index 7 + rank - file.
    val diagonalMasks     : ULongArray = ULongArray(15) {0UL} // Goes from bottom left to top right

    // For the anti diagonal, we can use rank + file.
    val antiDiagonalMasks : ULongArray = ULongArray(15) {0UL} // Goes from top left to bottom right

    fun ULong.reverse(): ULong {
        var value = this
        var result: ULong = 0u
        repeat(64) {
            result = (result shl 1) or (value and 1u)
            value = value shr 1
        }
        return result
    }

    init {
        for (i in 0 until 8) {
            fileMasks[i] = fileA shl i
            rankMasks[i] = if (i == 0) rank1 else rankMasks[i - 1] shl 8
            val left  : ULong = if (i > 0) fileA shl (i - 1) else 0UL
            val right : ULong = if (i < 7) fileA shl (i + 1) else 0UL
            adjacentFileMasks[i] = left or right
        }

        for (square in 0 until 64) {
            val file = BoardHelper.fileIndex(square)
            val rank = BoardHelper.rankIndex(square)
            val adjacentFiles : ULong = fileA shl max(0, file - 1) or fileA shl min(7, file + 1)

            // Passed Pawn mask
            val whiteForwardMask : ULong = (ULong.MAX_VALUE shr (64 - 8 * (rank + 1))).inv()
            val blackForwardMask : ULong = ((1UL shl 8 * rank) - 1UL)

            whitePassedPawnMasks[square] = (fileA shl file or adjacentFiles) and whiteForwardMask
            blackPassedPawnMasks[square] = (fileA shl file or adjacentFiles) and blackForwardMask

            // Diagonals
            diagonalMasks[7 + rank - file] =
                BitBoardUtility.toggleSquare(diagonalMasks[7 + rank - file], square)
            antiDiagonalMasks[rank + file] =
                BitBoardUtility.toggleSquare(antiDiagonalMasks[rank + file], square)

        }
    }



}