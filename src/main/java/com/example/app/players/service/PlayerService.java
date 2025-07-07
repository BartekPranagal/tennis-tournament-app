package com.example.app.players.service;

import com.example.app.players.entity.Player;
import com.example.app.players.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PlayerService {
    private final PlayerRepository repo;

    public PlayerService(PlayerRepository repo) {
        this.repo = repo;
    }

    public List<Player> getPlayers() {
        return repo.findAll();
    }

    public boolean addPlayer(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || repo.existsByName(trimmed)) {
            return false;
        }
        repo.save(new Player(trimmed));
        return true;
    }

    @Transactional
    public boolean removePlayer(String name) {
        if (repo.existsByName(name)) {
            repo.deleteByName(name);
            return true;
        }
        return false;
    }

    public void clearPlayers() {
        repo.deleteAll();
    }
}