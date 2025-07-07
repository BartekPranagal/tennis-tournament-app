package com.example.app.players.domain;

import java.util.List;

public class TournamentRound {
    private int roundNumber;
    private List<Match> matches;

    public TournamentRound(int roundNumber, List<Match> matches) {
        this.roundNumber = roundNumber;
        this.matches = matches;
    }

    public int getRoundNumber() { return roundNumber; }
    public List<Match> getMatches() { return matches; }

}
