package com.equitycalc.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Player {
    private List<Card> holeCards;
    private double winProbability;
    private double lossProbability;
    private double splitProbability;
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final int id;

    public Player(List<Card> holeCards) {
        if (holeCards == null || holeCards.size() != 2) {
            throw new IllegalArgumentException("Hole cards must contain exactly 2 cards.");
        }
        this.holeCards = holeCards;
        this.id = idCounter.getAndIncrement();
    }

    public List<Card> getHoleCards() {
        return holeCards;
    }

    public void setHoleCards(List<Card> holeCards) {
        if (holeCards == null || holeCards.size() != 2) {
            throw new IllegalArgumentException("Hole cards must contain exactly 2 cards.");
        }
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

    @Override
    public String toString() {
        return "Player{id=" + id + ", holeCards=" + holeCards + ", winProbability=" + winProbability +
                ", lossProbability=" + lossProbability + ", splitProbability=" + splitProbability + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
