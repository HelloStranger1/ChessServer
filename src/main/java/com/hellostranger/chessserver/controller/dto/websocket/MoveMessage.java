package com.hellostranger.chessserver.controller.dto.websocket;


import com.hellostranger.chessserver.models.enums.PieceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveMessage extends Message {
    private String playerEmail;
    private int startCol;
    private int startRow;
    private int endCol;
    private int endRow;
    private boolean isSecondCastleMove = false;
    private PieceType promotionType = null;

    public MoveMessage(String playerEmail, int startCol, int startRow, int endCol, int endRow){
        super(MessageType.MOVE);
        this.playerEmail = playerEmail;
        this.startCol = startCol;
        this.startRow = startRow;
        this.endCol = endCol;
        this.endRow = endRow;
    }

    @Override
    public String toString() {
        return "Move. name: " + playerEmail + "startCol " + startCol + "startRow" + startRow + "endCol" + endCol + "endRow" + endRow + "promotion: " + promotionType;
    }

}
