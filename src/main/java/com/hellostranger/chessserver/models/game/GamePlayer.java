package com.hellostranger.chessserver.models.game;


/*import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.models.enums.GameState;
import com.hellostranger.chessserver.models.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


*//*
    Represent how a user was in a game. Used to hold the elo at the time, the color and so on.
    Has a OneToMany relation with User (each user has been many GamePlayers).
    Exists to allow each game to know information about its player, such as Color and elo.
*//*
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor*/
public class GamePlayer {
    /*@Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    private Color color;

    @Enumerated(EnumType.STRING)
    private GameState gameState;

    private Integer elo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gameRepresentation_id")
    private GameRepresentation gameRepresentation;

    public String getEmail(){
        return user.getEmail();
    }

    @Override
    public String toString(){
        return "Id: " + id + " name: " + name + " Color: " + color + " gameState: " + gameState + " Elo: " + elo;
    }*/
}
