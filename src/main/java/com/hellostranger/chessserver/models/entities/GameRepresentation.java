package com.hellostranger.chessserver.models.entities;

import com.hellostranger.chessserver.core.GameResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    private GameResult result;

    private LocalDate date = LocalDate.now();

    private String startBoardFen;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "game_representation_id")
    private List<MoveRepresentation> moveRepresentations;

    public void addMoveRepresentation(MoveRepresentation moveRepresentation){
        if(this.moveRepresentations == null){
            this.moveRepresentations = new ArrayList<>();
        }

        this.moveRepresentations.add(moveRepresentation);

    }

    @Override
    public String toString(){
        return "Id: " + id + "white: " + whitePlayer + "and black is: " + blackPlayer;
    }
}
