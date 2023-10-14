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
public class MoveRepresentation {
    @Id
    @GeneratedValue
    private Integer id;

    private int startCol;
    private int startRow;
    private int endCol;
    private int endRow;

    /*@ManyToOne
    @JoinColumn(name = "gameRepresentation_id")
    @JsonIgnore
    private GameRepresentation game;*/

    @Override
    public String toString(){
        return "" + startCol + startRow + endCol + endRow;
    }


}
