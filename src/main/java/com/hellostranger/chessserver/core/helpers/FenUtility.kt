package com.hellostranger.chessserver.core.helpers

import com.hellostranger.chessserver.core.board.Board
import com.hellostranger.chessserver.core.board.Coord
import com.hellostranger.chessserver.core.board.Move
import com.hellostranger.chessserver.core.board.Piece

object FenUtility {
    const val startPositionFEN: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @JvmStatic
    fun positionFromFen(fen: String): PositionInfo {
        return PositionInfo(fen)
    }

    @JvmStatic
    fun currentFen(board: Board, alwaysIncludeEPSquare: Boolean = true): String {
        val fen = StringBuilder()
        for (rank in 7 downTo 0) {
            var numEmptyTiles = 0
            for (file in 0..7) {
                val i = rank * 8 + file
                val piece = board.square[i]
                if (piece != 0) {
                    if (numEmptyTiles != 0) {
                        fen.append(numEmptyTiles)
                        numEmptyTiles = 0
                    }
                    val isBlack = Piece.isColour(piece, Piece.BLACK)
                    val pieceType = Piece.pieceType(piece)
                    val pieceChar = Piece.getSymbol(pieceType)
                    fen.append(if ((isBlack)) pieceChar.lowercaseChar() else pieceChar)
                } else {
                    numEmptyTiles++
                }
            }
            if (numEmptyTiles != 0) {
                fen.append(numEmptyTiles)
            }
            if (rank != 0) {
                fen.append('/')
            }
        }

        // Side to move
        fen.append(' ')
        fen.append(if (board.isWhiteToMove) 'w' else 'b')

        // Castling
        val whiteKingside = board.currentGameState.hasKingSideCastleRights(true)
        val whiteQueenside = board.currentGameState.hasQueenSideCastleRights(true)
        val blackKingside = board.currentGameState.hasKingSideCastleRights(false)
        val blackQueenside = board.currentGameState.hasQueenSideCastleRights(false)
        fen.append(' ')
        fen.append(if (whiteKingside) 'K' else "")
        fen.append(if (whiteQueenside) 'Q' else "")
        fen.append(if (blackKingside) 'k' else "")
        fen.append(if (blackQueenside) 'q' else "")
        fen.append(if ((board.currentGameState.castlingRights == 0)) "-" else "")

        // En-passant
        fen.append(" ")
        val epFileIndex = board.currentGameState.enPassantFile
        val epRankIndex = if ((board.isWhiteToMove)) 5 else 2

        val isEnPassant = epFileIndex != -1
        val includeEP = alwaysIncludeEPSquare || enPassantCanBeCaptured(epFileIndex, epRankIndex, board)
        if (isEnPassant && includeEP) {
            fen.append(BoardHelper.squareNameFromCoord(epFileIndex, epRankIndex))
        } else {
            fen.append('-')
        }

        // 50 Move counter
        fen.append(' ')
        fen.append(board.currentGameState.fiftyMoveCounter)

        fen.append(' ')
        fen.append((board.plyCount / 2) + 1)

        return fen.toString()
    }

    @JvmStatic
    fun enPassantCanBeCaptured(epFileIndex: Int, epRankIndex: Int, board: Board): Boolean {
        val captureFromA = Coord(epFileIndex - 1, epRankIndex + if(board.isWhiteToMove) -1 else 1)
        val captureFromB = Coord(epFileIndex + 1, epRankIndex + if(board.isWhiteToMove) -1 else 1)
        val epCaptureSquare = Coord(epFileIndex, epRankIndex).squareIndex
        val friendlyPawn = Piece.makePiece(Piece.PAWN, board.moveColour)

        var canCapture = false
        // from A
        var isPawnOnSquare = board.square[captureFromA.squareIndex] == friendlyPawn
        if (captureFromA.isValidSquare && isPawnOnSquare) {
            val move = Move(captureFromA.squareIndex, epCaptureSquare, Move.EN_PASSANT_CAPTURE_FLAG)
            board.makeMove(move)
            board.makeNullMove()
            val wasLegalMove = !board.calculateInCheckState()

            board.unmakeNullMove()
            board.unmakeMove(move)
            if (wasLegalMove) return true
        }
        isPawnOnSquare = board.square[captureFromB.squareIndex] == friendlyPawn
        if (captureFromB.isValidSquare && isPawnOnSquare) {
            val move = Move(captureFromB.squareIndex, epCaptureSquare, Move.EN_PASSANT_CAPTURE_FLAG)
            board.makeMove(move)
            board.makeNullMove()
            canCapture =  !board.calculateInCheckState()
            board.unmakeNullMove()
            board.unmakeMove(move)
        }
        return canCapture

    }

    class PositionInfo(fen: String) {
        val fen: String
        val squares: IntArray

        // Castling rights
        val whiteCastleKingside: Boolean
        val whiteCastleQueenside: Boolean
        val blackCastleKingside: Boolean
        val blackCastleQueenside: Boolean

        // En passant file (1 is file a)
        val epFile: Int
        val whiteToMove: Boolean
        val fiftyMovePlyCount: Int
        val moveCount: Int

        init {
            var moveCount: Int
            var epFile: Int
            var fiftyMovePlyCount: Int
            this.fen = fen
            val squarePieces = IntArray(64)

            val sections = fen.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var file = 0
            var rank = 7

            for (i in 0 until sections[0].length) {
                val symbol = sections[0][i]

                if (symbol == '/') {
                    file = 0
                    rank--
                } else {
                    if (Character.isDigit(symbol)) {
                        file += Character.getNumericValue(symbol)
                    } else {
                        val pieceColour = if ((Character.isUpperCase(symbol))) Piece.WHITE else Piece.BLACK
                        val pieceType = Piece.getPieceFromSymbol(symbol)

                        squarePieces[rank * 8 + file] = Piece.makePiece(pieceType, pieceColour)
                        file++
                    }
                }
            }

            this.squares = squarePieces

            this.whiteToMove = (sections[1] == "w")

            val castlingRights = sections[2]
            whiteCastleKingside = castlingRights.contains("K")
            whiteCastleQueenside = castlingRights.contains("Q")
            blackCastleKingside = castlingRights.contains("k")
            blackCastleQueenside = castlingRights.contains("q")

            // Default values
            epFile = 0
            fiftyMovePlyCount = 0
            moveCount = 0

            if (sections.size > 3) {
                val enPassantFileName = sections[3][0].toString()

                if (BoardHelper.fileNames.contains(enPassantFileName)) {
                    epFile = BoardHelper.fileNames.indexOf(enPassantFileName) + 1
                }
            }

            if (sections.size > 4) {
                try {
                    fiftyMovePlyCount = sections[4].toInt()
                } catch (ignored: NumberFormatException) {
                }
            }
            if (sections.size > 5) {
                try {
                    moveCount = sections[5].toInt()
                } catch (ignored: NumberFormatException) {
                }
            }

            this.epFile = epFile
            this.fiftyMovePlyCount = fiftyMovePlyCount
            this.moveCount = moveCount
        }
    }
}
