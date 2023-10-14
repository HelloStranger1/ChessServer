package com.hellostranger.chessserver.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;

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

    @Column(columnDefinition = "TEXT")
    private String boardJson;

    @ManyToOne
    @JoinColumn(name = "gameRepresentation_id")
    @JsonIgnore
    private GameRepresentation game;

    @Override
    public String toString(){
        return "Id: " + id + "boards: " + boardJson;
    }


}
