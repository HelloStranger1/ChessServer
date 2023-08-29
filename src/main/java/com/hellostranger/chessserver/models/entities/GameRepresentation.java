package com.hellostranger.chessserver.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class GameRepresentation {

    @Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<FENRepresentation> FENRepresentations;

    public void addFENRepresentation(FENRepresentation fenRepresentation){
        if(this.FENRepresentations == null){
            this.FENRepresentations = new HashSet<>();
        }

        this.FENRepresentations.add(fenRepresentation);
    }


    @Override
    public String toString(){
        return "Id: " + id + "white: " + whitePlayer + "and black is: " + blackPlayer;
    }
}
