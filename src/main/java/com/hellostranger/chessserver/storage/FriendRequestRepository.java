package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.entities.FriendRequest;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Integer> {
    Optional<List<FriendRequest>> findByRecipientAndStatus(User recipient, RequestStatus status);
}
