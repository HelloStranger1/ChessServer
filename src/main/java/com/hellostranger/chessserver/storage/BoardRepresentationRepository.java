package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.entities.BoardRepresentation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepresentationRepository extends JpaRepository<BoardRepresentation, Integer> {
}
