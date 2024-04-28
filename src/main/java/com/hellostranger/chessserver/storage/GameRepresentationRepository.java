package com.hellostranger.chessserver.storage;


import com.hellostranger.chessserver.models.entities.GameRepresentation;
import com.hellostranger.chessserver.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface GameRepresentationRepository extends JpaRepository<GameRepresentation, Integer> {
    Optional<List<GameRepresentation>> findByWhitePlayerOrderByDateDesc(User whiteUser);
    Optional<List<GameRepresentation>> findByBlackPlayerOrderByDateDesc(User blackUser);

    Optional<List<GameRepresentation>> findByWhitePlayerOrBlackPlayerOrderByDateDesc(User whiteUser, User blackUser);

}
