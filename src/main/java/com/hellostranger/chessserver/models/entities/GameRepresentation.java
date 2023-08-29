package com.hellostranger.chessserver.models.entities;

import com.hellostranger.chessserver.models.enums.GameState;
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
    private GameState result;

    private LocalDate date = LocalDate.now();

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<BoardRepresentation> BoardRepresentations;

    public void addBoardRepresentation(BoardRepresentation boardRepresentation){
        if(this.BoardRepresentations == null){
            this.BoardRepresentations = new ArrayList<>();
        }

        this.BoardRepresentations.add(boardRepresentation);
    }


    @Override
    public String toString(){
        return "Id: " + id + "white: " + whitePlayer + "and black is: " + blackPlayer;
    }
}
