package com.hellostranger.chessserver.models;

import com.hellostranger.chessserver.models.game.GamePlayer;
import com.hellostranger.chessserver.models.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class GameRepresentation {

    @Id
    @GeneratedValue
    private Integer id;

    @OneToMany(mappedBy = "gameRepresentation")
    private List<GamePlayer> players;

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    private List<FENRepresentation> FENRepresentations;

    public void addFENRepresentation(String FEN){
        if(this.FENRepresentations == null){
            this.FENRepresentations = new ArrayList<>();
        }
        FENRepresentation fenRepresentation = new FENRepresentation();
        fenRepresentation.setFEN(FEN);
        fenRepresentation.setGame(this);
        this.FENRepresentations.add(fenRepresentation);
    }

    public void addPlayer(GamePlayer player){
        if(this.players == null){
            this.players = new ArrayList<>();
        }
        this.players.add(player);
    }

    @Override
    public String toString(){
        return "Id: " + id + "Players: " + players;
    }
}
