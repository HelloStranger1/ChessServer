@file:OptIn(ExperimentalUnsignedTypes::class)

package com.hellostranger.chessserver.core.moveGeneration

import com.hellostranger.chessserver.core.board.Board
import com.hellostranger.chessserver.core.board.Coord
import com.hellostranger.chessserver.core.helpers.BoardHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

object PrecomputedMoveData {

    val alignMask: Array<ULongArray>
    val dirRayMask: Array<ULongArray>

    // First 4 are orthogonal, last 4 are diagonals (N, S, W, E, NW, SE, NE, SW)
    val directionOffsets = intArrayOf(8, -8, -1, 1, 7, -7, 9, -9)

    private val dirOffsets2D = arrayOf(
        Coord(0, 1),
        Coord(0, -1),
        Coord(-1, 0),
        Coord(1, 0),
        Coord(-1, 1),
        Coord(1, -1),
        Coord(1, 1),
        Coord(-1, -1)
    )

    // Stores number of moves available in each of the 8 directions for every square on the board
    // Order of directions is: N, S, W, E, NW, SE, NE, SW
    // So for example, if availableSquares[0][1] == 7...
    // that means that there are 7 squares to the north of b1 (the square with index 1 in board array)
    val numSquaresToEdge: Array<IntArray>

    // Stores array of indices for each square a knight can land on from any square on the board
    // So for example, knightMoves[0] is equal to {10, 17}, meaning a knight on a1 can jump to c2 and b3
    val knightMoves: Array<ByteArray>
    val kingMoves: Array<ByteArray>

    // Pawn attack directions for white and black (NW, NE; SW SE)
    val pawnAttackDirections = arrayOf(
        byteArrayOf(4, 6),
        byteArrayOf(7, 5)
    )

    val pawnAttacksWhite: Array<IntArray>
    val pawnAttacksBlack: Array<IntArray>
    val directionLookup: IntArray

    val kingAttackBitboards: ULongArray
    val knightAttackBitboards: ULongArray
    val pawnAttackBitboards: Array<ULongArray>

    val rookMoves: ULongArray
    val bishopMoves: ULongArray
    val queenMoves: ULongArray

    // Aka manhattan distance (answers how many moves for a rook to get from square a to square b)
    var orthogonalDistance: Array<IntArray>
    // Aka chebyshev distance (answers how many moves for a king to get from square a to square b)
    var kingDistance: Array<IntArray>
    var centreManhattanDistance: IntArray

    fun numRookMovesToReachSquare(startSquare: Int, targetSquare: Int): Int {
        return orthogonalDistance[startSquare][targetSquare]
    }

    fun numKingMovesToReachSquare(startSquare: Int, targetSquare: Int): Int {
        return kingDistance[startSquare][targetSquare]
    }

