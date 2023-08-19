package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class UserNotFoundException extends Exception{
    private final String msg;

    public UserNotFoundException(String msg){
        this.msg = msg;
    }
}
