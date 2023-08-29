package com.hellostranger.chessserver.controller.dto.websocket;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcedeGameMessage extends Message{
    private String playerEmail;

    public ConcedeGameMessage(String playerEmail){
        super(MessageType.CONCEDE);
        this.playerEmail = playerEmail;
    }
}
