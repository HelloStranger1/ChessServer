package com.hellostranger.chessserver.models.user;

import com.hellostranger.chessserver.models.enums.Role;
import com.hellostranger.chessserver.models.game.GamePlayer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Data
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

    private String password;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<GamePlayer> gamesHistory;

    private Integer elo;

    @Enumerated(EnumType.STRING)
    private Role role;

    public User(String name, String email, String password, Role role){
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.elo = 800;

    }

    public void addGamePlayer(GamePlayer gamePlayer){
        if(this.gamesHistory == null){
            this.gamesHistory = new ArrayList<>();
        }
        this.gamesHistory.add(gamePlayer);

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
