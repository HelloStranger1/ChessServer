package com.hellostranger.chessserver.controller;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.controller.dto.UpdateRequest;
import com.hellostranger.chessserver.models.entities.FriendRequest;

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
        if(Objects.equals(recipientEmail, userEmail)){
            String errorMessage = "Can't send friend request to yourself";
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errorMessage);
        }
        if(friendRequestService.areUsersFriends(userEmail, recipientEmail)){
            String errorMessage = "Can't send friend request to a friend";
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errorMessage);
        }
        List<FriendRequest> recipientPendingRequests = friendRequestService.getPendingFriendRequests(recipientEmail);
        for(FriendRequest request : recipientPendingRequests){
            if(Objects.equals(request.getSender().getEmail(), userEmail)){
                String errorMessage = "You have already send a friend request to the user with email " + recipientEmail ;
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errorMessage);
            }
        }

        friendRequestService.sendFriendRequest(userEmail, recipientEmail);
        return ResponseEntity.ok("Friend request sent.");
    }

    @GetMapping("/{userEmail}/get-friend-requests")
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

    @GetMapping("/get-friends/{userEmail}")
    public ResponseEntity<?> getFriends(@PathVariable("userEmail") String userEmail){
        try{
            User user = userService.getUserByEmail(userEmail);
            List<User> friends = user.getFriends();
            return ResponseEntity.ok(friends);
        } catch (NoSuchElementException e){
            String errorMessage = "User with email " + userEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }
    @PostMapping("/remove-friend/{userEmail}")
    public ResponseEntity<String> removeFriend(
            @PathVariable("userEmail") String userEmail,
            @RequestParam("friendEmail") String friendEmail){
        try{
            User user = userService.getUserByEmail(userEmail);
            User friend = userService.getUserByEmail(friendEmail);
            List<User> friends = user.getFriends();
            int originalSize = friends.size();
            friends.remove(friend);
            if(originalSize != friends.size()){
                friend.getFriends().remove(user);
            }
            return ResponseEntity.ok("Friend Removed Successfully");
        } catch (NoSuchElementException e){
            String errorMessage = "Friend with email " + friendEmail + " not found.";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        }
    }
}

