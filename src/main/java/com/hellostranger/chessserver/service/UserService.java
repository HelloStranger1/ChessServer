package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.enums.Color;
import com.hellostranger.chessserver.storage.GameRepresentationRepository;
import com.hellostranger.chessserver.storage.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    GameRepresentationRepository gameRepresentationRepository;
  
    public User getUserByEmail(String userEmail) throws NoSuchElementException {
        return userRepository.findByEmail(userEmail).orElseThrow();
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public List<GameHistoryResponse> getUsersGameHistory(User user){
        List<GameRepresentation> gamesHistory = gameRepresentationRepository.findByWhitePlayerOrBlackPlayerOrderByDateDesc(user, user).orElse(new ArrayList<>());
        List<GameHistoryResponse> response = new ArrayList<>();
        for (GameRepresentation gameRepresentation : gamesHistory) {
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
                            .build()
            );
        }
        return response;
    }
    public void addFriend(User user, User friend) {
        user.getFriends().add(friend);
        friend.getFriends().add(user);
        userRepository.save(user);
        userRepository.save(friend);
    }

    public void removeFriend(User user, User friend) {
        user.getFriends().remove(friend);
        friend.getFriends().remove(user);
        userRepository.save(user);
        userRepository.save(friend);
    }
}
