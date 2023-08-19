package com.hellostranger.chessserver.models;

import com.hellostranger.chessserver.models.game.Game;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class FENRepresentation {
    @Id
    @GeneratedValue
    private Integer id;

    private String FEN;

    @ManyToOne
    @JoinColumn(name = "gameRepresentation_id")
    private GameRepresentation game;

    @Override
    public String toString(){
        return "Id: " + id + "FEN: " + FEN;
    }


}
