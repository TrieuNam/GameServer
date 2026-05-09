package com.SouthMillion.activity_service.entity;

public class DuoBaoData {
    private int score;
    private String playerName;

    public DuoBaoData(int score, String playerName) {
        this.score = score;
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void calculateReward() {
        // TODO: implement reward calculation logic
    }
}