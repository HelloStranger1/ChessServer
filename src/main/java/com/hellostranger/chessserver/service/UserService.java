package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.storage.GameRepresentationRepository;
import com.hellostranger.chessserver.storage.UserRepository;
import lombok.NonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.reverse;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;


    @Autowired
    GameRepresentationRepository gameRepresentationRepository;


    public User getUserByEmail(String userEmail) throws NoSuchElementException {
        User user =  userRepository.findByEmail(userEmail).orElseThrow();
        if (isActive((user)) && !user.getIsActive()) {
            user.setIsActive(true);
            userRepository.save(user);
        } else if (user.getIsActive() && !isActive(user)) {
            user.setIsActive(false);
            userRepository.save(user);
        }
        return user;
    }
//
//    @Scheduled(fixedRate = 90000)
//    public void checkInactiveUsers(){
//
//        LocalDateTime currentTime = LocalDateTime.now();
//        log.info(currentTime.toString());
//        for(Map.Entry<User, UserActivity> entry : userActivityMap.entrySet()){
//            User user = userRepository.findByEmail(entry.getKey().getEmail()).orElseThrow();
//            UserActivity userActivity = entry.getValue();
//
//            if(userActivity.getLastActiveTime().plusMinutes(1).isBefore(currentTime)){
//                user.setIsActive(false);
//                userRepository.save(user);
//                userActivityMap.remove(user);
//            }
//        }
//    }
    public boolean isActive(@NonNull User user){
        return user.getLastTimeActive().plusMinutes(1).isAfter(LocalDateTime.now());
    }
    public void keepAlive(@NonNull User user){
        user.setIsActive(true);
        user.setLastTimeActive(LocalDateTime.now());
        userRepository.save(user);
    }

    public void saveUser(User user){
        if (isActive((user)) && !user.getIsActive()) {
            user.setIsActive(true);
        } else if (user.getIsActive() && !isActive(user)) {
            user.setIsActive(false);
        }
        userRepository.save(user);

    }

    public GameHistoryResponse getGameHistoryByID(Integer id) throws NoSuchElementException{
        GameRepresentation response = gameRepresentationRepository.findById(id).orElseThrow();
        //This is only used to review the game, so it doesn't fully work with opponent.
        //TODO: Fix it
        return getResponseFromRepresentation(response, null);
    }
    public List<GameHistoryResponse> getUsersGameHistory(User user){
        List<GameRepresentation> gamesHistory = gameRepresentationRepository.findByWhitePlayerOrBlackPlayerOrderByDateDesc(user, user).orElse(new ArrayList<>());
        List<GameHistoryResponse> response = new ArrayList<>();
        for (GameRepresentation gameRepresentation : gamesHistory) {
            if(gameRepresentation.getResult() == null){
                continue;
            }
            response.add(getResponseFromRepresentation(gameRepresentation, user));

        }
        reverse(response);
        return response;
    }
    private GameHistoryResponse getResponseFromRepresentation(GameRepresentation representation, User user) {
        User blackPlayer = representation.getBlackPlayer();
        User whitePlayer = representation.getWhitePlayer();
        Color opponentColor;
        if (whitePlayer == user) {
            opponentColor = Color.BLACK;
        } else {
            opponentColor = Color.WHITE;
        }
        StringBuilder gameMoves = new StringBuilder();

        for (MoveRepresentation move : representation.getMoveRepresentations()) {
            gameMoves.append(move.toString());
        }

        return new GameHistoryResponse(representation,
                        whitePlayer, blackPlayer, opponentColor, gameMoves.toString());
    }


    public void addFriend(User user, User friend) {
        user.getFriends().add(friend);
        friend.getFriends().add(user);
        userRepository.save(user);
        userRepository.save(friend);
    }

    public void removeFriend(User user, User friend) {
        List<User> friends = user.getFriends();
        int originalSize = friends.size();
        friends.remove(friend);
        if(originalSize != friends.size()){
            friend.getFriends().remove(user);
        }
        userRepository.save(user);
        userRepository.save(friend);
    }
}
