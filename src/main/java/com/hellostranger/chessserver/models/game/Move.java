package com.hellostranger.chessserver.models.game;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Move {
    private int startCol;
    private int startRow;
    private int endCol;
    private int endRow;

}