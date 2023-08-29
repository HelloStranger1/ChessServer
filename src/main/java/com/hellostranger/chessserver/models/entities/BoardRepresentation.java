package com.hellostranger.chessserver.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardRepresentation {
    @Id
    @GeneratedValue
    private Integer id;

    private String boardJson;

    @ManyToOne
    @JoinColumn(name = "gameRepresentation_id")
    @JsonIgnore
    private GameRepresentation game;

    @Override
    public String toString(){
        return "Id: " + id + "FEN: " + boardJson;
    }


}
