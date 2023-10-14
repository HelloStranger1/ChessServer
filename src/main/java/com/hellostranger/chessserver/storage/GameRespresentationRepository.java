package com.hellostranger.chessserver.storage;


import com.hellostranger.chessserver.models.entities.GameRepresentation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRespresentationRepository extends JpaRepository<GameRepresentation, Integer> {
}
