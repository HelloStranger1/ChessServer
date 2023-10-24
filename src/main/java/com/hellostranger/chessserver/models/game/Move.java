package com.hellostranger.chessserver.models.game;

import com.hellostranger.chessserver.controller.dto.websocket.MoveMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Move {
    private int startCol;
    private int startRow;
    private int endCol;
    private int endRow;

    public Move(MoveMessage moveMessage){
        this.startCol = moveMessage.getStartCol();
        this.startRow = moveMessage.getStartRow();
        this.endCol = moveMessage.getEndCol();
        this.endRow = moveMessage.getEndRow();
    }

}