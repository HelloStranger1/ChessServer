package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.models.UserActivity;
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


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.reverse;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;


    @Autowired
    GameRepresentationRepository gameRepresentationRepository;


    private Map<User, UserActivity> userActivityMap = new ConcurrentHashMap<>();
    public User getUserByEmail(String userEmail) throws NoSuchElementException {
        return userRepository.findByEmail(userEmail).orElseThrow();
    }

    @Scheduled(fixedRate = 90000)
    public void checkInactiveUsers(){

        LocalDateTime currentTime = LocalDateTime.now();
        log.info(currentTime.toString());
        for(Map.Entry<User, UserActivity> entry : userActivityMap.entrySet()){
            User user = entry.getKey();
            UserActivity userActivity = entry.getValue();

            if(userActivity.getLastActiveTime().plusMinutes(1).isBefore(currentTime)){
                user.setIsActive(false);
                userRepository.save(user);
                userActivityMap.remove(user);
            }
        }
    }
    public boolean isActive(@NonNull User user){
        return user.getIsActive();
    }
    public void keepAlive(@NonNull User user){
        user.setIsActive(true);
        userRepository.save(user);
        userActivityMap.put(user, new UserActivity(user.getId(), LocalDateTime.now()));
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public List<GameHistoryResponse> getUsersGameHistory(User user){
        List<GameRepresentation> gamesHistory = gameRepresentationRepository.findByWhitePlayerOrBlackPlayerOrderByDateDesc(user, user).orElse(new ArrayList<>());
        List<GameHistoryResponse> response = new ArrayList<>();
        for (GameRepresentation gameRepresentation : gamesHistory) {
            if(gameRepresentation.getResult() == null){
                continue;
            }
            User blackPlayer = gameRepresentation.getBlackPlayer();
            User whitePlayer = gameRepresentation.getWhitePlayer();
            Color opponentColor;
            if (whitePlayer == user) {
                opponentColor = Color.BLACK;

            } else {
                opponentColor = Color.WHITE;
            }
            StringBuilder gameMoves = new StringBuilder();
            List<MoveRepresentation> moves = gameRepresentation.getMoveRepresentations();
            for (MoveRepresentation move : moves) {
                gameMoves.append(move.toString());
            }

            response.add(
                    GameHistoryResponse
                            .builder()
                            .whiteImage(whitePlayer.getImage())
                            .blackImage(blackPlayer.getImage())
                            .whiteName(whitePlayer.getName())
                            .blackName(blackPlayer.getName())
                            .whiteElo(whitePlayer.getElo())
                            .blackElo(blackPlayer.getElo())
                            .startBoardJson(gameRepresentation.getStartBoardJson())
                            .gameDate(gameRepresentation.getDate())
                            .result(gameRepresentation.getResult())
                            .opponentColor(opponentColor)
                            .gameMoves(gameMoves.toString())
                            .id(gameRepresentation.getId())
                            .build()
            );
        }
        reverse(response);
        return response;
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
