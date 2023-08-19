package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class NoOpenGameException extends Exception {
    private final String msg;

    public NoOpenGameException(String msg){
        this.msg = msg;
    }

}
