package com.hellostranger.chessserver.service;

import com.hellostranger.chessserver.models.entities.FriendRequest;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.enums.RequestStatus;
import com.hellostranger.chessserver.storage.FriendRequestRepository;
import com.hellostranger.chessserver.storage.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class FriendRequestService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    public void sendFriendRequest(String senderEmail, String recipientEmail) {
        User sender = userRepository.findByEmail(senderEmail).orElseThrow();
        User recipient = userRepository.findByEmail(recipientEmail).orElseThrow();

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setRecipient(recipient);
        friendRequest.setStatus(RequestStatus.PENDING);
        friendRequestRepository.save(friendRequest);
        userRepository.save(sender);
        userRepository.save(recipient);

    }
    public Boolean areUsersFriends(String u1Email, String u2Email){
        for(User user : userRepository.findByEmail(u1Email).orElseThrow().getFriends()){
            if(Objects.equals(user.getEmail(), u2Email)){
                return true;
            }
        }
        return false;
    }
    public List<FriendRequest> getPendingFriendRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        return friendRequestRepository.findByRecipientAndStatus(user, RequestStatus.PENDING).orElseThrow();
    }

    public void updateStateOfFriendRequest(RequestStatus requestStatus, String userEmail, Integer requestId){
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        FriendRequest friendRequest = friendRequestRepository.findById(requestId).orElse(null);
        if (friendRequest == null) {
            throw new IllegalArgumentException("Friend request not found.");
        }

        if (!friendRequest.getRecipient().equals(user)) {
            throw new IllegalArgumentException("You cannot accept this friend request.");
        }

        friendRequest.setStatus(requestStatus);
        friendRequestRepository.save(friendRequest);

        if(requestStatus == RequestStatus.ACCEPTED){
            User sender = friendRequest.getSender();
            user.getFriends().add(sender);
            sender.getFriends().add(user);
            userRepository.save(sender);
        }
        userRepository.save(user);

    }
}

