package com.example.app.players.repository;


import com.example.app.players.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByName(String name);
    void deleteByName(String name);
}
