package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class GameFinishedException extends Exception{
    private final String msg;

    public GameFinishedException(String msg){
        this.msg = msg;
    }

}