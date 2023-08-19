package com.hellostranger.chessserver.models.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.models.enums.GameState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Game {

    private String id;

    private Board board;

    @JsonIgnore
    private GamePlayer player1;

    @JsonIgnore
    private GamePlayer player2;


    @JsonIgnore
    private GamePlayer waitingGamePlayer;
    private Boolean isP1turn;

    private GameState gameState;

    private List<Move> moveList;

    private Integer halfMoves = 0; //This field is useful to enforce the 50-move draw rule. When this counter reaches 100 (allowing each player to make 50 moves), the game ends in a draw.

    private Integer fullMoves = 0;

    public void addMove(Move move){
        if(moveList == null){
            moveList = new ArrayList<>();
        }
        moveList.add(move);
    }
}
