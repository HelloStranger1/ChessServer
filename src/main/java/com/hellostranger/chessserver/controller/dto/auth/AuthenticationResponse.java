package com.hellostranger.chessserver.controller.dto.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    private Long accessExpiresIn;
    private String refreshToken;
    private Long refreshExpiresIn;
}
