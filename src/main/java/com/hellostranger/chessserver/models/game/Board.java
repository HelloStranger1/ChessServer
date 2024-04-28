package com.hellostranger.chessserver.models.game;

import com.hellostranger.chessserver.exceptions.SquareNotFoundException;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import com.hellostranger.chessserver.models.game.pieces.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
@EqualsAndHashCode
@Slf4j
public class Board {

    private Square[][] squaresArray;

    private King whiteKing;

    private King blackKing;

    private Square phantomPawnSquare; //for en passant.

    @Override
    public String toString() {
        return "Board{" +
                "squaresArray=" + Arrays.toString(squaresArray) +
                '}';
    }

    public Board(){
        squaresArray = new Square[8][8];

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = new Square(col, row);
                square.setBoard(this);
                squaresArray[row][col] = square;
            }
        }
    }

    /**
     * returns the square object at the specified row and col.
     * @param col - The col of the square
     * @param row - the row of the square
     * @return The Square
     * @throws SquareNotFoundException - if the square is not within the bounds of the board.
     */
    public Square getSquareAt(int col, int row) throws SquareNotFoundException {
        if(row >= 8 || row < 0 || col >= 8 || col < 0){
            throw new SquareNotFoundException("Square not in Board");
        }
        return squaresArray[row][col];
    }

    public void promotePawnAt(Square pawnSquare, PieceType promotionType){
        Piece newPiece = null;
        Piece oldPawn = pawnSquare.getPiece();
        switch (promotionType) {
            case QUEEN -> {
                newPiece = new Queen(oldPawn.getColor(), pawnSquare, true);
            }
            case ROOK -> {
                newPiece = new Rook(oldPawn.getColor(), pawnSquare, true);
            } case BISHOP -> {
                newPiece = new Bishop(oldPawn.getColor(), pawnSquare, true);
            } case KNIGHT -> {
                newPiece = new Knight(oldPawn.getColor(), pawnSquare, true);
            }
            case PAWN, KING -> {
                log.error("can't promote pawn to a pawn/King");
            }
        }
        pawnSquare.setPiece(newPiece);
    }


    /**
     * Check if a move from the start square to the end would be a castling move
     * @param start - The start square
     * @param end - the end square
     * @return whether or not the move would be a castling move
     */

    public boolean isCastlingMove(Square start, Square end){
        Piece startPiece = start.getPiece();
        Piece endPiece = end.getPiece();
        if(startPiece == null || endPiece == null){
            return false;
        }
        if(startPiece.getHasMoved() || endPiece.getHasMoved()){
            return false;
        }
        if(endPiece.getPieceType() != PieceType.ROOK || startPiece.getPieceType() != PieceType.KING){
            return false;
        }
        if(endPiece.getColor() != startPiece.getColor()){
            return false;
        }
        log.info("move from square: "+ start + "to " + end + "is a castling move");
        return true;
    }

    /**
     * Check for the validity of the move
     * @param start - the start square for the move
     * @param end - the end square for the move
     * @return whether the move is valid
     */
    public boolean isValidMove(Square start, Square end){
        Piece movingPiece = start.getPiece();

        if(movingPiece == null){
            return false;
        }

        if(!canPieceMoveTo(movingPiece, end)){
            return false;
        }

        boolean isCastlingMove = isCastlingMove(start, end);
        boolean isFirstMove = !movingPiece.getHasMoved();
        Piece capturedPiece = end.getPiece();

        if(isCastlingMove){
            //For a castling move to be legal, the king can't be in check. first, we check that:
            if(isKingInCheck(movingPiece.getColor() == Color.WHITE)){
                return false;
            }
            //The king cannot move into a square where it would be checked while performing the castle, so we check for that.
            if(end.getColIndex() > start.getColIndex() && !isValidMove(start, squaresArray[start.getRowIndex()][start.getColIndex() + 1])){
                //O-O
                return false;
            } else if(end.getColIndex() < start.getColIndex() && !isValidMove(start, squaresArray[start.getRowIndex()][start.getColIndex() - 1])) {
                //O-O-O
                return false;
            }
            // We check the path is clear
            int row = start.getRowIndex();
            int col = start.getColIndex();
            if (end.getColIndex() > col) {
                // O-O
                while (col < 7) {
                    if (squaresArray[row][col].getPiece() != null) {
                        log.info("King can't castle because we have a piece on (row, col) [" + row + "][" + col + "]. The piece is: " + squaresArray[row][col].getPiece());
                        return false;
                    }
                    col++;
                }
            } else {
                // O-O-O
                while (col > 0) {
                    if (squaresArray[row][col].getPiece() != null) {
                        log.info("King can't castle because we have a piece on (row, col) [" + row + "][" + col + "]. The piece is: " + squaresArray[row][col].getPiece());
                        return false;
                    }
                    col--;
                }
            }
            makeCastlingMove(start, end);
        }else{
            movePieceTemp(start, end);
        }

        boolean isLegalMove = !isKingInCheck(movingPiece.getColor() == Color.WHITE);
        if(isCastlingMove){
            undoCastlingMove(start, end);
        }
        else {
            movePieceTemp(end, start);
            end.setPiece(capturedPiece);
        }
        if(isFirstMove){
            movingPiece.setHasMoved(false);
        }


        return isLegalMove;
    }

    private boolean canPieceMoveTo(Piece piece, Square square) {
        boolean result = false;
        for (Square movebleSquare : piece.getMovableSquares(this)) {
            if (movebleSquare.equals(square)) {
                result = true;
                break;
            }
        }
        return result;
    }
    private boolean canPieceThreatenSquare(Piece piece, Square square) {
        boolean result = false;
        for (Square movebleSquare : piece.getThreatenedSquares(this)) {
            if (movebleSquare.equals(square)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Check if the player has a move
     * @param playerColor The color of the player we want to test
     * @return True - if the player has a move, false otherwise.
     */
    public boolean canPlayerPlay(Color playerColor){
        log.info("checking if player: "+ playerColor + "can play");
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                Square currentSquare = squaresArray[row][col];
                Piece currentPiece = currentSquare.getPiece();
                if(currentPiece == null || currentPiece.getColor() != playerColor){
                    continue;
                }
                if(doesPieceHaveAMove(currentPiece, currentSquare)){
                    return true;
                }
            }
        }
        return false;
    }
    private boolean doesPieceHaveAMove(Piece piece, Square startSquare){
        for(Square targetSquare : piece.getMovableSquares(this)){
            if(isValidMove(startSquare, targetSquare)){
                log.info("The piece on square: " + startSquare + "  can move to the square: " + targetSquare);
                return true;
            }
        }
        return false;
    }


    /**
     * Check if the king of the provided color is in check
     * @param isWhite The color of the king
     * @return True: if the king is in check, false otherwise
     */

    public boolean isKingInCheck(boolean isWhite) {
        Square kingSquare;
        try{
            if(isWhite){
                kingSquare = getSquareAt(whiteKing.getColIndex(), whiteKing.getRowIndex());
            } else{
                kingSquare = getSquareAt(blackKing.getColIndex(), blackKing.getRowIndex());
            }
        } catch (SquareNotFoundException e) {
            return false;
        }

        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                Square currentSquare = squaresArray[row][col];
                Piece piece = currentSquare.getPiece();
                if(piece == null || isWhite == (piece.getColor() == Color.WHITE)){
                    continue;
                }
                if (canPieceThreatenSquare(piece, kingSquare)) {
                    log.info("\n \n isKingInCheck king is in check. by piece: \n" + piece + "\n \n");
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Performs a move. this function doesn't validate the move, simply plays it.
     */
    public void movePiece(Square start, Square end){
        //this function does not care if the move is legal. just makes it (assumes piece at start isn't null).
        Piece movingPiece = start.getPiece();
        if(movingPiece.getPieceType() == PieceType.PAWN && start.getColIndex() != end.getColIndex()){
            if(end.getPiece() == null){
                squaresArray[start.getRowIndex()][end.getColIndex()].setPiece(null);
            }
        }

        movePieceTemp(start, end);
        if(movingPiece.getPieceType() == PieceType.PAWN && Math.abs(start.getRowIndex() - end.getRowIndex()) == 2){
            phantomPawnSquare = squaresArray[(start.getRowIndex() + end.getRowIndex())/2][start.getColIndex()];
        }else{
            phantomPawnSquare = null;
        }
    }

    /**
     * A simpler version of movePiece, meant to temporarily check something. ignores en-peasant.
     */
    public void movePieceTemp(Square start, Square end){
        //this function does not care if the move is legal. just makes it (assumes piece at start isn't null).
        Piece movingPiece = start.getPiece();
        movingPiece.setHasMoved(true);
        start.setPiece(null);
        end.setPiece(movingPiece);
        movingPiece.move(end);
    }

    /**
     * play the move necessary for castle
     */
    public void makeCastlingMove(Square start, Square end){
        //doesn't care if the move is legal. start -> king, end -> rook
        if(end.getColIndex() > start.getColIndex()){
            //O-O
            movePiece(start, squaresArray[end.getRowIndex()][end.getColIndex()-1]);
            movePiece(end, squaresArray[start.getRowIndex()][start.getColIndex()+1]);
        }else{
            //O-O-O
            movePiece(start, squaresArray[start.getRowIndex()][start.getColIndex()-2]);
            movePiece(end, squaresArray[start.getRowIndex()][start.getColIndex()-1]);
        }
    }

    public void undoCastlingMove(Square start, Square end){
        //doesn't care if the move is legal. start -> king, end -> rook
        if(end.getColIndex() > start.getColIndex()){
            //O-O
            movePiece(squaresArray[end.getRowIndex()][end.getColIndex()-1],start);
            movePiece( squaresArray[start.getRowIndex()][start.getColIndex()+1],end);
        }else{
            //O-O-O
            movePiece(squaresArray[start.getRowIndex()][start.getColIndex()-2], start);
            movePiece(squaresArray[start.getRowIndex()][start.getColIndex()-1], end);
        }
        squaresArray[start.getRowIndex()][start.getColIndex()].getPiece().setHasMoved(false);
        squaresArray[end.getRowIndex()][end.getColIndex()].getPiece().setHasMoved(false);
    }



    public void setPieceAt(int col, int row, Piece piece){
        Square square = squaresArray[row][col];
        square.setPiece(piece);

    }
    public void setBoardWithStandardPieces(){
        PieceFactory pieceFactory = new PieceFactory();
        Piece currentPiece;
        for (int col = 0; col < 8; col++){
            currentPiece = pieceFactory.getPiece(PieceType.PAWN, Color.WHITE, squaresArray[1][col]);
            setPieceAt(col, 1, currentPiece);

            currentPiece = pieceFactory.getPiece(PieceType.PAWN, Color.BLACK, squaresArray[6][col]);
            setPieceAt(col, 6, currentPiece);
        }

        currentPiece = pieceFactory.getPiece(PieceType.ROOK,Color.WHITE, squaresArray[0][0]);
        setPieceAt(0, 0, currentPiece);
        currentPiece = pieceFactory.getPiece(PieceType.ROOK,Color.WHITE,  squaresArray[0][7]);
        setPieceAt(7, 0, currentPiece);

        currentPiece = pieceFactory.getPiece(PieceType.ROOK, Color.BLACK,  squaresArray[7][0]);
        setPieceAt(0, 7, currentPiece);
        currentPiece = pieceFactory.getPiece( PieceType.ROOK, Color.BLACK, squaresArray[7][7]);
        setPieceAt(7, 7, currentPiece);

        currentPiece = pieceFactory.getPiece( PieceType.KNIGHT, Color.WHITE, squaresArray[0][1]);
        setPieceAt(1, 0, currentPiece);
        currentPiece = pieceFactory.getPiece( PieceType.KNIGHT, Color.WHITE, squaresArray[0][6]);
        setPieceAt(6, 0, currentPiece);

        currentPiece = pieceFactory.getPiece( PieceType.KNIGHT, Color.BLACK, squaresArray[7][1]);
        setPieceAt(1, 7, currentPiece);
        currentPiece = pieceFactory.getPiece( PieceType.KNIGHT, Color.BLACK, squaresArray[7][6]);
        setPieceAt(6, 7, currentPiece);

        currentPiece = pieceFactory.getPiece( PieceType.BISHOP, Color.WHITE, squaresArray[0][2]);
        setPieceAt(2, 0, currentPiece);
        currentPiece = pieceFactory.getPiece( PieceType.BISHOP, Color.WHITE, squaresArray[0][5]);
        setPieceAt(5, 0, currentPiece);

        currentPiece = pieceFactory.getPiece( PieceType.BISHOP, Color.BLACK, squaresArray[7][2]);
        setPieceAt(2, 7, currentPiece);
        currentPiece = pieceFactory.getPiece( PieceType.BISHOP, Color.BLACK, squaresArray[7][5]);
        setPieceAt(5, 7, currentPiece);


        currentPiece = pieceFactory.getPiece( PieceType.KING, Color.WHITE, squaresArray[0][4]);
        setPieceAt(4, 0, currentPiece);
        whiteKing = (King) currentPiece;

        currentPiece = pieceFactory.getPiece( PieceType.QUEEN, Color.WHITE, squaresArray[0][3]);
        setPieceAt(3, 0, currentPiece);

        currentPiece = pieceFactory.getPiece( PieceType.KING, Color.BLACK, squaresArray[7][4]);
        setPieceAt(4, 7, currentPiece);
        blackKing = (King) currentPiece;

        currentPiece = pieceFactory.getPiece( PieceType.QUEEN, Color.BLACK, squaresArray[7][3]);
        setPieceAt(3, 7, currentPiece);
    }

    public Square[] beamSearchThreat(int startRow, int startCol, Color color, int incrementCol, int incrementRow){
        ArrayList<Square> threatenedSquares = new ArrayList<>();
        int curRow = startRow + incrementRow;
        int curCol = startCol + incrementCol;
        while(curCol >= 0 && curRow >= 0 && curCol <= 7 && curRow <=7){
            Square curSquare = squaresArray[curRow][curCol];
            Piece curPiece = curSquare.getPiece();
            if(curPiece != null){
                if(curPiece.getColor() != color){
                    threatenedSquares.add(curSquare);
                }
                break;
            }
            threatenedSquares.add(curSquare);
            curCol += incrementCol;
            curRow += incrementRow;
        }
        Square[] sqrArr = new Square[threatenedSquares.size()];
        return threatenedSquares.toArray(sqrArr);
    }
    public Square pawnSpotSearchThreat(int startRow, int startCol, Color color, int incrementCol, int incrementRow, Boolean isAttacking){
        int curRow = startRow + incrementRow;
        int curCol = startCol + incrementCol;
        if(curRow >= 8 || curCol >= 8 || curRow < 0 || curCol < 0){
            return null;
        }
        Square curSquare = squaresArray[curRow][curCol];
        Piece curPiece = curSquare.getPiece();
        if(isAttacking && curPiece == null && phantomPawnSquare == curSquare){
            return curSquare;
        }
        if(curPiece != null){
            if(!isAttacking){
                return null;
            }
            if(curPiece.getColor() != color){
                return curSquare;
            }else{
                return null;
            }
        }
        if(!isAttacking){
            return curSquare;
        }
        return null;
    }
    public Square spotSearchThreat(int startRow, int startCol, Color color, int incrementCol, int incrementRow) {
        int curRow = startRow + incrementRow;
        int curCol = startCol + incrementCol;
        if(curRow >= 8 || curCol >= 8 || curRow < 0 || curCol < 0){
            return null;
        }
        Square curSquare = squaresArray[curRow][curCol];
        Piece curPiece = curSquare.getPiece();
        if(curPiece != null){
            if(curPiece.getColor() != color){
                return curSquare;
            }else{
                return null;
            }
        }
        return curSquare;
    }

    /**
     * used to print the board to the console, for testing
     * @return a string representation of the board
     */
    public String toStringPretty() {
        StringBuilder desc = new StringBuilder(" \n");
        int i;
        for (i = 7; i >= 0; i--) {
            desc.append(i);
            desc.append(" ");
            int j;
            for (j = 0; j <= 7; j++) {

                Piece pieceAt = squaresArray[i][j].getPiece();
                if (pieceAt == null) {
                    desc.append(". ");
                } else {
                    boolean isWhite = pieceAt.getColor() == Color.WHITE;
                    if (pieceAt.getPieceType() == PieceType.KING) {
                        desc.append(isWhite ? "k " : "K ");
                    } else if (pieceAt.getPieceType() == PieceType.QUEEN) {
                        desc.append(isWhite ? "q " : "Q ");
                    } else if (pieceAt.getPieceType() == PieceType.ROOK) {
                        desc.append(isWhite ? "r " : "R ");
                    } else if (pieceAt.getPieceType() == PieceType.BISHOP) {
                        desc.append(isWhite ? "b " : "B ");
                    } else if (pieceAt.getPieceType() == PieceType.KNIGHT) {
                        desc.append(isWhite ? "n " : "N ");
                    } else if (pieceAt.getPieceType() == PieceType.PAWN) {
                        desc.append(isWhite ? "p " : "P ");
                    }
                }
            }

            desc.append("\n");
        }

        desc.append("\n And phantom square is: ").append(phantomPawnSquare);
        return desc.toString();
    }

}
