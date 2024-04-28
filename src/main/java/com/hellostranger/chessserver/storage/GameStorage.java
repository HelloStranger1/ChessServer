package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.game.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
@Slf4j
public class GameStorage {
    private static Map<String, Game> games;

    //Matches the shorted code that people can share (6 chars long) to the long UUID of the game
    private static Map<String, String> keyUUIDMap;
    private static GameStorage instance;
    private GameStorage(){
        games = new HashMap<>();
        keyUUIDMap = new HashMap<>();
    }

    public String generateShortCode(Game game){
        if(keyUUIDMap.containsValue(game.getId())){
            for(String key : keyUUIDMap.keySet()){
                if(Objects.equals(keyUUIDMap.get(key), game.getId())){
                    return key;
                }
            }
        }
        String code = RandomStringUtils.randomAlphanumeric(6);
        while(keyUUIDMap.containsKey(code)){
            code = RandomStringUtils.randomAlphanumeric(6);
        }
        keyUUIDMap.put(code, game.getId());
        return code;

    }
    public String matchCodeToId(String code){
        if(keyUUIDMap.containsKey(code)){
            return keyUUIDMap.get(code);
        }
        log.error("Couldn't find matching Id to the code "+ code);
        return "";
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
    public void removeGame(Game game){games.remove(game.getId());}
}
