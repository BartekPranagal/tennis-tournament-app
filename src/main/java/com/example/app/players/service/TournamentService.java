package com.example.app.players.service;

import com.example.app.players.domain.Match;
import com.example.app.players.domain.TournamentRound;
import com.example.app.players.entity.Player;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TournamentService {

    public List<TournamentRound> generateRoundRobinTournament(List<Player> players, int courts) {
        // Tasowanie zawodników, żeby każde losowanie było inne
        List<Player> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList);

        int numPlayers = playerList.size();
        boolean hasBye = false;

        if (numPlayers % 2 != 0) {
            playerList.add(null); // null = pauza (bye)
            numPlayers++;
            hasBye = true;
        }

        int numRounds = numPlayers - 1;
        int matchesPerRound = numPlayers / 2;

        // Stworzenie wszystkich możliwych par (każdy z każdym)
        Set<Set<Player>> allPairs = new HashSet<>();
        for (int i = 0; i < numPlayers; i++) {
            for (int j = i + 1; j < numPlayers; j++) {
                Player p1 = playerList.get(i);
                Player p2 = playerList.get(j);
                // pomijamy parę pauza-pauza
                if (p1 != null && p2 != null) {
                    allPairs.add(new HashSet<>(Arrays.asList(p1, p2)));
                }
            }
        }

        List<TournamentRound> rounds = new ArrayList<>();
        Set<Set<Player>> usedPairs = new HashSet<>();

        // Algorytm: w każdej rundzie wybieramy tyle unikalnych par ile kortów,
        // dbając by nikt nie grał 2x w tej samej rundzie, a reszta pauzuje
        while (usedPairs.size() < allPairs.size()) {
            List<Match> matchesThisRound = new ArrayList<>();
            Set<Player> usedThisRound = new HashSet<>();

            // Losowo miksujemy pary, by turniej był za każdym razem inny
            List<Set<Player>> pairsLeft = new ArrayList<>(allPairs);
            pairsLeft.removeAll(usedPairs);
            Collections.shuffle(pairsLeft);

            for (Set<Player> pair : pairsLeft) {
                Iterator<Player> it = pair.iterator();
                Player p1 = it.next();
                Player p2 = it.next();
                if (!usedThisRound.contains(p1) && !usedThisRound.contains(p2)) {
                    matchesThisRound.add(new Match(p1, p2));
                    usedThisRound.add(p1);
                    usedThisRound.add(p2);
                    usedPairs.add(pair);
                    if (matchesThisRound.size() >= courts) {
                        break;
                    }
                }
            }

            // Dodajemy pauzy dla zawodników którzy nie zagrali w tej rundzie
            for (Player p : playerList) {
                if (p != null && !usedThisRound.contains(p)) {
                    matchesThisRound.add(new Match(p, null));
                }
            }

            rounds.add(new TournamentRound(rounds.size() + 1, matchesThisRound));
        }

        return rounds;
    }
}