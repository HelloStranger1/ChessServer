package com.hellostranger.chessserver.controller.dto.websocket;

public class InvalidMoveMessage extends Message {
    public InvalidMoveMessage(){
        super(MessageType.INVALID_MOVE);
    }
}
