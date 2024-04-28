package com.hellostranger.chessserver.controller.dto.websocket;


import lombok.Getter;

@Getter
public class DrawOfferMessage extends Message {
    private final String playerEmail;
    private final boolean isWhite;

    public DrawOfferMessage(String email, boolean isWhite) {
        super(MessageType.DRAW_OFFER);
        this.playerEmail = email;
        this.isWhite = isWhite;
    }

}
