package com.example.app.players.service;

import com.example.app.players.domain.Match;
import com.example.app.players.domain.TournamentRound;
import com.example.app.players.entity.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TournamentService {
    public List<TournamentRound> generateTournament(List<Player> players, int courtCount) {
        List<Player> copy = new ArrayList<>(players);
        boolean odd = false;
        if (copy.size() % 2 != 0) {
            copy.add(null); // null = pauza
            odd = true;
        }
        int n = copy.size();
        int rounds = n - 1;
        List<TournamentRound> tournament = new ArrayList<>();

        for (int round = 0; round < rounds; round++) {
            List<Match> matches = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Player p1 = copy.get(i);
                Player p2 = copy.get(n - 1 - i);
                matches.add(new Match(p1, p2));
            }
            // ograniczamy do liczby kortów
            List<Match> matchesForThisRound = matches.subList(0, Math.min(matches.size(), courtCount));
            tournament.add(new TournamentRound(round + 1, matchesForThisRound));

            // rotacja graczy (round-robin)
            Player last = copy.remove(copy.size() - 1);
            copy.add(1, last);
        }
        return tournament;
    }
    public List<TournamentRound> generateRoundRobinTournament(List<Player> players, int courts) {
        List<Player> playerList = new ArrayList<>(players);
        if (playerList.size() % 2 != 0) {
            playerList.add(null); // "wolny los" jeśli nieparzysta liczba graczy
        }
        int numRounds = playerList.size() - 1;
        int numMatchesPerRound = playerList.size() / 2;

        List<TournamentRound> rounds = new ArrayList<>();
        for (int round = 0; round < numRounds; round++) {
            List<Match> matches = new ArrayList<>();
            for (int match = 0; match < numMatchesPerRound; match++) {
                Player p1 = playerList.get(match);
                Player p2 = playerList.get(playerList.size() - 1 - match);
                if (p1 != null && p2 != null) {
                    matches.add(new Match(p1, p2));
                } else if (p1 != null || p2 != null) {
                    matches.add(new Match(p1 != null ? p1 : p2, null)); // pauza
                }
            }
            rounds.add(new TournamentRound(round + 1, matches)); // numer rundy od 1
            // "obrót" listy, żeby kolejne pary były inne
            Player last = playerList.remove(playerList.size() - 1);
            playerList.add(1, last);
        }
        return rounds;
    }
}
