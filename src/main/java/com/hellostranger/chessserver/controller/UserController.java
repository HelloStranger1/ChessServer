package com.hellostranger.chessserver.controller;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.controller.dto.UpdateRequest;
import com.hellostranger.chessserver.models.entities.FriendRequest;
import com.hellostranger.chessserver.models.entities.GameRepresentation;

import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.service.FriendRequestService;
import com.hellostranger.chessserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FriendRequestService friendRequestService;


    @GetMapping("/{userEmail}")
    public ResponseEntity<?> getUserByEmail(
            @PathVariable("userEmail") String userEmail
    ){
        try{
            User user = userService.getUserByEmail(userEmail);
            userService.saveUser(user);
            return ResponseEntity.ok(user);
        } catch (NoSuchElementException e){
            String errorMessage = "User with email " + userEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }

    @PutMapping("/upload-image-URL/{userEmail}")
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable("userEmail") String userEmail,
            @RequestBody UpdateRequest updateRequest
    ){
        try{
            User user = userService.getUserByEmail(userEmail);
            String imageUrl = updateRequest.getUpdatedValue();
            log.info("imageUrl was: " + imageUrl + "and after substring it is: " + imageUrl.substring(1, imageUrl.length()-1));
            user.setImage(imageUrl);
            userService.saveUser(user);
            return ResponseEntity.ok("User Image Uploaded Successfully");
        } catch (NoSuchElementException e){
            String errorMessage = "User with email " + userEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }

    @PutMapping("/update-user-name/{userEmail}")
    public ResponseEntity<String> updateUserName(
            @PathVariable("userEmail") String userEmail,
            @RequestBody UpdateRequest updateRequest
    ){
        try{
            String name = updateRequest.getUpdatedValue();
            User user = userService.getUserByEmail(userEmail);
            user.setName(name);
            userService.saveUser(user);
            return ResponseEntity.ok("User name updated Successfully");
        } catch (NoSuchElementException e){
            String errorMessage = "User with email " + userEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }

    @GetMapping("/games-history/{userEmail}")
    public ResponseEntity<?> getGamesHistory(
            @PathVariable("userEmail") String userEmail
    ) {
        try{
            User user = userService.getUserByEmail(userEmail);
            List<GameHistoryResponse> games = userService.getUsersGameHistory(user);
            for(int i = 0; i < games.size(); i++){
                log.info("GamesHistoryResponse FEN at " + i + " is: " + games.get(i).getBoardsHistoryFEN());
            }
            return ResponseEntity.ok(games);
        } catch (NoSuchElementException e){
            String errorMessage = "User with email " + userEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }


    @PostMapping("/{userEmail}/send-friend-request")
    public ResponseEntity<String> sendFriendRequest(
            @PathVariable("userEmail") String userEmail,
            @RequestParam("recipientEmail") String recipientEmail) {

        friendRequestService.sendFriendRequest(userEmail, recipientEmail);
        return ResponseEntity.ok("Friend request sent.");
    }

    @GetMapping("/{userEmail}/friend-requests")
    public ResponseEntity<List<FriendRequest>> getFriendRequests(
            @PathVariable("userEmail") String userEmail) {

        List<FriendRequest> friendRequests = friendRequestService.getPendingFriendRequests(userEmail);
        return ResponseEntity.ok(friendRequests);
    }

    @PostMapping("/{userEmail}/accept-friend-request")
    public ResponseEntity<String> acceptFriendRequest(
            @PathVariable("userEmail") String userEmail,
            @RequestParam("requestId") Integer requestId) {

        friendRequestService.acceptFriendRequest(userEmail, requestId);
        return ResponseEntity.ok("Friend request accepted.");
    }
}

