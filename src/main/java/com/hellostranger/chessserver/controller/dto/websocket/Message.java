package com.hellostranger.chessserver.controller.dto.websocket;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message {
    protected MessageType messageType;

    public Message(MessageType messageType){
        this.messageType = messageType;
    }
    public Message(){
        this.messageType = null;
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "messageType=" + messageType +
                '}';
    }

}
