package com.hellostranger.chessserver.controller.dto;

import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.GameState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@Builder
public class GameHistoryResponse {
    private Integer id;
    private String opponentImage;
    private String opponentName;
    private Integer opponentElo;
    private LocalDate gameDate;
    private Color opponentColor;
    private GameState result;
    private List<String> boardsHistoryFEN;


}
