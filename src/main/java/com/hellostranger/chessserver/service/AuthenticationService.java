package com.hellostranger.chessserver.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellostranger.chessserver.controller.dto.auth.RegisterRequest;
import com.hellostranger.chessserver.controller.dto.auth.AuthenticationRequest;
import com.hellostranger.chessserver.controller.dto.auth.AuthenticationResponse;
import com.hellostranger.chessserver.models.entities.Token;
import com.hellostranger.chessserver.models.enums.Role;
import com.hellostranger.chessserver.models.entities.User;
import com.hellostranger.chessserver.models.enums.TokenType;
import com.hellostranger.chessserver.storage.TokenRepository;
import com.hellostranger.chessserver.storage.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException   ;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    // Repositories and services required for authentication operations
    private final UserRepository userRepository; // Repository to handle User data operations
    private final TokenRepository tokenRepository; // Repository to manage Token data
    private final PasswordEncoder passwordEncoder; // Service to encode passwords securely
    private final JwtService jwtService; // Service to handle JWT token operations
    private final AuthenticationManager authenticationManager; // Manager to authenticate users

    /**
     * Registers a new user.
     * @param request the registration request containing user details
     * @return an AuthenticationResponse with generated tokens
     */
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if a user with the given email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            // Throw an exception if the user exists
            throw new RuntimeException("User with email " + request.getEmail() + " already exists.");
        }

        // Create a new user object with details from the request
        var user = User.builder()
                .name(request.getName()) // Set username
                .email(request.getEmail()) // Set user email
                .password(passwordEncoder.encode(request.getPassword())) // Encode and set password
                .role(Role.USER) // Assign default role
                .build();

        // Save the newly created user to the database
        var savedUser = userRepository.save(user);

        // Generate JWT and refresh tokens for the user
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Save the generated JWT token to the user's token repository
        saveUserToken(savedUser, jwtToken);

        // Build and return the authentication response with the generated tokens and expiration times
        return AuthenticationResponse.builder()
                .accessToken(jwtToken) // Set the access token
                .accessExpiresIn(jwtService.extractExpiration(jwtToken).getTime()) // Set access token expiration
                .refreshToken(refreshToken) // Set the refresh token
                .refreshExpiresIn(jwtService.extractExpiration(refreshToken).getTime()) // Set refresh token expiration
                .build();
    }


    /**
     * Authenticates a user and returns a response with JWT tokens.
     * @param request the authentication request containing user credentials
     * @return an AuthenticationResponse with generated tokens
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Authenticate the user with provided email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), // User's email
                        request.getPassword() // User's password
                )
        );

        // Find the user by email in the repository, throw an exception if not found
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(); // Throw if user is not found

        // Generate JWT and refresh tokens for the authenticated user
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Revoke all existing tokens for the user
        revokeAllUserTokens(user);

        // Save the new JWT token to the user's token repository
        saveUserToken(user, jwtToken);

        // Build and return the authentication response with the generated tokens and expiration times
        return AuthenticationResponse.builder()
                .accessToken(jwtToken) // Set the access token
                .accessExpiresIn(jwtService.extractExpiration(jwtToken).getTime()) // Set access token expiration
                .refreshToken(refreshToken) // Set the refresh token
                .refreshExpiresIn(jwtService.extractExpiration(refreshToken).getTime()) // Set refresh token expiration
                .build();
    }

    /**
     * Revokes all valid tokens for the given user.
     * @param user the user whose tokens will be revoked
     */
    private void revokeAllUserTokens(User user) {
        // Retrieve all valid tokens for the user from the repository
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());

        // If there are no valid tokens, return immediately
        if (validUserTokens.isEmpty()) {
            return;
        }

        // Mark each token as expired and revoked
        validUserTokens.forEach(t -> {
            t.setExpired(true); // Mark token as expired
            t.setRevoked(true); // Mark token as revoked
        });

        // Save the updated tokens back to the repository
        tokenRepository.saveAll(validUserTokens);
    }

    /**
     * Saves a JWT token for the user in the token repository.
     * @param user the user for whom the token is saved
     * @param jwtToken the JWT token to be saved
     */
    private void saveUserToken(User user, String jwtToken) {
        // Create a new token object with the given JWT token and user details
        var token = Token.builder()
                .user(user) // Associate token with the user
                .token(jwtToken) // Set the token value
                .tokenType(TokenType.BEARER) // Set the token type to BEARER
                .expired(false) // Mark token as not expired
                .revoked(false) // Mark token as not revoked
                .build();

        // Save the token to the repository
        tokenRepository.save(token);
    }

    /**
     * Handles the refresh token process.
     * @param request the HTTP request containing the refresh token
     * @param response the HTTP response where the new tokens will be written
     * @throws IOException if an input or output error is detected
     */
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // Extract the authorization header from the HTTP request
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        // If the header is null or doesn't start with "Bearer ", return immediately
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        // Extract the token from the header
        refreshToken = authHeader.substring(7);
        // Extract the user's email from the token
        userEmail = jwtService.extractUsername(refreshToken);

        // If the email is not null, proceed with token validation and generation
        if (userEmail != null) {
            // Retrieve user details from the repository using the extracted email
            var userDetails = this.userRepository.findByEmail(userEmail)
                    .orElseThrow(); // Throw if user is not found

            // If the token is valid, generate new tokens and save them
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                var accessToken = jwtService.generateToken(userDetails); // Generate a new access token
                revokeAllUserTokens(userDetails); // Revoke all existing tokens for the user
                saveUserToken(userDetails, accessToken); // Save the new access token

                // Create an authentication response with the new tokens and expiration times
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken) // Set the access token
                        .accessExpiresIn(jwtService.extractExpiration(accessToken).getTime()) // Set access token expiration
                        .refreshToken(refreshToken) // Set the refresh token
                        .refreshExpiresIn(jwtService.extractExpiration(refreshToken).getTime()) // Set refresh token expiration
                        .build();

                // Write the response to the HTTP output stream
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }}
