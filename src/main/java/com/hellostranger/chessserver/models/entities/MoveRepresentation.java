package com.hellostranger.chessserver.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.models.enums.MoveType;
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
    private MoveType moveType;

    /*@ManyToOne
    @JoinColumn(name = "gameRepresentation_id")
    @JsonIgnore
    private GameRepresentation game;*/

    @Override
    public String toString(){
        return "" + startCol + startRow + endCol + endRow + matchTypeToChar(moveType);
    }

    private String matchTypeToChar(MoveType moveType){
        return switch (moveType){
            case REGULAR -> "0";
            case CASTLE -> "C";
            case PROMOTION_QUEEN -> "Q";
            case PROMOTION_ROOK -> "R";
            case PROMOTION_BISHOP -> "B";
            case PROMOTION_KNIGHT -> "K";
        };
    }

}
