package com.hellostranger.chessserver.controller;



import com.hellostranger.chessserver.controller.dto.JoinRequest;
import com.hellostranger.chessserver.exceptions.GameFullException;
import com.hellostranger.chessserver.exceptions.GameNotFoundException;
import com.hellostranger.chessserver.models.game.Game;
import com.hellostranger.chessserver.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        try{
            Game joinedGame = gameService.joinPrivateGame(request.getPlayerEmail(), shortenedId);
            return ResponseEntity.ok(joinedGame);
        } catch (GameFullException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (GameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/join/{gameId}")
    public ResponseEntity<Game> joinGame(@PathVariable(name = "gameId") String gameId, @RequestBody JoinRequest request){
        try{
            Game joinedGame = gameService.joinGame(gameId, request.getPlayerEmail());
            return ResponseEntity.ok(joinedGame);
        } catch (GameFullException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/join/random")
    public ResponseEntity<Game> joinRandomGame(@RequestBody JoinRequest request){
        try{
            Game joinedGame = gameService.joinRandomGame(request.getPlayerEmail());
            return ResponseEntity.ok(joinedGame);
        } catch (GameFullException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }




}
