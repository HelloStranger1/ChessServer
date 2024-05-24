package com.hellostranger.chessserver.controller.dto.websocket;


import com.hellostranger.chessserver.core.GameResult;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class GameEndMessage extends Message {
    private String message;
    private GameResult state;
    private Integer whiteElo;
    private Integer blackElo;
    private Integer gameRepresentationId;


    public GameEndMessage(GameResult state, String message, int whiteElo, int blackElo, int gameRepresentationId){
        super(MessageType.END);
        this.state = state;
        this.message = message;
        this.whiteElo = whiteElo;
        this.blackElo = blackElo;
        this.gameRepresentationId = gameRepresentationId;
    }

    @Override
    public String toString() {
        return "GameEndMessage{" +
                "state=" + state +
                ", messageType=" + messageType +
                ", whiteElo=" + whiteElo +
                ", blackElo=" + blackElo +
                ", gameRepresentationId=" + gameRepresentationId +
                '}';
    }

}
