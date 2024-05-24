package com.hellostranger.chessserver.core.moveGeneration

import com.hellostranger.chessserver.core.board.Board
import com.hellostranger.chessserver.core.board.Move
import com.hellostranger.chessserver.core.board.Piece
import com.hellostranger.chessserver.core.helpers.BoardHelper
import com.hellostranger.chessserver.core.moveGeneration.PrecomputedMoveData.alignMask
import com.hellostranger.chessserver.core.moveGeneration.PrecomputedMoveData.dirRayMask
import com.hellostranger.chessserver.core.moveGeneration.PrecomputedMoveData.directionOffsets
import com.hellostranger.chessserver.core.moveGeneration.PrecomputedMoveData.numSquaresToEdge
import com.hellostranger.chessserver.core.moveGeneration.bitboards.BitBoardUtility
import com.hellostranger.chessserver.core.moveGeneration.bitboards.Bits

@ExperimentalUnsignedTypes
class MoveGenerator {
    companion object {
        const val MAX_MOVES = 218 // Max moves possible in a chess position
    }
    enum class PromotionMode {All, QueenOnly, QueenAndKnight}

    var promotionsToGenerate : PromotionMode = PromotionMode.All

    private var isWhiteToMove : Boolean = true
    private var friendlyColour     : Int = 0
    private var opponentColour     : Int = 0
    private var friendlyKingSquare : Int = 0
    private var friendlyIndex      : Int = 0
    private var enemyIndex         : Int = 0

    private var inCheck       : Boolean = false
    private var inDoubleCheck : Boolean = false

    // If in check, this bitboard contains squares in line from checking piece up to king
    // If not in check, all bits are set to 1
    private var checkRayBitmask : ULong = 0UL

    private var pinRays    : ULong = 0UL
    private var notPinRays : ULong = 0UL
    private var opponentAttackMapNoPawns = 0UL
    var opponentAttackMap     : ULong = 0UL
    var opponentPawnAttackMap : ULong = 0UL
    private var opponentSlidingAttackMap : ULong = 0UL

    var generateQuietMoves : Boolean = true
    lateinit var board : Board
    var currentMoveIndex : Int = 0

    var enemyPieces         : ULong = 0UL
    var friendlyPieces      : ULong = 0UL
    var allPieces           : ULong = 0UL
    var emptySquares        : ULong = 0UL
    var emptyOrEnemySquares : ULong = 0UL

    // If only captures should be generated, this will have 1s only in positions of enemy pieces.
    // Otherwise it will have 1s everywhere.
    var moveTypeMask : ULong = 0UL

    fun generateMoves(board: Board, capturesOnly : Boolean = false) : Array<Move> {
        val moves : Array<Move?> = arrayOfNulls(MAX_MOVES)
        return generateMoves(board, moves, capturesOnly)
    }

    fun generateMoves(board: Board, moves : Array<Move?>, capturesOnly : Boolean = false) : Array<Move> {
        this.board = board
        generateQuietMoves = !capturesOnly

        initialize()

        generateKingMoves(moves)

        // Only king moves can be valid in double check, so we can skip the rest
        if (!inDoubleCheck) {
            generateSlidingMoves(moves)
            generateKnightMoves(moves)
            generatePawnMoves(moves)
        }

        return moves.filterNotNull().toTypedArray()
    }

    fun inCheck() : Boolean {
        return inCheck
    }

    private fun initialize() {
        // Reset State
        currentMoveIndex = 0
        inCheck = false
        inDoubleCheck = false
        checkRayBitmask = 0UL
        pinRays = 0UL

        // Store some info for convenience
        isWhiteToMove = board.isWhiteToMove
        friendlyColour = board.moveColour
        opponentColour = board.opponentColour
        friendlyKingSquare = board.kingSquare[board.moveColourIndex]
        friendlyIndex = board.moveColourIndex
        enemyIndex = 1 - friendlyIndex

        // Store some bitboards
        enemyPieces = board.colourBitboards!![enemyIndex]
        friendlyPieces = board.colourBitboards!![friendlyIndex]
        allPieces = board.allPiecesBitboard
        emptySquares = allPieces.inv()
        emptyOrEnemySquares = emptySquares or enemyPieces
        moveTypeMask = if (generateQuietMoves) ULong.MAX_VALUE else enemyPieces

        calculateAttackData()
    }

