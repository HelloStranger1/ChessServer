package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.game.Game;

import java.util.HashMap;
import java.util.Map;

public class GameStorage {
    private static Map<String, Game> games;
    private static GameStorage instance;
    private GameStorage(){
        games = new HashMap<>();
    }

    //Singleton
    public static synchronized GameStorage getInstance(){
        if(instance == null){
            instance = new GameStorage();
        }
        return instance;
    }

    public Map<String, Game> getGames(){
        return games;
    }

    public void setGame(Game game){
        games.put(game.getId(), game);
    }
}
