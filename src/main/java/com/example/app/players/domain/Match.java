package com.example.app.players.domain;

import com.example.app.players.entity.Player;

public class Match {
    private Player player1;
    private Player player2; // może być null jeśli ktoś pauzuje
    private Integer score1;
    private Integer score2;

    public Match(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.score1 = null;
        this.score2 = null;
    }

    // get/set
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Integer getScore1() { return score1; }
    public Integer getScore2() { return score2; }
    public void setScore1(Integer score1) { this.score1 = score1; }
    public void setScore2(Integer score2) { this.score2 = score2; }
}
