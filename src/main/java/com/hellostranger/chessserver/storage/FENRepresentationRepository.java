package com.hellostranger.chessserver.storage;

import com.hellostranger.chessserver.models.entities.FENRepresentation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FENRepresentationRepository extends JpaRepository<FENRepresentation, Integer> {
}
