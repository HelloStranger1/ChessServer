package com.hellostranger.chessserver.models.entities;

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

    private String move;

    @Override
    public String toString(){
        return move;

    }


}
