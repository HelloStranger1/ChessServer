package com.hellostranger.chessserver.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hellostranger.chessserver.models.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String email;

    @Builder.Default private LocalDateTime lastTimeActive = LocalDateTime.now();
    @Builder.Default private Boolean isActive = false;

    @Builder.Default private Integer elo = 800;

    @Builder.Default private String image = "";

    @Builder.Default private LocalDate accountCreation = LocalDate.now();

    @Builder.Default private Integer totalGames = 0;

    @Builder.Default private Integer gamesWon = 0;

    @Builder.Default private Integer gamesLost = 0;

    @Builder.Default private Integer gamesDrawn = 0;

    @JsonIgnore
    private String password;

    @JsonIgnore
    @OneToMany(mappedBy = "whitePlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GameRepresentation> whiteGamesHistory;

    @JsonIgnore
    @OneToMany(mappedBy = "blackPlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GameRepresentation> blackGamesHistory;

    @JsonIgnore
    @OneToMany(mappedBy = "sender")
    private List<FriendRequest> sentFriendRequests = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_friends",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private List<User> friends = new ArrayList<>();

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Role role;

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Token> tokens;

    public User(String name, String email, String password, Role role){
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.elo = 800;
        this.image = "";


    }

    public void addGameToWhiteGameHistory(GameRepresentation gameRepresentation){
        if(this.whiteGamesHistory == null){
            this.whiteGamesHistory = new HashSet<>();
        }
        this.whiteGamesHistory.add(gameRepresentation);
    }
    public void addGameToBlackGameHistory(GameRepresentation gameRepresentation){
        if (this.blackGamesHistory == null) {
            this.blackGamesHistory = new HashSet<>();
        }
        this.blackGamesHistory.add(gameRepresentation);
    }

    @Override
    public String toString(){
        return "Id: " + id + " name: " + name + " email: " + email + " Elo: " + elo + " password: " + password;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


}
