package com.hellostranger.chessserver.controller.dto.websocket;


import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.game.Move;
import lombok.Getter;

import java.util.List;


@Getter
public class GameEndMessage extends Message {
    private GameState state;


    public GameEndMessage(GameState state){
        super(MessageType.END);
        this.state = state;
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
