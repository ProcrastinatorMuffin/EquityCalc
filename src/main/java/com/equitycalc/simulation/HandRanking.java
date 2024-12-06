package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import java.util.List;


public class HandRanking implements Comparable<HandRanking> {
    private final HandType type;
    private final List<Card.Rank> tiebreakers;
    
    public HandRanking(HandType type, List<Card.Rank> tiebreakers) {
        this.type = type;
        this.tiebreakers = tiebreakers;
    }
    
    @Override
    public int compareTo(HandRanking other) {
        long startTime = System.nanoTime();
        int typeComparison = type.compareTo(other.type);
        if (typeComparison != 0) {
            PerformanceLogger.logOperation("HandCompare", startTime);
            return typeComparison;
        }
        
        // Compare tiebreakers in order
        for (int i = 0; i < tiebreakers.size() && i < other.tiebreakers.size(); i++) {
            int rankComparison = tiebreakers.get(i).compareTo(other.tiebreakers.get(i));
            if (rankComparison != 0) {
                PerformanceLogger.logOperation("HandCompare", startTime);
                return rankComparison;
            }
        }
        PerformanceLogger.logOperation("HandCompare", startTime);
        return 0;
    }
}