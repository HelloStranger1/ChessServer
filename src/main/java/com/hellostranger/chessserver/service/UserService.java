package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.storage.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User getUserByEmail(String userEmail) throws NoSuchElementException {
        return userRepository.findByEmail(userEmail).orElseThrow();
    }

    public void saveUser(User user){
        userRepository.save(user);
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
