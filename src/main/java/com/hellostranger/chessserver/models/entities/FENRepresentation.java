package com.hellostranger.chessserver.models.entities;

import com.hellostranger.chessserver.models.entities.GameRepresentation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
