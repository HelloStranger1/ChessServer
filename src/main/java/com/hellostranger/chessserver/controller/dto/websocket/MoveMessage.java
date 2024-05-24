package com.hellostranger.chessserver.controller.dto.websocket;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveMessage extends Message {
    private String playerEmail;
    private Integer move;

    public MoveMessage(String playerEmail, Integer moveValue){
        super(MessageType.MOVE);
        this.playerEmail = playerEmail;
        this.move = moveValue;
    }

    @Override
    public String toString() {
        return "Move. player: " + playerEmail + "moveValue: " + move;
    }

}
