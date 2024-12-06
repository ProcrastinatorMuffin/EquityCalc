package com.equitycalc.simulation;

import java.io.Serializable;

public class SimulationResult implements Serializable {
    private final int[] wins;
    private final int[] losses;
    private final int[] splits;
    private int totalHands;
    
    public SimulationResult(int numPlayers) {
        wins = new int[numPlayers];
        losses = new int[numPlayers];
        splits = new int[numPlayers];
    }
    
    public double getWinProbability(int playerIndex) {
        return totalHands > 0 ? (double) wins[playerIndex] / totalHands : 0;
    }
    
    public double getLossProbability(int playerIndex) {
        return totalHands > 0 ? (double) losses[playerIndex] / totalHands : 0;
    }
    
    public double getSplitProbability(int playerIndex) {
        return totalHands > 0 ? (double) splits[playerIndex] / totalHands : 0;
    }
    
    public void incrementWin(int playerIndex) {
        wins[playerIndex]++;
    }
    
    public void incrementLoss(int playerIndex) {
        losses[playerIndex]++;
    }
    
    public void incrementSplit(int playerIndex) {
        splits[playerIndex]++;
    }
    
    public void incrementTotalHands() {
        totalHands++;
    }
}