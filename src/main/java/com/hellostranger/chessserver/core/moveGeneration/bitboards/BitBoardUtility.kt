@file:OptIn(ExperimentalUnsignedTypes::class)

package com.hellostranger.chessserver.core.moveGeneration.bitboards

import com.hellostranger.chessserver.core.helpers.BoardHelper
import com.hellostranger.chessserver.core.moveGeneration.bitboards.Bits.reverse

object BitBoardUtility {

    const val fileA: ULong = 0x101010101010101UL

    const val rank1: ULong = 0b11111111UL
    val rank2: ULong = rank1 shl 8
    val rank3: ULong = rank2 shl 8
    val rank4: ULong = rank3 shl 8
    val rank5: ULong = rank4 shl 8
    val rank6: ULong = rank5 shl 8
    val rank7: ULong = rank6 shl 8
    val rank8: ULong = rank7 shl 8

    val notAFile: ULong = fileA.inv()
    val notHFile: ULong = (fileA shl 7).inv()

    val knightAttacks = Array(64) { 0UL }
    val kingMoves = Array(64) { 0UL }
    val whitePawnAttacks = Array(64) { 0UL }
    val blackPawnAttacks = Array(64) { 0UL }

    // Get Index of least significant set bit in given 64 bit value, and clears that bit

    fun getLSB(b : ULong) : Int {
        return java.lang.Long.numberOfTrailingZeros(b.toLong())
    }

    fun clearLSB(u : ULong) : ULong {
        return u and (u - 1u)
    }

    fun setSquare(bitboard: ULong, squareIndex: Int) : ULong {
        return bitboard or (1UL shl squareIndex)
    }

    fun clearSquare(bitboard: ULong, squareIndex: Int) : ULong {
        return bitboard and (1UL shl squareIndex).inv()
    }

    fun toggleSquare(bitboard: ULong, squareIndex: Int) : ULong {
        return bitboard xor (1UL shl squareIndex)
    }

    fun toggleSquares(bitboard: ULong, squareA: Int, squareB: Int) : ULong {
        return bitboard xor ((1UL shl squareA) or (1UL shl squareB))
    }

    fun containsSquare(bitboard: ULong, square: Int): Boolean {
        return ((bitboard shr square) and 1UL) != 0UL
    }

    fun pawnAttacks(pawnBitboard: ULong, isWhite: Boolean): ULong {
        // Pawn attacks are calculated like so: (example given with white to move)

        // The first half of the attacks are calculated by shifting all pawns north-east: northEastAttacks = pawnBitboard << 9
        // Note that pawns on the h file will be wrapped around to the a file, so then mask out the a file: northEastAttacks &= notAFile
        // (Any pawns that were originally on the a file will have been shifted to the b file, so a file should be empty).

        // The other half of the attacks are calculated by shifting all pawns north-west. This time the h file must be masked out.
        // Combine the two halves to get a bitboard with all the pawn attacks: northEastAttacks | northWestAttacks{

        return if (isWhite) {
            ((pawnBitboard shl 9) and notAFile) or (pawnBitboard shl 7 and notHFile)
        } else {
            (pawnBitboard shr 7 and notAFile) or (pawnBitboard shr 9 and notHFile)
        }
    }

    fun getSliderAttacks(square: Int, blockers: ULong, ortho: Boolean): ULong {
        return if (ortho) getRookAttacks(square, blockers) else getBishopAttacks(square, blockers)
    }

    fun getRookAttacks(square: Int, blockers: ULong): ULong {
        val binarySquare : ULong = 1UL shl square
        val fileMask = Bits.fileMasks[BoardHelper.fileIndex(square)]
        val rankMask = Bits.rankMasks[BoardHelper.rankIndex(square)]
        val possibleHorizontal = (blockers - 2UL * binarySquare) xor (blockers.reverse() - 2UL * binarySquare.reverse()).reverse()
        val possibleVertical = ((blockers and fileMask) - (2UL * binarySquare)) xor ((blockers and fileMask).reverse() - 2UL * (binarySquare.reverse())).reverse()

        return (possibleHorizontal and rankMask) or (possibleVertical and fileMask)
    }

