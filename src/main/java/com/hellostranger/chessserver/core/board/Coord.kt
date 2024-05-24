package com.hellostranger.chessserver.core.board

import com.hellostranger.chessserver.core.helpers.BoardHelper
import com.hellostranger.chessserver.core.helpers.BoardHelper.indexFromCoord

data class Coord(val fileIndex: Int, val rankIndex: Int) {

    constructor(squareIndex : Int) : this(BoardHelper.fileIndex(squareIndex), BoardHelper.rankIndex(squareIndex))

    val isLightSquare: Boolean
        get() = (this.rankIndex + this.fileIndex) % 2 != 0

    val isValidSquare: Boolean
        get() = fileIndex in 0..7 && rankIndex in 0..7

    val squareIndex: Int
        get() = indexFromCoord(this)

    operator fun plus(b : Coord) = Coord(this.fileIndex + b.fileIndex, this.rankIndex + b.rankIndex)
    operator fun minus(b : Coord) = Coord(this.fileIndex - b.fileIndex, this.rankIndex - b.rankIndex)
    operator fun times(m : Int) = Coord(this.fileIndex * m, this.rankIndex * m)

    override fun equals(other : Any?) : Boolean{
        if (other is Coord) {
            return other.squareIndex == this.squareIndex
        }
        return false
    }

    override fun hashCode(): Int {
        var result = fileIndex
        result = 31 * result + rankIndex
        return result
    }

}
