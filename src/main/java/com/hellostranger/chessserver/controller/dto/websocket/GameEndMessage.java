package com.hellostranger.chessserver.controller.dto.websocket;


import com.hellostranger.chessserver.models.enums.GameState;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class GameEndMessage extends Message {
    private String message;
    private GameState state;


    public GameEndMessage(GameState state, String message){
        super(MessageType.END);
        this.state = state;
        this.message = message;
    }

    @Override
    public String toString() {
        return "GameEndMessage{" +
                "state=" + state +
                ", messageType=" + messageType +
                '}';
    }

    public void setState(GameState state) {
        this.state = state;
    }

}