    // Initialize lookup data
    init {
        pawnAttacksWhite = Array(64) { IntArray(0) }
        pawnAttacksBlack = Array(64) { IntArray(0) }
        numSquaresToEdge = Array(64) { IntArray(0) }
        knightMoves = Array(64) { ByteArray(0) }
        kingMoves = Array(64) { ByteArray(0) }

        rookMoves = ULongArray(64)
        bishopMoves = ULongArray(64)
        queenMoves = ULongArray(64)

        // Calculate knight jumps and available squares for each square on the board.
        // See comments by variable definitions for more info.
        val allKnightJumps = intArrayOf(15, 17, -17, -15, 10, -6, 6, -10)
        knightAttackBitboards = ULongArray(64)
        kingAttackBitboards = ULongArray(64)
        pawnAttackBitboards = Array(64) { ULongArray(2) }

        for (squareIndex in 0 until 64) {
            val y = squareIndex / 8
            val x = squareIndex - y * 8

            val north = 7 - y
            val south = y
            val west = x
            val east = 7 - x
            numSquaresToEdge[squareIndex] = intArrayOf(north, south, west, east, minOf(north, west), minOf(south, east), minOf(north, east), minOf(south, west))

            // Calculate all squares knight can jump to from current square
            val legalKnightJumps = mutableListOf<Byte>()
            var knightBitboard: ULong = 0UL
            for (knightJumpDelta in allKnightJumps) {
                val knightJumpSquare = squareIndex + knightJumpDelta
                if (knightJumpSquare in 0 until 64) {
                    val knightSquareY = knightJumpSquare / 8
                    val knightSquareX = knightJumpSquare - knightSquareY * 8
                    // Ensure knight has moved max of 2 squares on x/y axis (to reject indices that have wrapped around side of board)
                    val maxCoordMoveDst = max(abs(x - knightSquareX), abs(y - knightSquareY))
                    if (maxCoordMoveDst == 2) {
                        legalKnightJumps.add(knightJumpSquare.toByte())
                        knightBitboard = knightBitboard or (1UL shl knightJumpSquare)
                    }
                }
            }
            knightMoves[squareIndex] = legalKnightJumps.toByteArray()
            knightAttackBitboards[squareIndex] = knightBitboard

            // Calculate all squares king can move to from current square (not including castling)
            val legalKingMoves = mutableListOf<Byte>()
            for (kingMoveDelta in directionOffsets) {
                val kingMoveSquare = squareIndex + kingMoveDelta
                if (kingMoveSquare in 0 until 64) {
                    val kingSquareY = kingMoveSquare / 8
                    val kingSquareX = kingMoveSquare - kingSquareY * 8
                    // Ensure king has moved max of 1 square on x/y axis (to reject indices that have wrapped around side of board)
                    val maxCoordMoveDst = max(abs(x - kingSquareX), abs(y - kingSquareY))
                    if (maxCoordMoveDst == 1) {
                        legalKingMoves.add(kingMoveSquare.toByte())
                        kingAttackBitboards[squareIndex] = kingAttackBitboards[squareIndex] or (1UL shl kingMoveSquare)
                    }
                }
            }
            kingMoves[squareIndex] = legalKingMoves.toByteArray()

            // Calculate legal pawn captures for white and black
            val pawnCapturesWhite = mutableListOf<Int>()
            val pawnCapturesBlack = mutableListOf<Int>()
            pawnAttackBitboards[squareIndex] = ULongArray(2)
            if (x > 0) {
                if (y < 7) {
                    pawnCapturesWhite.add(squareIndex + 7)
                    pawnAttackBitboards[squareIndex][Board.whiteIndex] = pawnAttackBitboards[squareIndex][Board.whiteIndex] or (1UL shl (squareIndex + 7))
                }
                if (y > 0) {
                    pawnCapturesBlack.add(squareIndex - 9)
                    pawnAttackBitboards[squareIndex][Board.blackIndex] = pawnAttackBitboards[squareIndex][Board.blackIndex] or (1UL shl (squareIndex - 9))
                }
            }
            if (x < 7) {
                if (y < 7) {
                    pawnCapturesWhite.add(squareIndex + 9)
                    pawnAttackBitboards[squareIndex][Board.whiteIndex] = pawnAttackBitboards[squareIndex][Board.whiteIndex] or (1UL shl (squareIndex + 9))
                }
                if (y > 0) {
                    pawnCapturesBlack.add(squareIndex - 7)
                    pawnAttackBitboards[squareIndex][Board.blackIndex] = pawnAttackBitboards[squareIndex][Board.blackIndex] or (1UL shl (squareIndex - 7))
                }
            }
            pawnAttacksWhite[squareIndex] = pawnCapturesWhite.toIntArray()
            pawnAttacksBlack[squareIndex] = pawnCapturesBlack.toIntArray()

            // Rook moves
            for (directionIndex in 0 until 4) {
                val currentDirOffset = directionOffsets[directionIndex]
                for (n in 0 until numSquaresToEdge[squareIndex][directionIndex]) {
                    val targetSquare = squareIndex + currentDirOffset * (n + 1)
                    rookMoves[squareIndex] = rookMoves[squareIndex] or (1UL shl targetSquare)
                }
            }
            // Bishop moves
            for (directionIndex in 4 until 8) {
                val currentDirOffset = directionOffsets[directionIndex]
                for (n in 0 until numSquaresToEdge[squareIndex][directionIndex]) {
                    val targetSquare = squareIndex + currentDirOffset * (n + 1)
                    bishopMoves[squareIndex] = bishopMoves[squareIndex] or (1UL shl targetSquare)
                }
            }
            queenMoves[squareIndex] = rookMoves[squareIndex] or bishopMoves[squareIndex]
        }

        directionLookup = IntArray(127)
        for (i in 0 until 127) {
            val offset = i - 63
            val absOffset = abs(offset)
            val absDir = when {
                absOffset % 9 == 0 -> 9
                absOffset % 8 == 0 -> 8
                absOffset % 7 == 0 -> 7
                else -> 1
            }
            directionLookup[i] = absDir * offset.sign
        }

        // Distance lookup
        orthogonalDistance = Array(64) { IntArray(64) }
        kingDistance = Array(64) { IntArray(64) }
        centreManhattanDistance = IntArray(64)
        for (squareA in 0 until 64) {
            val coordA = Coord(squareA)
            val fileDstFromCentre = max(3 - coordA.fileIndex, coordA.fileIndex - 4)
            val rankDstFromCentre = max(3 - coordA.rankIndex, coordA.rankIndex - 4)
            centreManhattanDistance[squareA] = fileDstFromCentre + rankDstFromCentre
            for (squareB in 0 until 64) {
                val coordB = Coord(squareB)
                val rankDistance = abs(coordA.rankIndex - coordB.rankIndex)
                val fileDistance = abs(coordA.fileIndex - coordB.fileIndex)
                orthogonalDistance[squareA][squareB] = fileDistance + rankDistance
                kingDistance[squareA][squareB] = max(fileDistance, rankDistance)
            }
        }

        alignMask = Array(64) { ULongArray(64) }
        for (squareA in 0 until 64) {
            for (squareB in 0 until 64) {
                val cA = Coord(squareA)
                val cB = Coord(squareB)
                val delta = cB - cA
                val dir = Coord(delta.fileIndex.sign, delta.rankIndex.sign)
                for (i in -8 until 8) {
                    val coord = Coord(squareA) + dir * i
                    if (coord.isValidSquare) {
                        alignMask[squareA][squareB] = alignMask[squareA][squareB] or (1UL shl (BoardHelper.indexFromCoord(coord)))
                    }
                }
            }
        }

        dirRayMask = Array(8) { ULongArray(64) }
        for (dirIndex in dirOffsets2D.indices) {
            for (squareIndex in 0 until 64) {
                val square = Coord(squareIndex)
                for (i in 0 until 8) {
                    val coord = square + dirOffsets2D[dirIndex] * i
                    if (coord.isValidSquare) {
                        dirRayMask[dirIndex][squareIndex] = dirRayMask[dirIndex][squareIndex] or (1UL shl (BoardHelper.indexFromCoord(coord)))
                    } else {
                        break
                    }
                }
            }
        }
    }
}
