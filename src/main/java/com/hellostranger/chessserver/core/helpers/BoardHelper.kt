package com.hellostranger.chessserver.core.helpers

import com.hellostranger.chessserver.core.board.Board
import com.hellostranger.chessserver.core.board.Coord
import com.hellostranger.chessserver.core.board.Piece
import com.hellostranger.chessserver.core.moveGeneration.bitboards.BitBoardUtility

object BoardHelper {
    const val a1: Int = 0
    const val b1: Int = 1
    const val c1: Int = 2
    const val d1: Int = 3
    const val e1: Int = 4
    const val f1: Int = 5
    const val g1: Int = 6
    const val h1: Int = 7

    const val a8: Int = 56
    const val b8: Int = 57
    const val c8: Int = 58
    const val d8: Int = 59
    const val e8: Int = 60
    const val f8: Int = 61
    const val g8: Int = 62
    const val h8: Int = 63

    val rookDirections: Array<Coord> = arrayOf(Coord(-1, 0), Coord(1, 0), Coord(0, 1), Coord(0, -1))
    val bishopDirections: Array<Coord> = arrayOf(Coord(-1, 1), Coord(1, 1), Coord(1, -1), Coord(-1, -1))
    const val fileNames: String = "abcdefgh"
    const val rankNames: String = "12345678"

    @JvmStatic
    fun rankIndex(squareIndex: Int): Int {
        return squareIndex shr 3
    }

    @JvmStatic
    fun fileIndex(squareIndex: Int): Int {
        return squareIndex and 7
    }

    @JvmStatic
    fun indexFromCoord(rankIndex: Int, fileIndex: Int): Int {
        return rankIndex * 8 + fileIndex
    }

    @JvmStatic
    fun indexFromCoord(coord: Coord): Int {
        return indexFromCoord(coord.rankIndex, coord.fileIndex)
    }

    @JvmStatic
    fun coordFromIndex(squareIndex: Int): Coord {
        return Coord(fileIndex(squareIndex), rankIndex(squareIndex))
    }

    @JvmStatic
    fun isLightSquare(fileIndex: Int, rankIndex: Int): Boolean {
        return (rankIndex + fileIndex) % 2 != 0
    }

    @JvmStatic
    fun isLightSquare(squareIndex: Int) : Boolean {
        return isLightSquare(rankIndex(squareIndex), fileIndex(squareIndex))
    }

    @JvmStatic
    fun isLightSquare(coord: Coord): Boolean {
        return isLightSquare(coord.fileIndex, coord.rankIndex)
    }

    @JvmStatic
    fun squareNameFromCoord(fileIndex: Int, rankIndex: Int): String {
        return fileNames[fileIndex].toString() + "" + (rankIndex + 1)
    }
    @JvmStatic
    fun squareNameFromIndex(squareIndex: Int): String {
        return squareNameFromCoord(Coord(squareIndex))
    }

    @JvmStatic
    fun squareNameFromCoord(coord: Coord): String {
        return squareNameFromCoord(coord.fileIndex, coord.rankIndex)
    }


    @JvmStatic
    fun squareIndexFromName(name: String): Int {
        val fileName = name[0]
        val rankName = name[1]
        val fileIndex = fileNames.indexOf(fileName)
        val rankIndex = rankNames.indexOf(rankName)
        return indexFromCoord(rankIndex, fileIndex)
    }

    @JvmStatic
    fun isValidCoord(x: Int, y: Int): Boolean {
        return (x >= 0) && (x <= 7) && (y >= 0) && (y <= 7)
    }

    // Creates an ascii diagrom of the bitboard
    @JvmStatic
    fun createBitboard(bitboard : ULong) : String {
        val result : StringBuilder = StringBuilder()
        for (y in 0 until 8) {
            val rankIndex = 7 - y
            result.appendLine("+---+---+---+---+---+---+---+---+")

            for (fileIndex in 0 until 8) {
                val squareIndex = indexFromCoord(rankIndex, fileIndex)

                if (BitBoardUtility.containsSquare(bitboard, squareIndex)) {
                    result.append("| P ")
                } else {
                    result.append("| _ ")
                }

                if (fileIndex == 7) {
                    result.appendLine("| ${rankIndex + 1}")
                }
            }

            if (y == 7) {
                result.appendLine("+---+---+---+---+---+---+---+---+")
                val fileNames = "  a   b   c   d   e   f   g   h  "
                result.appendLine(fileNames)
                result.appendLine()
            }
        }
        return result.toString()
    }

    /**
     * Creates an ASCII diagram of the current board.
     *
     */
    @JvmStatic
    fun createDiagram(
        board: Board,
        blackAtTop: Boolean = true,
        includeFen: Boolean = true,
        includeZobristKey: Boolean = true
    ): String {
        val result : StringBuilder = StringBuilder()
        val lastMoveSquare = if (board.allGameMoves.isNotEmpty()) board.allGameMoves.last().targetSquare else -1

        for (y in 0 until 8) {
            val rankIndex = if (blackAtTop) 7 - y else y
            result.appendLine("+---+---+---+---+---+---+---+---+")

            for (x in 0 until 8) {
                val fileIndex = if (blackAtTop) x else 7 - x
                val squareIndex = indexFromCoord(rankIndex, fileIndex)
                val highlight : Boolean = squareIndex == lastMoveSquare
                val piece = board.square[squareIndex]

                if (highlight) {
                    result.append("|(${Piece.getSymbol(piece)})")
                } else {
                    result.append("| ${Piece.getSymbol(piece)} ")
                }

                if (x == 7) {
                    result.appendLine("| ${rankIndex + 1}")
                }
            }

            if (y == 7) {
                result.appendLine("+---+---+---+---+---+---+---+---+")
                val fileNames = "  a   b   c   d   e   f   g   h  "
                val fileNamesReversed = "  h   g   f   e   d   c   b   a  "
                if (blackAtTop) {
                    result.appendLine(fileNames)
                } else {
                    result.appendLine(fileNamesReversed)
                }
                result.appendLine()
                if (includeFen) {
                    result.appendLine("Fen         : ${FenUtility.currentFen(board)}")
                }
                if (includeZobristKey) {
                    result.appendLine("Zobrist Key : ${board.zobristKey}")
                }
            }
        }

        return result.toString()
    }
}
