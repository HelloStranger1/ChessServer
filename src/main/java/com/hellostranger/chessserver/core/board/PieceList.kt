package com.hellostranger.chessserver.core.board

class PieceList(maxPieces: Int = 16) {
    // Indices of squares occupied by given piece (Only elements up to count are not garbage)
    private var occupiedSquares: IntArray = IntArray(maxPieces)

    // Map to go from index of a square to the index of the square in occupiedSquares
    private var map: IntArray = IntArray(64)
    private var numPieces: Int = 0

    val count : Int
        get() = numPieces

    operator fun get(index : Int) = occupiedSquares[index]


    fun addPieceAtSquare(square: Int) {
        occupiedSquares[numPieces] = square
        map[square] = numPieces
        numPieces++
    }

    fun removePieceAtSquare(square: Int) {
        val pieceIndex = map[square]
        occupiedSquares[pieceIndex] = occupiedSquares[numPieces - 1]
        map[occupiedSquares[pieceIndex]] = pieceIndex
        numPieces--
    }

    fun movePiece(startSquare: Int, targetSquare: Int) {
        val pieceIndex = map[startSquare]
        occupiedSquares[pieceIndex] = targetSquare
        map[targetSquare] = pieceIndex
    }
}
