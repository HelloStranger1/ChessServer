package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class GameNotFoundException extends Exception {
    private final String msg;

    public GameNotFoundException(String msg){
        this.msg = msg;
    }
}