    private fun generateKingMoves(moves: Array<Move?>) {
        val legalMask = (opponentAttackMap or friendlyPieces).inv()
        var kingMoves = BitBoardUtility.kingMoves[friendlyKingSquare] and legalMask and moveTypeMask
        while (kingMoves != 0UL) {
            val targetSquare : Int = BitBoardUtility.getLSB(kingMoves)
            kingMoves = BitBoardUtility.clearLSB(kingMoves)
            moves[currentMoveIndex++] = Move(friendlyKingSquare, targetSquare)
        }

        // Castling
        if (inCheck || !generateQuietMoves) {
            return
        }

        val castleBlockers = opponentAttackMap or board.allPiecesBitboard
        if (board.currentGameState.hasKingSideCastleRights(isWhiteToMove)) {
            val castleMask : ULong = if (isWhiteToMove) Bits.whiteKingsideMask else Bits.blackKingsideMask
            if ((castleMask and castleBlockers) == 0UL) {
                val targetSquare : Int = if (isWhiteToMove) BoardHelper.g1 else BoardHelper.g8
                moves[currentMoveIndex++] = Move(friendlyKingSquare, targetSquare, Move.CASTLE_FLAG)
            }
        }
        if (board.currentGameState.hasQueenSideCastleRights(isWhiteToMove)) {
            val castleMaskChecks : ULong = if (isWhiteToMove) Bits.whiteQueensideMaskChecks else Bits.blackQueensideMaskChecks
            val castleMaskBlocking : ULong = if (isWhiteToMove) Bits.whiteQueensideMask else Bits.blackQueensideMask
            if ((castleMaskChecks and castleBlockers) == 0UL && (castleMaskBlocking and board.allPiecesBitboard) == 0UL) {
                val targetSquare = if (isWhiteToMove) BoardHelper.c1 else BoardHelper.c8
                moves[currentMoveIndex++] = Move(friendlyKingSquare, targetSquare, Move.CASTLE_FLAG)
            }
        }
    }

    private fun generateSlidingMoves(moves: Array<Move?>) {
        // Limit movement to empty or enemy squares, and must block check if king is in check.
        val moveMask = emptyOrEnemySquares and checkRayBitmask and moveTypeMask

        var orthogonalSliders = board.friendlyOrthogonalSliders
        var diagonalSliders = board.friendlyDiagonalSliders

        // Pinned pieces cannot move if king is in check
        if (inCheck) {
            orthogonalSliders = orthogonalSliders and pinRays.inv()
            diagonalSliders = diagonalSliders and pinRays.inv()
        }
        // Ortho
        while (orthogonalSliders != 0UL) {
            val startSquare = BitBoardUtility.getLSB(orthogonalSliders)
            orthogonalSliders = BitBoardUtility.clearLSB(orthogonalSliders)
            var moveSquares = BitBoardUtility.getRookAttacks(startSquare, allPieces) and moveMask

            // When pinned, can only move along the pin ray
            if (isPinned(startSquare)) {
                moveSquares = moveSquares and alignMask[startSquare][friendlyKingSquare]
            }

            while (moveSquares != 0UL) {
                val targetSquare = BitBoardUtility.getLSB(moveSquares)
                moveSquares = BitBoardUtility.clearLSB(moveSquares)
                moves[currentMoveIndex++] = Move(startSquare, targetSquare)
            }
        }

        // Diagonal
        while (diagonalSliders != 0UL) {
            val startSquare = BitBoardUtility.getLSB(diagonalSliders)
            diagonalSliders = BitBoardUtility.clearLSB(diagonalSliders)
            var moveSquares = BitBoardUtility.getBishopAttacks(startSquare, allPieces) and moveMask

            // When pinned, can only move along the pin ray
            if (isPinned(startSquare)) {
                moveSquares = moveSquares and alignMask[startSquare][friendlyKingSquare]
            }

            while (moveSquares != 0UL) {
                val targetSquare = BitBoardUtility.getLSB(moveSquares)
                moveSquares = BitBoardUtility.clearLSB(moveSquares)
                moves[currentMoveIndex++] = Move(startSquare, targetSquare)
            }
        }

    }

    private fun generateKnightMoves(moves: Array<Move?>) {
        val friendlyKnightPiece = Piece.makePiece(Piece.KNIGHT, board.moveColour)
        var knights : ULong = board.pieceBitboards!![friendlyKnightPiece] and notPinRays
        val moveMask : ULong = emptyOrEnemySquares and checkRayBitmask and moveTypeMask

        while (knights != 0UL) {
            val knightSquare = BitBoardUtility.getLSB(knights)
            knights = BitBoardUtility.clearLSB(knights)
            var moveSquares : ULong = BitBoardUtility.knightAttacks[knightSquare] and moveMask

            while (moveSquares != 0UL) {
                val targetSquare = BitBoardUtility.getLSB(moveSquares)
                moveSquares = BitBoardUtility.clearLSB(moveSquares)
                moves[currentMoveIndex++] = Move(knightSquare, targetSquare)
            }
        }
    }

