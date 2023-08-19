package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class GameFullException extends Exception{
    private final String msg;

    public GameFullException(String msg){
        this.msg = msg;
    }

}
