package com.hellostranger.chessserver.models.entities;


import com.hellostranger.chessserver.models.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class FriendRequest {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User recipient;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}
