package com.hellostranger.chessserver.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
public class UserActivity {
    private Integer userId;
    private LocalDateTime lastActiveTime;
}
