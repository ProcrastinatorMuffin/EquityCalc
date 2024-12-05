package com.equitycalc.model;

public class Player {
    private String holeCards;
    private double winProbability;
    private double lossProbability;
    private double splitProbability;
    private static int idCounter = 1;
    private int id;

    public Player(String holeCards) {
        this.holeCards = holeCards;
        this.id = idCounter++;
    }

    public String getHoleCards() {
        return holeCards;
    }

    public void setHoleCards(String holeCards) {
        this.holeCards = holeCards;
    }

    public double getWinProbability() {
        return winProbability;
    }

    public void setWinProbability(double winProbability) {
        this.winProbability = winProbability;
    }

    public double getLossProbability() {
        return lossProbability;
    }

    public void setLossProbability(double lossProbability) {
        this.lossProbability = lossProbability;
    }

    public double getSplitProbability() {
        return splitProbability;
    }

    public void setSplitProbability(double splitProbability) {
        this.splitProbability = splitProbability;
    }

    public int getId() {
        return id;
    }
}
