package com.hellostranger.chessserver.controller;



import com.hellostranger.chessserver.controller.dto.JoinRequest;
import com.hellostranger.chessserver.exceptions.GameFullException;
import com.hellostranger.chessserver.exceptions.GameNotFoundException;
import com.hellostranger.chessserver.exceptions.NoOpenGameException;
import com.hellostranger.chessserver.models.game.Game;
import com.hellostranger.chessserver.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.mapping.Join;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@AllArgsConstructor
@Slf4j
public class GameController {
    private final GameService gameService;

    @PostMapping("/create-private")
    public ResponseEntity<String> createPrivateGame(){
        String code = gameService.createPrivateGame();
        return ResponseEntity.ok(code);
    }

    @PostMapping("/join/private/{shortenedId}")
    public ResponseEntity<Game> joinPrivateGame(@PathVariable(name = "shortenedId") String shortenedId, @RequestBody JoinRequest request){
        log.info("Request to join private. shortId: " + shortenedId + " By player: " + request.getPlayerEmail());
        try{
            Game joinedGame = gameService.joinPrivateGame(request.getPlayerEmail(), shortenedId);
            log.info("Joined private");
            return ResponseEntity.ok(joinedGame);
        } catch (GameFullException e) {
            log.info("Game full");
            return null;
        } catch (GameNotFoundException e) {
            log.info("Game not found");
            return null;
        }
    }

    @PostMapping("/join/{gameId}")
    public ResponseEntity<Game> joinGame(@PathVariable(name = "gameId") String gameId, @RequestBody JoinRequest request){
        log.info("Request to join. gameId: " + gameId + "By player: " + request.getPlayerEmail());
        try{

            Game joinedGame = gameService.joinGame(gameId, request.getPlayerEmail());
            log.info("here");
            return ResponseEntity.ok(joinedGame);
        } catch (GameNotFoundException e){
            log.info("game not found");
            return null;
        } catch (GameFullException e){
            log.info("game full");
            return null;
        }
    }

    @PostMapping("/join/random")
    public ResponseEntity<Game> joinRandomGame(@RequestBody JoinRequest request){
        log.info("Request to join random. By player: " + request.getPlayerEmail());
        try{
            Game joinedGame = gameService.joinRandomGame(request.getPlayerEmail());
            log.info("here");
            return ResponseEntity.ok(joinedGame);
        } catch (GameNotFoundException e){
            log.info("game not found");
            return null;
        } catch (GameFullException e){
            log.info("game full");
            return null;
        } catch (NoOpenGameException e) {
            throw new RuntimeException(e);
        }
    }




}
