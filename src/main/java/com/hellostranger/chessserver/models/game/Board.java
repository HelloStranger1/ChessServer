package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.exceptions.InvalidMoveException;
import com.hellostranger.chessserver.exceptions.SquareNotFoundException;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.PieceType;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
@Slf4j
public class Board {

    private Square[][] squaresArray;

    @JsonIgnore
    private Piece whiteKing;

    @JsonIgnore
    private Piece blackKing;


    private Square phantomPawnSquare; //for en passant.

    @Override
    public String toString() {
        return "Board{" +
                "squaresArray=" + Arrays.toString(squaresArray) +
                ", phantomPawnSquare=" + phantomPawnSquare +
                '}';
    }

    public Board(){
        squaresArray = new Square[8][8];
        initializeBoard();

        setBoard();
    }
    private void initializeBoard() {
        // Initialize the squares and set the associated board for each square
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = new Square(col, row);
                square.setBoard(this);
                squaresArray[row][col] = square;
            }
        }

        // Set up the initial pieces on the board...
    }

    public Square getSquareAt(int col, int row) throws SquareNotFoundException {
        if(row >= 8 || row < 0 || col >= 8 || col < 0){
            throw new SquareNotFoundException("Square not in Board");
        }
        return squaresArray[row][col];
    }

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
    public boolean isValidMove(Square start, Square end) throws InvalidMoveException {
        //assumes there is a piece at square start
        Piece movingPiece = start.getPiece();

        if(!movingPiece.canMakeMove(start,end)){
            throw new InvalidMoveException("Piece can't make this move");
        }
        boolean isCastlingMove = isCastlingMove(start, end);
        if(isPieceBlocked(movingPiece, start, end, isCastlingMove)){
            throw new InvalidMoveException("Piece can't make this move, its blocked");
        }

        Piece capturedPiece = end.getPiece();
        boolean isFirstMove = !movingPiece.getHasMoved();
        if(isCastlingMove){
            makeCastlingMove(start, end);
        }else{
            movePieceTemp(start, end);
        }

        boolean isLegalMove = true;
        try{
            isLegalMove = !isKingInCheck(movingPiece.getColor() == Color.WHITE);
        } catch (SquareNotFoundException e){
            log.info("isValidMove error: " + e.getMsg());
        }
        if(isCastlingMove){
            undoCastlingMove(start, end);
        }else{
            movePieceTemp(end, start);
            end.setPiece(capturedPiece);
            if(isFirstMove){
                movingPiece.setHasMoved(false);
            }
        }

        return isLegalMove;
    }

    public boolean canPlayerPlay(Color playerColor){
        log.info("checking if player: "+ playerColor + "can play");
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                Square currentSquare = squaresArray[row][col];
                if(currentSquare.getPiece() != null && currentSquare.getPiece().getColor() == playerColor){
                    switch (currentSquare.getPiece().getPieceType()){
                        case PAWN -> {
                            if(playerColor == Color.BLACK){
                                if(doesBlackPawnHaveAMove(currentSquare)){ return true; }
                            } else{
                                if(doesWhitePawnHaveAMove(currentSquare)){ return true; }
                            }
                        }
                        case KNIGHT -> {
                            if(doesKnightHaveAMove(currentSquare)) { return true; }
                        }
                        case BISHOP -> {
                            if(doesBishopHaveAMove(currentSquare)){ return true; }
                        }
                        case ROOK -> {
                            if(doesRookHaveAMove(currentSquare)){ return true; }
                        }
                        case QUEEN -> {
                            if(doesQueenHaveAMove(currentSquare)){ return true; }
                        }
                        case KING -> {
                            if(doesKingHaveAMove(currentSquare)){ return true; }
                        }
                    }
                }
            }
        }
        return false;
    }
    private boolean doesBlackPawnHaveAMove(Square currentSquare) {
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        if(row - 2 >= 0){
            try{
                if(isValidMove(currentSquare, squaresArray[row-2][col])){
                    log.info("A black pawn on square " + currentSquare + "can move forward 2 squares");
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

        }
        if (row - 1 >= 0) {
            try{
                if(isValidMove(currentSquare, squaresArray[row-1][col])){
                    log.info("A black pawn on square " + currentSquare + "can move forward");
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
            try{
                if(col + 1 < 8 && isValidMove(currentSquare, squaresArray[row-1][col+1])){
                    log.info("A black pawn on square " + currentSquare + "can move capture to the right");
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
            try{
                if(col - 1 >= 0 && isValidMove(currentSquare, squaresArray[row-1][col-1])){
                    log.info("A black pawn on square " + currentSquare + "can move capture to the left");
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
        }
        return false;
    }

    private boolean doesWhitePawnHaveAMove(Square currentSquare){
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        if(row + 2 < 8){
            try{
                if(isValidMove(currentSquare, squaresArray[row+2][col])){
                    log.info("A white pawn on square " + currentSquare + "can move forward 2");
                    return true;
                }
            }catch (InvalidMoveException ignored){}

        }
        if (row + 1 < 8) {
            try{
                if(isValidMove(currentSquare, squaresArray[row+1][col])){
                    log.info("A white pawn on square " + currentSquare + "can move forward");
                    return true;
                }
            }catch (InvalidMoveException ignored){}
            try{
                if(col + 1 < 8 && isValidMove(currentSquare, squaresArray[row+1][col+1])){
                    log.info("A white pawn on square " + currentSquare + "can move capture to the left");
                    return true;
                }
            }catch (InvalidMoveException ignored){}
            try{
                if(col - 1 >= 0 && isValidMove(currentSquare, squaresArray[row+1][col-1])){
                    log.info("A white pawn on square " + currentSquare + "can move capture to the left");
                    return true;
                }
            }catch (InvalidMoveException ignored){}

        }
        return false;
    }
    private boolean doesKnightHaveAMove(Square currentSquare) {
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        if(row + 1 < 8){
            try{
                if(col + 2 < 8 && isValidMove(currentSquare, squaresArray[row + 1][col + 2])) {
                    log.info("knight can move from1 " + currentSquare + "to " + squaresArray[row + 1][col + 2]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
            try{
                if(col - 2 > 0 && isValidMove(currentSquare, squaresArray[row + 1][col - 2])){
                    log.info("knight can move from2 " + currentSquare + "to " + squaresArray[row + 1][col - 2]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
        }
        if(row - 1 > 0){
            try{
                if(col + 2 < 8 && isValidMove(currentSquare, squaresArray[row - 1][col + 2])) {
                    log.info("knight can move from3 " + currentSquare + "to " + squaresArray[row - 1][col + 2]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}
            try{
                if(col - 2 > 0 && isValidMove(currentSquare, squaresArray[row - 1][col - 2])){
                    log.info("knight can move from4 " + currentSquare + "to " + squaresArray[row - 1][col - 2]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

        }
        if(row + 2 < 8){
            try{
                if(col + 2 < 8 && isValidMove(currentSquare, squaresArray[row + 2][col + 1])) {
                    log.info("knight can move from5 " + currentSquare + "to " + squaresArray[row + 2][col + 1]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

            try{
                if(col - 2 > 0 && isValidMove(currentSquare, squaresArray[row + 2][col - 1])){
                    log.info("knight can move from6 " + currentSquare + "to " + squaresArray[row + 2][col - 1]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

        }
        if(row - 2 > 0){
            try{
                if(col + 2 < 8 && isValidMove(currentSquare, squaresArray[row - 1][col + 1])) {
                    log.info("knight can move from7 " + currentSquare + "to " + squaresArray[row - 1][col + 1]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

            try{
                if(col - 2 > 0 && isValidMove(currentSquare, squaresArray[row - 1][col - 1])){
                    log.info("knight can move from8 " + currentSquare + "to " + squaresArray[row - 1][col - 1]);
                    return true;
                }
            }catch (InvalidMoveException ignored) {}

        }

        return false;
    }
    private boolean doesBishopHaveAMove(Square currentSquare) {
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        for(int diff = 1; diff < 8; diff++){
            try{
                if(col + diff < 8){
                    try{
                        if(row + diff < 8 && isValidMove(currentSquare, getSquareAt(col + diff, row + diff))){
                            log.info("bishop can move from1 " + currentSquare + "to " + getSquareAt(col + diff, row + diff));
                            return true;
                        }
                    }catch (InvalidMoveException ignored) {}
                    try{
                        if(row - diff >= 0 && isValidMove(currentSquare, getSquareAt(col + diff, row - diff))){
                            log.info("bishop can move from2 " + currentSquare + "to " + getSquareAt(col + diff, row - diff));
                            return true;
                        }
                    }catch (InvalidMoveException ignored) {}
                }
                if(col - diff >= 0){
                    try{
                        if(row + diff < 8 && isValidMove(currentSquare, getSquareAt(col - diff, row + diff))){
                            log.info("bishop can move from3 " + currentSquare + "to " + getSquareAt(col - diff, row + diff));
                            return true;
                        }
                    }catch (InvalidMoveException ignored) {}
                    try{
                        if(row - diff >= 0 && isValidMove(currentSquare, getSquareAt(col - diff, row - diff))){
                            log.info("bishop can move from4 " + currentSquare + "to " + getSquareAt(col - diff, row - diff));
                            return true;
                        }
                    }catch (InvalidMoveException ignored) {}
                }
            }catch (SquareNotFoundException ignored){

            }
        }
        return false;
    }

    private boolean doesRookHaveAMove(Square currentSquare){
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        for(int curRow = 0; curRow < 8; curRow++){
            if(curRow != row){
                try{
                    if(isValidMove(currentSquare, squaresArray[curRow][col])){
                        log.info("rook can move from1 " + currentSquare + "to " + squaresArray[curRow][col]);
                        return true;
                    }
                } catch (InvalidMoveException ignored) {}

            }
        }
        for(int curCol = 0; curCol < 8; curCol++){
            if(curCol != row){
                try{
                    if(isValidMove(currentSquare, squaresArray[row][curCol])){
                        log.info("rook can move from2 " + currentSquare + "to " + squaresArray[row][curCol]);
                        return true;
                    }
                } catch (InvalidMoveException ignored) {}

            }
        }
        return false;
    }

    private boolean doesQueenHaveAMove(Square currentSquare){
        if(doesRookHaveAMove(currentSquare) || doesBishopHaveAMove(currentSquare)){
            log.info("Queen can move");
            return true;
        }
        return false;
    }

    private boolean doesKingHaveAMove(Square currentSquare) {
        int row = currentSquare.getRowIndex();
        int col = currentSquare.getColIndex();
        log.info("king is currently at row: " + row + " and col: " + col);
        for(int i = -1; i < 2; i++){
            for(int j = -1; j < 2; j++){
                if(i != row || j != col){
                    try{
                        if(row + i < 8 && row + i >= 0
                                && col + j < 8 && col + j >=0 &&
                                isValidMove(currentSquare, squaresArray[row + i][col + j])){
                            log.info("King can move from " + currentSquare + "to " + squaresArray[row + i][col + j]);
                            return true;
                        }
                    } catch (InvalidMoveException e){log.info("king can't move to: " + squaresArray[row + i][col + j] + "eror is: " + e.getMsg());}
                }
            }
        }
        return false;
    }

    public boolean isKingInCheck(boolean isWhite) throws SquareNotFoundException {
        Square kingSquare;
        if(isWhite){
            kingSquare = getSquareAt(whiteKing.getColIndex(), whiteKing.getRowIndex());
        } else{
            kingSquare = getSquareAt(blackKing.getColIndex(), blackKing.getRowIndex());
        }

        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                Square currentSquare = squaresArray[row][col];
                Piece piece = currentSquare.getPiece();
                if(piece != null && isWhite != (piece.getColor() == Color.WHITE)){
                    if(piece.getPieceType() == PieceType.QUEEN){
                        log.info("king is at: " + kingSquare + ". queen is at: " + currentSquare + ". is Queen blocked? " + isPieceBlocked(piece, currentSquare, kingSquare, false) + ". can the queen make the move: " + piece.canMakeMove(currentSquare, kingSquare));
                    }
                    if(piece.canMakeMove(currentSquare, kingSquare) && !isPieceBlocked(piece, currentSquare, kingSquare, false)){
                        log.info("\n \n isKingInCheck king is in check. by piece: \n" + piece + "\n \n");
                        return true;
                    }
                }
            }
        }
        return false;

    }

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

    public void movePieceTemp(Square start, Square end){
        //this function does not care if the move is legal. just makes it (assumes piece at start isn't null).
        Piece movingPiece = start.getPiece();
        movingPiece.setHasMoved(true);
        start.setPiece(null);
        end.setPiece(movingPiece);
        movingPiece.setColIndex(end.getColIndex());
        movingPiece.setRowIndex(end.getRowIndex());
    }


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


    public boolean isPieceBlocked(Piece movingPiece, Square start, Square end, boolean isCastlingMove){
        switch(movingPiece.getPieceType()){
            case KING -> {
                if(isCastlingMove){
                    if(end.getColIndex() > start.getColIndex()){
                        //short castle
                        try{
                            log.info("eh1" + getSquareAt(start.getColIndex()+1, start.getRowIndex()).getPiece());
                            log.info("eh2" + getSquareAt(start.getColIndex()+2, start.getRowIndex()).getPiece());
                            return getSquareAt(start.getColIndex()+1, start.getRowIndex()).getPiece() != null ||
                                    getSquareAt(start.getColIndex()+2, start.getRowIndex()).getPiece() != null;
                        }catch (SquareNotFoundException e){
                            log.info("isBlockingPiece, square not found: " + e.getMsg());
                        }

                    }else{
                        //long castle
                        try{
                            return getSquareAt(start.getColIndex()-1, start.getRowIndex()).getPiece() != null ||
                                    getSquareAt(start.getColIndex()-2, start.getRowIndex()).getPiece() != null ||
                                    getSquareAt(start.getColIndex()-3, start.getRowIndex()).getPiece() != null;
                        }catch (SquareNotFoundException e){
                            log.info("isBlockingPiece, square not found: " + e.getMsg());
                        }

                    }
                }else{
                    return false; //king can only move 1 square, nothing is blocking his way
                }
            }
            case KNIGHT -> {
                return false; //knight skips over pieces
            }
            case QUEEN -> {
                if(start.getColIndex() == end.getColIndex()){
                    return !isClearVertically(start, end);
                } else if(start.getRowIndex() == end.getRowIndex()){
                    return !isClearHorizontally(start, end);
                } else{
                    //diagonal
                    return !isClearDiagonally(start, end);
                }
            }
            case ROOK -> {
                if(start.getColIndex() == end.getColIndex()){
                    return !isClearVertically(start, end);
                } else if(start.getRowIndex() == end.getRowIndex()) {
                    return !isClearHorizontally(start, end);
                }
            }
            case BISHOP -> {
                return !isClearDiagonally(start, end);
            }
            case PAWN ->{
                if(end.getRowIndex() - start.getRowIndex() == 2){
                    log.info("pawn moving 2 forward");
                    return squaresArray[start.getRowIndex() + 1][start.getColIndex()].getPiece() != null ||
                            end.getPiece() != null;
                } else if(end.getRowIndex() - start.getRowIndex() == -2){
                    log.info("pawn moving 2 down");
                    return squaresArray[start.getRowIndex() - 1][start.getColIndex()].getPiece() != null ||
                            end.getPiece() != null;
                } else if (end.getColIndex() == start.getColIndex()){
                    return end.getPiece() != null;
                } else{
                    return false;
                }
            }
        }

        //Every case was covered. this will never be reached.
        return false;
    }

    public boolean isClearVertically(Square start, Square end){
        if(start.getRowIndex() > end.getRowIndex()){ //piece is moving down the board
            //checking for blocking pieces
            for(int curRow = start.getRowIndex() - 1; curRow > end.getRowIndex(); curRow--){
                Piece pieceAt = squaresArray[curRow][start.getColIndex()].getPiece();
                if(pieceAt != null){

                    return false;
                }
            }
        } else{ //piece is moving up the board
            for(int curRow = start.getRowIndex() + 1; curRow < end.getRowIndex(); curRow++){
                Piece pieceAt = squaresArray[curRow][start.getColIndex()].getPiece();
                if(pieceAt != null){

                    return false;
                }
            }
        }
        return true;
    }

    public boolean isClearHorizontally(Square start, Square end){
        if(start.getColIndex() > end.getColIndex()){ //piece is from right to left
            //checking for blocking pieces
            for(int curCol = start.getColIndex() - 1; curCol > end.getRowIndex(); curCol--){
                Piece pieceAt = squaresArray[start.getRowIndex()][curCol].getPiece();
                if(pieceAt != null){

                    return false;
                }
            }
        } else{ //piece is left to right
            for(int curCol = start.getColIndex() + 1; curCol < end.getRowIndex(); curCol++){
                Piece pieceAt = squaresArray[start.getRowIndex()][curCol].getPiece();
                if(pieceAt != null){

                    return false;
                }
            }
        }
        return true;
    }

    public boolean isClearDiagonally(Square start, Square end){
        if(start.getColIndex() < end.getColIndex()){
            //going from the left to the right
            if(start.getRowIndex() < end.getRowIndex()){
                //going up
                for(int curDiff = 1; curDiff < end.getRowIndex() - start.getRowIndex(); curDiff++){
                    log.info("startSquare:" + start + "endSquare: " + end + "and curDiff: " + curDiff + "so we are checking at row: " + (start.getRowIndex() + curDiff) + "and col: " + (start.getColIndex() + curDiff));
                    if(squaresArray[start.getRowIndex() + curDiff][start.getColIndex() + curDiff].getPiece() != null){
                        return false;
                    }
                }
            }else{
                //going down. the row number is going down, col number going up
                for(int curDiff = 1; curDiff < start.getRowIndex() - end.getRowIndex(); curDiff++){
                    if(squaresArray[start.getRowIndex() - curDiff][start.getColIndex() + curDiff].getPiece() != null){
                        return false;
                    }
                }
            }
        }else{
            //going from left to right. col number going down
            if(start.getRowIndex() < end.getRowIndex()){
                //going up. row num going up, col down
                for(int curDiff = 1; curDiff < end.getRowIndex() - start.getRowIndex(); curDiff++){
                    if(squaresArray[start.getRowIndex() + curDiff][start.getColIndex() - curDiff].getPiece() != null){
                        return false;
                    }
                }
            }else{
                //going down. the row number is going down, col number down
                for(int curDiff = 1; curDiff < start.getRowIndex() - end.getRowIndex(); curDiff++){
                    if(squaresArray[start.getRowIndex() - curDiff][start.getColIndex() - curDiff].getPiece() != null){
                        return false;
                    }
                }
            }
        }
        return true;
    }
    public void setPieceAt(int col, int row, Piece piece){
        Square square = squaresArray[row][col];
        square.setPiece(piece);

    }
    private void setBoard(){
        Piece currentPiece;
        for (int col = 0; col < 8; col++){
            currentPiece = new Piece(Color.WHITE, PieceType.PAWN, false, squaresArray[1][col]);
            setPieceAt(col, 1, currentPiece);

            currentPiece = new Piece(Color.BLACK, PieceType.PAWN, false, squaresArray[6][col]);
            setPieceAt(col, 6, currentPiece);
        }

        currentPiece = new Piece(Color.WHITE, PieceType.ROOK, false, squaresArray[0][0]);
        setPieceAt(0, 0, currentPiece);
        currentPiece = new Piece(Color.WHITE, PieceType.ROOK, false, squaresArray[0][7]);
        setPieceAt(7, 0, currentPiece);

        currentPiece = new Piece(Color.BLACK, PieceType.ROOK, false, squaresArray[7][0]);
        setPieceAt(0, 7, currentPiece);
        currentPiece = new Piece(Color.BLACK, PieceType.ROOK, false, squaresArray[7][7]);
        setPieceAt(7, 7, currentPiece);

        currentPiece = new Piece(Color.WHITE, PieceType.KNIGHT, false, squaresArray[0][1]);
        setPieceAt(1, 0, currentPiece);
        currentPiece = new Piece(Color.WHITE, PieceType.KNIGHT, false, squaresArray[0][6]);
        setPieceAt(6, 0, currentPiece);

        currentPiece = new Piece(Color.BLACK, PieceType.KNIGHT, false, squaresArray[7][1]);
        setPieceAt(1, 7, currentPiece);
        currentPiece = new Piece(Color.BLACK, PieceType.KNIGHT, false, squaresArray[7][6]);
        setPieceAt(6, 7, currentPiece);

        currentPiece = new Piece(Color.WHITE, PieceType.BISHOP, false, squaresArray[0][2]);
        setPieceAt(2, 0, currentPiece);
        currentPiece = new Piece(Color.WHITE, PieceType.BISHOP, false, squaresArray[0][5]);
        setPieceAt(5, 0, currentPiece);

        currentPiece = new Piece(Color.BLACK, PieceType.BISHOP, false, squaresArray[7][2]);
        setPieceAt(2, 7, currentPiece);
        currentPiece = new Piece(Color.BLACK, PieceType.BISHOP, false, squaresArray[7][5]);
        setPieceAt(5, 7, currentPiece);


        currentPiece = new Piece(Color.WHITE, PieceType.KING, false, squaresArray[0][4]);
        setPieceAt(4, 0, currentPiece);
        whiteKing = currentPiece;

        currentPiece = new Piece(Color.WHITE, PieceType.QUEEN, false, squaresArray[0][3]);
        setPieceAt(3, 0, currentPiece);

        currentPiece = new Piece(Color.BLACK, PieceType.KING, false, squaresArray[7][4]);
        setPieceAt(4, 7, currentPiece);
        blackKing = currentPiece;

        currentPiece = new Piece(Color.BLACK, PieceType.QUEEN, false, squaresArray[7][3]);
        setPieceAt(3, 7, currentPiece);
    }


    /*@Override
    public String toString() {
        StringBuilder desc = new StringBuilder(" \n");
        for(int i = 7; i > -1; i--){
            desc.append(i);
            desc.append(" ");
            for(int j = 0; j <=7; j++){
                Piece pieceAt;
                try {
                    pieceAt = getSquareAt(j, i).getPiece();
                } catch (SquareNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if(pieceAt == null){
                    desc.append(". ");
                } else{
                    boolean isWhite = Objects.equals(pieceAt.getColor(), Color.WHITE);
                    if(pieceAt.getPieceType() ==  PieceType.KING){
                        if(isWhite) {
                            desc.append("k ");
                        }else{
                            desc.append("K ");
                        }
                    }else if(pieceAt.getPieceType() ==  PieceType.QUEEN){
                        if(isWhite) {
                            desc.append("q ");
                        }else{
                            desc.append("Q ");
                        }
                    }else if(pieceAt.getPieceType() ==  PieceType.ROOK){
                        if(isWhite) {
                            desc.append("r ");
                        }else{
                            desc.append("R ");
                        }
                    }else if(pieceAt.getPieceType() ==  PieceType.BISHOP){
                        if(isWhite) {
                            desc.append("b ");
                        }else{
                            desc.append("B ");
                        }
                    }else if(pieceAt.getPieceType() ==  PieceType.KNIGHT){
                        if(isWhite) {
                            desc.append("n ");
                        }else{
                            desc.append("N ");
                        }
                    }else if(pieceAt.getPieceType() ==  PieceType.PAWN){
                        if(isWhite) {
                            desc.append("p ");
                        }else{
                            desc.append("P ");
                        }
                    }
                }

            }
            desc.append("\n");
        }
        return desc.toString();
    }*/

}
