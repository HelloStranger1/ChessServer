package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class InvalidMoveException extends Exception{
    private final String msg;

    public InvalidMoveException(String msg){
        this.msg = msg;
    }

}
