package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class InvalidGameException extends  Exception{
    private final String msg;

    public InvalidGameException(String msg){
        this.msg = msg;
    }

}
