package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.core.board.Board;
import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.storage.GameRepresentationRepository;
import com.hellostranger.chessserver.storage.UserRepository;
import lombok.NonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;

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
        int opponentColorIndex;
        if (whitePlayer == user) {
            opponentColorIndex = Board.blackIndex;
        } else {
            opponentColorIndex = Board.whiteIndex;
        }
        StringBuilder gameMoves = new StringBuilder();

        for (MoveRepresentation move : representation.getMoveRepresentations()) {
            gameMoves.append(move.toString());
            gameMoves.append(',');
        }

        return new GameHistoryResponse(representation,
                        whitePlayer, blackPlayer, opponentColorIndex, gameMoves.toString());
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
