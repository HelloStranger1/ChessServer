package com.hellostranger.chessserver.controller.dto;

import com.hellostranger.chessserver.core.GameResult;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@Builder
@AllArgsConstructor
public class GameHistoryResponse {
    private Integer id;
    private String whiteImage;
    private String blackImage;
    private String whiteName;
    private String blackName;
    private Integer whiteElo;
    private Integer blackElo;
    private LocalDate gameDate;
    private Integer opponentColorIndex;
    private GameResult result;
    private String startBoardFen;
    private String gameMoves;

    public GameHistoryResponse(GameRepresentation gameRepresentation, User white, User black, Integer opponentColor, String gameMoves) {
        this.id = gameRepresentation.getId();
        this.whiteImage = white.getImage();
        this.blackImage = black.getImage();
        this.whiteName = white.getName();
        this.blackName = black.getName();
        this.whiteElo = white.getElo();
        this.blackElo = black.getElo();
        this.gameDate = gameRepresentation.getDate();
        this.opponentColorIndex = opponentColor;
        this.result = gameRepresentation.getResult();
        this.startBoardFen = gameRepresentation.getStartBoardFen();
        this.gameMoves = gameMoves;
    }

}
