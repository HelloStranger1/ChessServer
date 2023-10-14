package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.controller.dto.GameHistoryResponse;
import com.hellostranger.chessserver.models.entities.BoardRepresentation;
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
        for(int i = 0; i < gamesHistory.size(); i++){
            GameRepresentation gameRepresentation = gamesHistory.get(i);
            User opponent;
            Color opponentColor;
            if(gameRepresentation.getWhitePlayer() == user){
                opponent = gameRepresentation.getBlackPlayer();
                opponentColor = Color.BLACK;

            } else{
                opponent = gameRepresentation.getWhitePlayer();
                opponentColor = Color.WHITE;
            }
            String opponentImage = opponent.getImage(), opponentName = opponent.getName();
            Integer opponentElo = opponent.getElo();
            List<String> FENStrings = gameRepresentation.getBoardRepresentations().stream().map(BoardRepresentation::getBoardJson).toList();
            response.add(
                    GameHistoryResponse.builder()
                            .id(gameRepresentation.getId())
                            .boardsHistoryFEN(FENStrings)
                            .opponentColor(opponentColor)
                            .opponentElo(opponentElo)
                            .opponentImage(opponentImage)
                            .opponentName(opponentName)
                            .result(gameRepresentation.getResult())
                            .gameDate(LocalDate.now())
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
