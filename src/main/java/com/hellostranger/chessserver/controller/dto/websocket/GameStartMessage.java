package com.hellostranger.chessserver.controller.dto.websocket;

import com.hellostranger.chessserver.models.entities.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameStartMessage extends Message {
    private String whitePlayerName;
    private String blackPlayerName;
    private String whitePlayerEmail;
    private String blackPlayerEmail;
    private String whitePlayerImage;
    private String blackPlayerImage;
    private Integer whiteElo;
    private Integer blackElo;

    public GameStartMessage(User whitePlayer, User blackPlayer){
        super(MessageType.START);
        this.whitePlayerName = whitePlayer.getName();
        this.whitePlayerEmail = whitePlayer.getEmail();
        this.blackPlayerName = blackPlayer.getName();
        this.blackPlayerEmail = blackPlayer.getEmail();
        this.whitePlayerImage = whitePlayer.getImage();
        this.blackPlayerImage = blackPlayer.getImage();
        this.blackElo = blackPlayer.getElo();
        this.whiteElo = whitePlayer.getElo();
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