    private fun generatePawnMoves(moves: Array<Move?>) {
        val pushDir : Int = if (isWhiteToMove) 1 else -1
        val pushOffset : Int = pushDir * 8

        val friendlyPawnPiece = Piece.makePiece(Piece.PAWN, board.moveColour)
        val pawns = board.pieceBitboards!![friendlyPawnPiece]

        val promotionRankMask = if (isWhiteToMove) BitBoardUtility.rank8 else BitBoardUtility.rank1
        val singlePush = (BitBoardUtility.shift(pawns, pushOffset)) and emptySquares
        var pushPromotions = singlePush and promotionRankMask and checkRayBitmask

        val captureEdgeFileMask = if (isWhiteToMove) BitBoardUtility.notAFile else BitBoardUtility.notHFile
        val captureEdgeFileMask2 = if (isWhiteToMove) BitBoardUtility.notHFile else BitBoardUtility.notAFile
        var captureA = BitBoardUtility.shift(pawns and captureEdgeFileMask, pushDir * 7) and enemyPieces
        var captureB = BitBoardUtility.shift(pawns and captureEdgeFileMask2, pushDir * 9) and enemyPieces

        var singlePushNoPromotions = singlePush and promotionRankMask.inv() and checkRayBitmask

        var capturePromotionA = captureA and promotionRankMask and checkRayBitmask
        var capturePromotionB = captureB and promotionRankMask and checkRayBitmask

        captureA = captureA and checkRayBitmask and promotionRankMask.inv()
        captureB = captureB and checkRayBitmask and promotionRankMask.inv()

        // Single and double push
        if (generateQuietMoves) {
            // Generate single push
            while (singlePushNoPromotions != 0UL) {
                val targetSquare = BitBoardUtility.getLSB(singlePushNoPromotions)
                singlePushNoPromotions = BitBoardUtility.clearLSB(singlePushNoPromotions)
                val startSquare = targetSquare - pushOffset
                if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                    moves[currentMoveIndex++] = Move(startSquare, targetSquare)
                }
            }

            // Generate double pawn pushed
            val doublePushTargetRankMask = if (isWhiteToMove) BitBoardUtility.rank4 else BitBoardUtility.rank5
            var doublePush = BitBoardUtility.shift(singlePush, pushOffset) and emptySquares and doublePushTargetRankMask and checkRayBitmask

            while (doublePush != 0UL) {
                val targetSquare = BitBoardUtility.getLSB(doublePush)
                doublePush = BitBoardUtility.clearLSB(doublePush)
                val startSquare = targetSquare - pushOffset * 2
                if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                    moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PAWN_TWO_UP_FLAG)
                }
            }
        }

        // Captures
        while (captureA != 0UL) {
            val targetSquare = BitBoardUtility.getLSB(captureA)
            captureA = BitBoardUtility.clearLSB(captureA)
            val startSquare = targetSquare - pushDir * 7

            if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                moves[currentMoveIndex++] = Move(startSquare, targetSquare)
            }
        }

        while (captureB != 0UL) {
            val targetSquare = BitBoardUtility.getLSB(captureB)
            captureB = BitBoardUtility.clearLSB(captureB)
            val startSquare = targetSquare - pushDir * 9

            if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                moves[currentMoveIndex++] = Move(startSquare, targetSquare)
            }
        }

        // Promotions
        while (pushPromotions != 0UL) {
            val targetSquare = BitBoardUtility.getLSB(pushPromotions)
            pushPromotions = BitBoardUtility.clearLSB(pushPromotions)
            val startSquare = targetSquare - pushOffset

            if (!isPinned(startSquare)) {
                generatePromotions(startSquare, targetSquare, moves)
            }
        }
        while (capturePromotionA != 0UL) {
            val targetSquare = BitBoardUtility.getLSB(capturePromotionA)
            capturePromotionA = BitBoardUtility.clearLSB(capturePromotionA)
            val startSquare = targetSquare - pushDir * 7

            if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                generatePromotions(startSquare, targetSquare, moves)
            }
        }

        while (capturePromotionB != 0UL) {
            val targetSquare = BitBoardUtility.getLSB(capturePromotionB)
            capturePromotionB = BitBoardUtility.clearLSB(capturePromotionB)
            val startSquare = targetSquare - pushDir * 9

            if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                generatePromotions(startSquare, targetSquare, moves)
            }
        }

        // En passant
        if (board.currentGameState.enPassantFile <= 0) {
            return
        }
        val epFileIndex = board.currentGameState.enPassantFile - 1
        val epRankIndex = if(isWhiteToMove) 5 else 2
        val targetSquare = epRankIndex * 8 + epFileIndex
        val capturedPawnSquare = targetSquare - pushOffset
        if (!BitBoardUtility.containsSquare(checkRayBitmask, capturedPawnSquare)) {
            return
        }

        var pawnsThatCanCaptureEp = pawns and BitBoardUtility.pawnAttacks(1UL shl targetSquare, !isWhiteToMove)
        while (pawnsThatCanCaptureEp != 0UL) {
            val startSquare = BitBoardUtility.getLSB(pawnsThatCanCaptureEp)
            pawnsThatCanCaptureEp = BitBoardUtility.clearLSB(pawnsThatCanCaptureEp)
            if (!isPinned(startSquare) || alignMask[startSquare][friendlyKingSquare] == alignMask[targetSquare][friendlyKingSquare]) {
                if (!inCheckAfterEnPassant(startSquare, targetSquare, capturedPawnSquare)) {
                    moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.EN_PASSANT_CAPTURE_FLAG)
                }
            }
        }

    }

    private fun generatePromotions(startSquare: Int, targetSquare: Int, moves: Array<Move?>) {
        moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PROMOTE_TO_QUEEN_FLAG)
        // Don't generate non-queen moves in q-search
        if (generateQuietMoves) {
            if (promotionsToGenerate == PromotionMode.All) {
                moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PROMOTE_TO_KNIGHT_FLAG)
                moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PROMOTE_TO_ROOK_FLAG)
                moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PROMOTE_TO_BISHOP_FLAG)
            } else if (promotionsToGenerate == PromotionMode.QueenAndKnight) {
                moves[currentMoveIndex++] = Move(startSquare, targetSquare, Move.PROMOTE_TO_KNIGHT_FLAG)
            }
        }

    }

    private fun isPinned(square : Int) : Boolean {
        return ((pinRays shr square) and 1UL) != 0UL
    }
    private fun generateSlidingAttackMap() {
        opponentSlidingAttackMap = 0UL

        var pieceBoard : ULong = board.enemyOrthogonalSliders
        val blockers : ULong = board.allPiecesBitboard and (1UL shl friendlyKingSquare).inv()

        while (pieceBoard != 0UL) {
            val startSquare = BitBoardUtility.getLSB(pieceBoard)
            pieceBoard = BitBoardUtility.clearLSB(pieceBoard)
            val moveBoard : ULong = BitBoardUtility.getSliderAttacks(startSquare, blockers, true)

            opponentSlidingAttackMap = opponentSlidingAttackMap or moveBoard
        }

        pieceBoard = board.enemyDiagonalSliders
        while (pieceBoard != 0UL) {
            val startSquare = BitBoardUtility.getLSB(pieceBoard)
            pieceBoard = BitBoardUtility.clearLSB(pieceBoard)
            val moveBoard : ULong = BitBoardUtility.getSliderAttacks(startSquare, blockers, false)

            opponentSlidingAttackMap = opponentSlidingAttackMap or moveBoard
        }
    }

    private fun calculateAttackData() {
        generateSlidingAttackMap()
        // Search squares in all directions around friendly king for checks/pins by enemy sliding pieces (queen, rook, bishop)
        var startDirIndex = 0;
        var endDirIndex = 8

        if (board.queens[enemyIndex].count == 0) {
            startDirIndex = if (board.rooks[enemyIndex].count > 0) 0 else 4
            endDirIndex = if (board.bishops[enemyIndex].count > 0) 8 else 4
        }

        for (dir in startDirIndex until endDirIndex) {
            val isDiagonal : Boolean = dir > 3
            val slider : ULong = if (isDiagonal) board.enemyDiagonalSliders else board.enemyOrthogonalSliders
            if ((dirRayMask[dir][friendlyKingSquare] and slider) == 0UL) {
                continue
            }
            val n : Int = numSquaresToEdge[friendlyKingSquare][dir]
            val directionOffset : Int = directionOffsets[dir]
            var isFriendlyPieceAlongRay : Boolean = false
            var rayMask : ULong = 0UL

            for (i in 0 until n) {
                val squareIndex : Int = friendlyKingSquare + directionOffset * (i + 1)
                rayMask = rayMask or (1UL shl squareIndex)
                val piece : Int = board.square[squareIndex]

                if (piece == Piece.NONE) {
                    continue
                }

                if (Piece.isColour(piece, friendlyColour)) {
                    if (!isFriendlyPieceAlongRay) {
                        // First friendly piece we have come across in this direction, so it might be pinned
                        isFriendlyPieceAlongRay = true
                    } else {
                        // This is the second friendly piece we've found in this direction, therefore pin is not possible
                        break
                    }
                } else {
                    // This square contains an enemy piece
                    val pieceType : Int = Piece.pieceType(piece)
                    // Check if piece is in bitmask of pieces able to move in current direction
                    if ((isDiagonal && Piece.isDiagonalSlider(pieceType)) || (!isDiagonal && Piece.isOrthogonalSlider(pieceType))) {
                        if (isFriendlyPieceAlongRay) {
                            // Friendly piece blocks the check, so this is a pin
                            pinRays = pinRays or rayMask
                        } else {
                            // This is a check
                            checkRayBitmask = checkRayBitmask or rayMask
                            inDoubleCheck = inCheck // If already in check, then this is double check
                            inCheck = true
                        }
                        break
                    } else {
                        // This enemy pin in blocking any checks/pins in this direction
                        break
                    }
                }


            }
            if (inDoubleCheck) {
                break
            }
        }

        notPinRays = pinRays.inv()

        var opponentKnightAttacks : ULong = 0UL
        var knights : ULong = board.pieceBitboards!![Piece.makePiece(Piece.KNIGHT, board.opponentColour)]
        val friendlyKingBoard = board.pieceBitboards!![Piece.makePiece(Piece.KING, board.moveColour)]

        while (knights != 0UL) {
            val knightSquare : Int = BitBoardUtility.getLSB(knights)
            knights = BitBoardUtility.clearLSB(knights)
            val knightAttacks = BitBoardUtility.knightAttacks[knightSquare]
            opponentKnightAttacks = opponentKnightAttacks or knightAttacks

            if ((knightAttacks and friendlyKingBoard) != 0UL) {
                inDoubleCheck = inCheck
                inCheck = true
                checkRayBitmask = checkRayBitmask or (1UL shl knightSquare)
            }
        }

        // Pawns
        val opponentPawnsBoard : ULong = board.pieceBitboards!![Piece.makePiece(Piece.PAWN, board.opponentColour)]
        opponentPawnAttackMap = BitBoardUtility.pawnAttacks(opponentPawnsBoard, !isWhiteToMove)
        if (BitBoardUtility.containsSquare(opponentPawnAttackMap, friendlyKingSquare)) {
            inDoubleCheck = inCheck
            inCheck = true
            val possiblePawnAttackOrigins : ULong = if (isWhiteToMove) BitBoardUtility.whitePawnAttacks[friendlyKingSquare] else BitBoardUtility.blackPawnAttacks[friendlyKingSquare]
            val pawnCheckMap : ULong = opponentPawnsBoard and possiblePawnAttackOrigins
            checkRayBitmask = checkRayBitmask or pawnCheckMap
        }

        val enemyKingSquare : Int = board.kingSquare[enemyIndex]

        opponentAttackMapNoPawns = opponentSlidingAttackMap or opponentKnightAttacks or BitBoardUtility.kingMoves[enemyKingSquare]
        opponentAttackMap = opponentAttackMapNoPawns or opponentPawnAttackMap

        if (!inCheck) {
            checkRayBitmask = ULong.MAX_VALUE
        }
    }

    // Test if capturing a pawn with en-passant reveals a sliding piece attack against the king
    // Note: this is only used for cases where pawn appears to not be pinned due to opponent pawn being on same rank
    // (therefore only need to check orthogonal sliders)
    private fun inCheckAfterEnPassant(startSquare: Int, targetSquare: Int, epCaptureSquare : Int) : Boolean {
        val enemyOrtho : ULong = board.enemyOrthogonalSliders

        if (enemyOrtho != 0UL) {
            val maskedBlockers = (allPieces xor ((1UL shl epCaptureSquare) or (1UL shl startSquare) or (1UL shl targetSquare)))
            val rookAttacks = BitBoardUtility.getRookAttacks(friendlyKingSquare, maskedBlockers)
            return (rookAttacks and enemyOrtho) != 0UL
        }
        return false
    }

}