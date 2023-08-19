package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class SquareNotFoundException extends Exception{
    private final String msg;

    public SquareNotFoundException(String msg){
        this.msg = msg;
    }

}
