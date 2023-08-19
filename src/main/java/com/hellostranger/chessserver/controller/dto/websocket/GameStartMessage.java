package com.hellostranger.chessserver.controller.dto.websocket;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameStartMessage extends Message {
    private String whitePlayerName;
    private String blackPlayerName;
    private String whitePlayerEmail;
    private String blackPlayerEmail;

    public GameStartMessage(String whitePlayerName, String blackPlayerName, String whitePlayerEmail, String blackPlayerEmail){
        super(MessageType.START);
        this.whitePlayerName = whitePlayerName;
        this.whitePlayerEmail = whitePlayerEmail;
        this.blackPlayerName = blackPlayerName;
        this.blackPlayerEmail = blackPlayerEmail;
    }

    @Override
    public String toString() {
        return "GameStartMessage{" +
                "whitePlayerName='" + whitePlayerName + '\'' +
                ", blackPlayerName='" + blackPlayerName + '\'' +
                ", whitePlayerUid='" + whitePlayerEmail + '\'' +
                ", blackPlayerUid='" + blackPlayerEmail + '\'' +
                ", messageType=" + messageType +
                '}';
    }


}
