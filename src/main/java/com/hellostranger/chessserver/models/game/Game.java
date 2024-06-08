package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.core.GameResult;
import com.hellostranger.chessserver.core.board.Board;
import com.hellostranger.chessserver.core.helpers.BoardHelper;
import com.hellostranger.chessserver.core.moveGeneration.MoveGenerator;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Game {

    private String id;

    @JsonIgnore
    private Board board;

    @JsonIgnore
    private User whitePlayer;

    @JsonIgnore
    private User blackPlayer;

    @JsonIgnore
    private User waitingPlayer;

    @JsonIgnore
    public MoveGenerator moveGenerator = new MoveGenerator();

    private GameResult gameState;


    private List<String> boardsFen;

    private Boolean isDrawOfferedByWhite = false;
    private Boolean isDrawOfferedByBlack = false;

    @JsonIgnore
    private GameRepresentation gameRepresentation = null;

    public void addFenToList(String fen) {
        if (boardsFen == null) {
            boardsFen = new ArrayList<>();
        }
        boardsFen.add(fen);
    }

    @Override
    public String toString() {
        if (whitePlayer == null || blackPlayer == null) {
            return "Board is: " + BoardHelper.createDiagram(board, true, true, true);
        }
        return "White email is: " + whitePlayer.getEmail() + " Black email is: " + blackPlayer.getEmail() + "and the board is: \n"+ BoardHelper.createDiagram(board, true, true, true);
    }
}
