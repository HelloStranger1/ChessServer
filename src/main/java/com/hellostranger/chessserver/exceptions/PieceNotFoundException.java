package com.hellostranger.chessserver.exceptions;

import lombok.Getter;

@Getter
public class PieceNotFoundException extends Exception{
    private final String msg;

    public PieceNotFoundException(String msg){
        this.msg = msg;
    }

}
