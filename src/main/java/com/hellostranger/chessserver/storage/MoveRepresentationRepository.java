package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.entities.MoveRepresentation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MoveRepresentationRepository extends JpaRepository<MoveRepresentation, Integer> {
}
