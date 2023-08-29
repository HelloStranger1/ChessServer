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
    private Integer whiteElo;
    private Integer blackElo;

    public GameStartMessage(String whitePlayerName, String blackPlayerName, String whitePlayerEmail, String blackPlayerEmail, Integer whiteElo, Integer blackElo){
        super(MessageType.START);
        this.whitePlayerName = whitePlayerName;
        this.whitePlayerEmail = whitePlayerEmail;
        this.blackPlayerName = blackPlayerName;
        this.blackPlayerEmail = blackPlayerEmail;
        this.blackElo = blackElo;
        this.whiteElo = whiteElo;
    }

    @Override
    public String toString() {
        return "GameStartMessage{" +
                "whitePlayerName='" + whitePlayerName + '\'' +
                ", blackPlayerName='" + blackPlayerName + '\'' +
                ", whitePlayerUid='" + whitePlayerEmail + '\'' +
                ", blackPlayerUid='" + blackPlayerEmail + '\'' +
                ", whiteElo='" + whiteElo +'\'' +
                ", blackElo='" + blackElo +'\'' +
                ", messageType=" + messageType +
                '}';
    }


}
