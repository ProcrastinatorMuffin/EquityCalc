package com.equitycalc.simulation;

import java.io.Serializable;

public class SimulationResult implements Serializable {
    private final int[] wins;
    private final int[] losses;
    private final int[] splits;
    private int totalHands;
    private static final double Z_SCORE_95 = 1.96;
    
    public SimulationResult(int numPlayers) {
        wins = new int[numPlayers];
        losses = new int[numPlayers];
        splits = new int[numPlayers];
    }

    private double calculateMarginOfError(double probability) {
        if (totalHands == 0) return 0.0;
        // Using normal distribution approximation
        return Z_SCORE_95 * Math.sqrt((probability * (1 - probability)) / totalHands);
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

    public double[] getWinProbabilityWithConfidence(int playerIndex) {
        double probability = getWinProbability(playerIndex);
        double marginOfError = calculateMarginOfError(probability);
        return new double[] {
            Math.max(0.0, probability - marginOfError),  // Lower bound
            Math.min(1.0, probability + marginOfError)   // Upper bound
        };
    }
    
    public double[] getLossProbabilityWithConfidence(int playerIndex) {
        double probability = getLossProbability(playerIndex);
        double marginOfError = calculateMarginOfError(probability);
        return new double[] {
            Math.max(0.0, probability - marginOfError),
            Math.min(1.0, probability + marginOfError)
        };
    }
    
    public double[] getSplitProbabilityWithConfidence(int playerIndex) {
        double probability = getSplitProbability(playerIndex);
        double marginOfError = calculateMarginOfError(probability);
        return new double[] {
            Math.max(0.0, probability - marginOfError),
            Math.min(1.0, probability + marginOfError)
        };
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

    public int getTotalHands() {
        return totalHands;
    }
}