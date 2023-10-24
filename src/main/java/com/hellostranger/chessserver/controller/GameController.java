package com.hellostranger.chessserver.controller;



import com.hellostranger.chessserver.controller.dto.JoinRequest;
import com.hellostranger.chessserver.exceptions.GameFullException;
import com.hellostranger.chessserver.exceptions.GameNotFoundException;
import com.hellostranger.chessserver.exceptions.NoOpenGameException;
import com.hellostranger.chessserver.models.game.Game;
import com.hellostranger.chessserver.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@AllArgsConstructor
@Slf4j
public class GameController {
    private final GameService gameService;

    @PostMapping("/create")
    public ResponseEntity<Game> createGame(){
        Game newGame = gameService.createGame();
        log.info("Game created. id: " + newGame.getId());
        return ResponseEntity.ok(newGame);
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

    /*@GetMapping("/{gameId}/playerOne/name")
    public ResponseEntity<String> getPlayerOneName(@PathVariable String gameId){
        ghghghgghgghghghghghg       Game game = null;
        try{
            game = gameService.getGameById(gameId);
        } catch (NotFoundException e){
            log.info("Game not found: " + e.getMsg());
            return ResponseEntity.ok("");
        }
        log.info("getP1 name, name: " + ResponseEntity.ok(game.getPlayer1().getName()));
        return ResponseEntity.ok(game.getPlayer1().getName());
    }
    @GetMapping("/{gameId}/playerOne/uid")
    public ResponseEntity<String> getPlayerOneUid(@PathVariable String gameId){
        Game game = null;
        try{
            game = gameService.getGameById(gameId);
        } catch (NotFoundException e){
            log.info("Game not found: " + e.getMsg());
            return ResponseEntity.ok("");
        }
        log.info("getP1 uid, uid: " + ResponseEntity.ok(game.getPlayer1().getUid()));
        return ResponseEntity.ok(game.getPlayer1().getUid());
    }

    @GetMapping("/{gameId}/playerTwo/name")
    public ResponseEntity<String> getPlayerTwoName(@PathVariable String gameId){
        Game game = null;
        try{
            game = gameService.getGameById(gameId);
        } catch (NotFoundException e){
            log.info("Game not found: " + e.getMsg());
            return ResponseEntity.ok("");
        }
        if(game.getPlayer2() == null){
            return ResponseEntity.ok("");
        }
        log.info("getP2 name, name: " + ResponseEntity.ok(game.getPlayer2().getName()));
        return ResponseEntity.ok(game.getPlayer2().getName());
    }
    @GetMapping("/{gameId}/playerTwo/uid")
    public ResponseEntity<String> getPlayerTwoUid(@PathVariable String gameId){
        Game game = null;
        try{
            game = gameService.getGameById(gameId);
        } catch (NotFoundException e){
            log.info("Game not found: " + e.getMsg());
            return ResponseEntity.ok("");
        }
        if(game.getPlayer2() == null){
            return ResponseEntity.ok("");
        }
        log.info("getP2 uid, uid: " + ResponseEntity.ok(game.getPlayer2().getUid()));
        return ResponseEntity.ok(game.getPlayer2().getUid());
    }
*/


}
