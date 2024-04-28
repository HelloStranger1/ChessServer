package com.hellostranger.chessserver.controller.dto.websocket;

import lombok.Getter;

@Getter
public class ConcedeGameMessage extends Message{
    private final String playerEmail;

    public ConcedeGameMessage(String playerEmail) {
        super(MessageType.CONCEDE);
        this.playerEmail = playerEmail;
    }
}
