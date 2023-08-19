package com.hellostranger.chessserver.controller.dto;

import com.hellostranger.chessserver.models.game.GamePlayer;
import lombok.Data;

@Data
public class JoinRequest {
    private String playerEmail;
}
