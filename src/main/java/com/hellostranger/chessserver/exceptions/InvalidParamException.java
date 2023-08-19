package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class InvalidParamException extends Exception{
    private final String msg;

    public InvalidParamException(String msg){
        this.msg = msg;
    }

}
