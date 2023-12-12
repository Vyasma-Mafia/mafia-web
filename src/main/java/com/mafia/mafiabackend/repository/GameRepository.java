package com.mafia.mafiabackend.repository;

import java.util.List;

import com.mafia.mafiabackend.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findAllByGameFinishedFalse();

    List<Game> findAllByGameFinishedTrue();
}