    fun getBishopAttacks(square: Int, blockers: ULong): ULong {
        val binarySquare : ULong = 1UL shl square
        val diagonalMask     = Bits.diagonalMasks[7 + BoardHelper.rankIndex(square) - BoardHelper.fileIndex(square)]
        val antiDiagonalMask = Bits.antiDiagonalMasks[BoardHelper.rankIndex(square) + BoardHelper.fileIndex(square)]
        val possibleDiagonal = ((blockers and diagonalMask) - (2UL * binarySquare)) xor ((blockers and diagonalMask).reverse() - 2UL * (binarySquare.reverse())).reverse()
        val possibleAntiDiagonal = ((blockers and antiDiagonalMask) - (2UL * binarySquare)) xor ((blockers and antiDiagonalMask).reverse() - 2UL * (binarySquare.reverse())).reverse()

        return (possibleDiagonal and diagonalMask) or (possibleAntiDiagonal and antiDiagonalMask)
    }

    fun shift(bitboard: ULong, numSquaresToShift: Int): ULong {
        return if (numSquaresToShift > 0) {
            bitboard shl numSquaresToShift
        } else {
            bitboard shr -numSquaresToShift
        }
    }

    init {
        val orthoDir = arrayOf((-1 to 0), (0 to 1), (1 to 0), (0 to -1))
        val diagDir = arrayOf((-1 to -1), (-1 to 1), (1 to 1), (1 to -1))
        val knightJumps = arrayOf((-2 to -1), (-2 to 1), (-1 to 2), (1 to 2), (2 to 1), (2 to -1), (1 to -2), (-1 to -2))

        fun processSquare(x: Int, y: Int) {
            val squareIndex = y * 8 + x

            for (dirIndex in 0 until 4) {
                // Orthogonal and diagonal directions
                for (dst in 1 until 8) {
                    val (orthoDirX, orthoDirY) = orthoDir[dirIndex]
                    val (diagDirX, diagDirY) = diagDir[dirIndex]

                    val orthoX = x + orthoDirX * dst
                    val orthoY = y + orthoDirY * dst
                    val diagX  = x + diagDirX  * dst
                    val diagY  = y + diagDirY  * dst

                    if (validSquareIndex(orthoX, orthoY)) {
                        val orthoTargetIndex = orthoX + orthoY * 8
                        if (dst == 1) {
                            kingMoves[squareIndex] = kingMoves[squareIndex] or (1UL shl orthoTargetIndex)
                        }
                    }

                    if (validSquareIndex(diagX, diagY)) {
                        val diagTargetIndex = diagX + diagY * 8
                        if (dst == 1) {
                            kingMoves[squareIndex] = kingMoves[squareIndex] or (1UL shl diagTargetIndex)
                        }
                    }
                }

                // Knight jumps
                for ((knightJumpX, knightJumpY) in knightJumps) {
                    val knightX = x + knightJumpX
                    val knightY = y + knightJumpY
                    if (validSquareIndex(knightX, knightY)) {
                        val knightTargetSquare = x + knightJumpX + (y + knightJumpY) * 8
                        knightAttacks[squareIndex] = knightAttacks[squareIndex] or (1UL shl knightTargetSquare)
                    }
                }

                // Pawn attacks
                if ((x + 1) in 0 until 8 && (y + 1) in 0 until 8) {
                    val whitePawnRight = x + 1 + (y + 1) * 8
                    whitePawnAttacks[squareIndex] = whitePawnAttacks[squareIndex] or (1UL shl whitePawnRight)
                }
                if ((x - 1) in 0 until 8 && (y + 1) in 0 until 8) {
                    val whitePawnLeft = x - 1 + (y + 1) * 8
                    whitePawnAttacks[squareIndex] = whitePawnAttacks[squareIndex] or (1UL shl whitePawnLeft)
                }

                if ((x + 1) in 0 until 8 && (y - 1) in 0 until 8) {
                    val blackPawnAttackRight = x + 1 + (y - 1) * 8
                    blackPawnAttacks[squareIndex] = blackPawnAttacks[squareIndex] or (1UL shl blackPawnAttackRight)
                }
                if ((x - 1) in 0 until 8 && (y - 1) in 0 until 8) {
                    val blackPawnAttackLeft = x - 1 + (y - 1) * 8
                    blackPawnAttacks[squareIndex] = blackPawnAttacks[squareIndex] or (1UL shl blackPawnAttackLeft)
                }
            }
        }

        for (y in 0 until 8) {
            for (x in 0 until 8) {
                processSquare(x, y)
            }
        }
    }
    private fun validSquareIndex(x : Int, y : Int) : Boolean {
        return x in 0..7 && y in 0..7
    }


}
